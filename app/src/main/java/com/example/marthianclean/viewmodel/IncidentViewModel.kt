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
import com.example.marthianclean.model.IncidentMeta
import com.example.marthianclean.model.VehiclePlacement
import com.example.marthianclean.network.GeocodingRepository
import com.example.marthianclean.network.ReverseGeocodingRepository
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

    private val _incident = MutableStateFlow<Incident?>(null)
    val incident: StateFlow<Incident?> = _incident.asStateFlow()

    private val geocodingRepo = GeocodingRepository(RetrofitClient.geocodingService)

    // ✅ ReverseGeocoding
    private val reverseRepo = ReverseGeocodingRepository(RetrofitClient.reverseGeocodingService)

    private val _candidates = MutableStateFlow<List<PlaceCandidate>>(emptyList())
    val candidates: StateFlow<List<PlaceCandidate>> = _candidates.asStateFlow()

    private val _searchLoading = MutableStateFlow(false)
    val searchLoading: StateFlow<Boolean> = _searchLoading.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    // =========================
    // 지도 줌 유지(복원)용
    // =========================
    var preferredMapZoom by mutableStateOf<Double?>(null)
        private set

    fun setMapPreferredZoom(zoom: Double?) {
        preferredMapZoom = zoom
    }

    fun setIncident(value: Incident) {
        _incident.value = value
        // ✅ 주소검색으로 들어온 주소를 현장정보수정에 “자동 표시”되게 싱크
        syncMetaAddressIfBlank()
        syncDefaultDatesIfBlank()
    }

    fun updateIncidentMeta(newMeta: IncidentMeta) {
        val cur = _incident.value ?: return
        _incident.value = cur.copy(meta = newMeta)
    }

    fun setIncidentAndRestoreAll(value: Incident) {
        _incident.value = value

        placedVehicles = value.placements.map { p ->
            PlacedVehicle(
                id = p.id,
                department = p.department,
                equipment = p.equipment,
                position = LatLng(p.lat, p.lng)
            )
        }

        dispatchMatrix = value.dispatchPlan.matrix
        dispatchDepartments = value.dispatchPlan.departments
        dispatchEquipments = value.dispatchPlan.equipments

        // ✅ 지난현장 불러와도 “현장정보수정” 주소 자동 표출 싱크
        syncMetaAddressIfBlank()
        syncDefaultDatesIfBlank()
    }

    private fun syncMetaAddressIfBlank() {
        val cur = _incident.value ?: return
        val addr = cur.address.trim()
        if (addr.isBlank()) return

        val meta = cur.meta
        if (meta.재난발생위치.isNotBlank()) return

        _incident.value = cur.copy(
            meta = meta.copy(재난발생위치 = addr)
        )
    }

    fun clearIncident() {
        _incident.value = null
    }

    fun clearCandidates() {
        _candidates.value = emptyList()
        _searchError.value = null
    }

    fun searchPlaceCandidates(query: String, onDone: () -> Unit = {}) {
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

                    PlaceCandidate(title = cleanTitle, address = addr)
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
                    // ✅ 주소검색 성공 시 meta.재난발생위치 자동 채움(비어있을 때만)
                    syncMetaAddressIfBlank()

                    onSuccess()
                }
            }
        }
    }

    // =========================
    // ✅ 현장 마커 이동 → 좌표/주소/메타 연동
    // =========================
    fun updateSceneLocationFromDrag(context: Context, latLng: LatLng) {
        val cur = _incident.value ?: return

        // 1) 좌표는 즉시 갱신
        _incident.value = cur.copy(
            latitude = latLng.latitude,
            longitude = latLng.longitude
        )

        // 2) reverse geocode로 주소 갱신 후 incident.address + meta.재난발생위치 같이 갱신
        viewModelScope.launch {
            when (val out = reverseRepo.reverse(latLng.latitude, latLng.longitude)) {
                is ReverseGeocodingRepository.Outcome.Fail -> {
                    // 실패해도 좌표는 이미 반영됨 (운영상 유리)
                    saveCurrentIncident(context)
                }
                is ReverseGeocodingRepository.Outcome.Ok -> {
                    val now = _incident.value ?: return@launch
                    val addr = out.address

                    val meta = now.meta
                    val newMeta = meta.copy(
                        재난발생위치 = addr
                    )

                    _incident.value = now.copy(
                        address = addr,
                        meta = newMeta
                    )

                    saveCurrentIncident(context)
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

    fun updateDispatchMeta(departments: List<String>, equipments: List<String>) {
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

    fun placeVehicle(id: String, department: String, equipment: String, latLng: LatLng) {
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

    private fun todayString(): String {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.KOREA)
        return fmt.format(java.util.Date())
    }

    private fun syncDefaultDatesIfBlank() {
        val cur = _incident.value ?: return
        val meta = cur.meta

        // ✅ “날짜 관련” 대표: 신고접수
        if (meta.신고접수.isBlank()) {
            _incident.value = cur.copy(meta = meta.copy(신고접수 = todayString()))
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

    fun saveCurrentIncident(context: Context) {
        val snap = snapshotIncidentForSave() ?: return
        viewModelScope.launch {
            IncidentStore.upsert(context, snap)
        }
    }

    fun loadPastIncidents(context: Context, onLoaded: (List<Incident>) -> Unit) {
        viewModelScope.launch {
            val list = IncidentStore.loadAll(context)
            onLoaded(list)
        }
    }

    fun deletePastIncidents(context: Context, ids: List<String>, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            IncidentStore.deleteMany(context, ids)
            onDone()
        }
    }
}