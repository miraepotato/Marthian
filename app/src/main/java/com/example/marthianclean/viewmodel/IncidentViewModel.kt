package com.example.marthianclean.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.marthianclean.BuildConfig
import com.example.marthianclean.data.IncidentStore
import com.example.marthianclean.model.Incident
import com.example.marthianclean.model.VehiclePlacement
import com.example.marthianclean.network.GeocodingRepository
import com.example.marthianclean.network.RetrofitClient
import com.naver.maps.geometry.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlaceCandidate(
    val title: String,
    val address: String
)

// ✅ 지도 배치(스티커)용 (UI에서 쓰는 상태)
data class PlacedVehicle(
    val id: String,
    val department: String,
    val equipment: String,
    val position: LatLng
)

class IncidentViewModel : ViewModel() {

    /* =========================
       기존: Incident / 검색 상태
       ========================= */

    private val _incident = MutableStateFlow<Incident?>(null)
    val incident: StateFlow<Incident?> = _incident.asStateFlow()

    private val geocodingRepo = GeocodingRepository(RetrofitClient.geocodingService)

    // ✅ 장소 후보(최대 10개)
    private val _candidates = MutableStateFlow<List<PlaceCandidate>>(emptyList())
    val candidates: StateFlow<List<PlaceCandidate>> = _candidates.asStateFlow()

    private val _searchLoading = MutableStateFlow(false)
    val searchLoading: StateFlow<Boolean> = _searchLoading.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    fun setIncident(value: Incident) {
        _incident.value = value
    }

    /**
     * ✅ A안 핵심: Incident를 세팅하면서
     * - incident.placements -> placedVehicles 복원
     * - incident.dispatchPlan -> 매트릭스/메타 복원
     *
     * ⚠️ VehiclePlacement 좌표 필드는 lat/lng 기준
     */
    fun setIncidentAndRestoreAll(value: Incident) {
        _incident.value = value

        // 1) 배치 복원
        placedVehicles = value.placements.map { p ->
            PlacedVehicle(
                id = p.id,
                department = p.department,
                equipment = p.equipment,
                position = LatLng(p.lat, p.lng)
            )
        }

        // 2) 출동대 편성 복원(있으면)
        dispatchMatrix = value.dispatchPlan.matrix
        dispatchDepartments = value.dispatchPlan.departments
        dispatchEquipments = value.dispatchPlan.equipments
    }

    fun clearIncident() {
        _incident.value = null
    }

    fun clearCandidates() {
        _candidates.value = emptyList()
        _searchError.value = null
    }

    // ✅ 1) 장소명 검색 -> 후보 최대 10개
    fun searchPlaceCandidates(
        query: String,
        onDone: () -> Unit = {}
    ) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _searchError.value = "검색어가 비어 있습니다."
            _candidates.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {
                _searchLoading.value = true
                _searchError.value = null
                _candidates.value = emptyList()

                val res = RetrofitClient.localSearchService.searchLocal(
                    clientId = BuildConfig.NAVER_SEARCH_CLIENT_ID,
                    clientSecret = BuildConfig.NAVER_SEARCH_CLIENT_SECRET,
                    query = trimmed,
                    display = 10
                )

                val out = res.items.mapNotNull { item ->
                    val addr = item.roadAddress?.takeIf { it.isNotBlank() }
                        ?: item.address?.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null

                    val cleanTitle = (item.title ?: trimmed)
                        .replace("<b>", "")
                        .replace("</b>", "")

                    PlaceCandidate(
                        title = cleanTitle,
                        address = addr
                    )
                }

                if (out.isEmpty()) {
                    _searchError.value = "검색 결과가 없습니다. (주소로도 한 번 시도해보세요)"
                }

                _candidates.value = out
                onDone()
            } catch (e: Exception) {
                _searchError.value = "장소 검색 실패: ${e.message ?: "unknown"}"
                _candidates.value = emptyList()
            } finally {
                _searchLoading.value = false
            }
        }
    }

    // ✅ 2) 주소로 지오코딩해서 incident에 반영
    fun geocodeAndApply(
        query: String,
        onSuccess: () -> Unit,
        onFail: (String) -> Unit
    ) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            onFail("검색어가 비어 있습니다.")
            return
        }

        viewModelScope.launch {
            when (val out = geocodingRepo.geocode(trimmed)) {
                is GeocodingRepository.Outcome.Fail -> onFail(out.reason)
                is GeocodingRepository.Outcome.Ok -> {
                    val data = out.data

                    val current = _incident.value
                    val updated = current?.copy(
                        address = data.resolvedAddress,
                        latitude = data.latitude,
                        longitude = data.longitude
                    ) ?: Incident(
                        address = data.resolvedAddress,
                        latitude = data.latitude,
                        longitude = data.longitude
                    )

                    _incident.value = updated
                    onSuccess()
                }
            }
        }
    }

    /* =========================
       출동대 편성(매트릭스) + 메타
       ========================= */

    var dispatchMatrix by mutableStateOf<List<List<Int>>>(emptyList())
        private set

    fun updateDispatchMatrix(matrix: List<List<Int>>) {
        dispatchMatrix = matrix
    }

    var dispatchDepartments by mutableStateOf<List<String>>(emptyList())
        private set

    var dispatchEquipments by mutableStateOf<List<String>>(emptyList())
        private set

    fun updateDispatchMeta(
        departments: List<String>,
        equipments: List<String>
    ) {
        dispatchDepartments = departments
        dispatchEquipments = equipments
    }

    fun getDispatchCount(valueToCount: Int = 1): Int {
        val m = dispatchMatrix
        if (m.isEmpty()) return 0
        var sum = 0
        for (r in m.indices) {
            val row = m[r]
            for (c in row.indices) {
                if (row[c] == valueToCount) sum += 1
            }
        }
        return sum
    }

    fun getPlacedCount(): Int = placedVehicles.size

    fun getRemainingToPlace(): Int {
        val remaining = getDispatchCount(1) - getPlacedCount()
        return if (remaining < 0) 0 else remaining
    }

    data class StickerItem(
        val id: String,
        val department: String,
        val equipment: String
    )

    fun buildStickerQueue(valueToInclude: Int = 1): List<StickerItem> {
        val depts = dispatchDepartments
        val equips = dispatchEquipments
        val m = dispatchMatrix

        if (depts.isEmpty() || equips.isEmpty() || m.isEmpty()) return emptyList()

        val out = ArrayList<StickerItem>(64)
        val rMax = minOf(m.size, depts.size)

        for (r in 0 until rMax) {
            val row = m[r]
            val cMax = minOf(row.size, equips.size)

            for (c in 0 until cMax) {
                if (row[c] == valueToInclude) {
                    out.add(
                        StickerItem(
                            id = "r${r}_c${c}",
                            department = depts[r],
                            equipment = equips[c]
                        )
                    )
                }
            }
        }
        return out
    }

    /* =========================
       지도 스티커 배치 상태 (UI)
       ========================= */

    var placedVehicles by mutableStateOf<List<PlacedVehicle>>(emptyList())
        private set

    fun clearPlacedVehicles() {
        placedVehicles = emptyList()
    }

    fun placeVehicle(
        id: String,
        department: String,
        equipment: String,
        latLng: LatLng
    ) {
        val current = placedVehicles
        val idx = current.indexOfFirst { it.id == id }
        placedVehicles = if (idx >= 0) {
            current.toMutableList().apply {
                this[idx] = this[idx].copy(
                    department = department,
                    equipment = equipment,
                    position = latLng
                )
            }
        } else {
            current + PlacedVehicle(
                id = id,
                department = department,
                equipment = equipment,
                position = latLng
            )
        }
    }

    fun moveVehicle(id: String, newLatLng: LatLng) {
        val current = placedVehicles
        val idx = current.indexOfFirst { it.id == id }
        if (idx < 0) return
        placedVehicles = current.toMutableList().apply {
            this[idx] = this[idx].copy(position = newLatLng)
        }
    }

    fun removeVehicle(id: String) {
        placedVehicles = placedVehicles.filterNot { it.id == id }
    }

    /* =========================
       A안 핵심: UI 배치 -> Incident.placements 로 저장
       ========================= */

    private fun buildPlacementsForSave(): List<VehiclePlacement> {
        return placedVehicles.map { pv ->
            VehiclePlacement(
                id = pv.id,
                department = pv.department,
                equipment = pv.equipment,
                lat = pv.position.latitude,
                lng = pv.position.longitude
            )
        }
    }

    fun snapshotIncidentForSave(): Incident? {
        val cur = _incident.value ?: return null
        return cur.copy(
            dispatchPlan = cur.dispatchPlan.copy(
                matrix = dispatchMatrix,
                departments = dispatchDepartments,
                equipments = dispatchEquipments
            ),
            placements = buildPlacementsForSave()
        )
    }

    /* =========================
       지난 현장 저장/로드/삭제
       ========================= */

    fun saveCurrentIncident(context: Context) {
        val snap = snapshotIncidentForSave() ?: return
        viewModelScope.launch {
            IncidentStore.upsert(context, snap)
        }
    }

    fun saveIncident(context: Context, incident: Incident) {
        val snap = incident.copy(
            dispatchPlan = incident.dispatchPlan.copy(
                matrix = dispatchMatrix,
                departments = dispatchDepartments,
                equipments = dispatchEquipments
            ),
            placements = buildPlacementsForSave()
        )

        viewModelScope.launch {
            IncidentStore.upsert(context, snap)
        }
    }

    fun loadPastIncidents(
        context: Context,
        onLoaded: (List<Incident>) -> Unit
    ) {
        viewModelScope.launch {
            val list = IncidentStore.loadAll(context)
            onLoaded(list)
        }
    }

    fun deletePastIncidents(
        context: Context,
        ids: List<String>,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            IncidentStore.deleteMany(context, ids)
            onDone()
        }
    }
}
