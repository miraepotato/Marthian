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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class PlaceCandidate(
    val title: String,
    val address: String
)

data class PlacedVehicle(
    val id: String,
    val department: String,
    val equipment: String,
    val position: LatLng
)

data class WeatherData(
    val temp: String = "-",
    val windSpeed: String = "-",
    val windDir: Double = 0.0,
    val windDirStr: String = "-",
    val stationName: String = "-"
)

data class WeatherStation(
    val stnId: Int,
    val name: String,
    val lat: Double,
    val lng: Double
)

class IncidentViewModel : ViewModel() {

    private val _incident = MutableStateFlow<Incident?>(null)
    val incident: StateFlow<Incident?> = _incident.asStateFlow()

    private val geocodingRepo = GeocodingRepository(RetrofitClient.geocodingService)
    private val reverseRepo = ReverseGeocodingRepository(RetrofitClient.reverseGeocodingService)

    private val _candidates = MutableStateFlow<List<PlaceCandidate>>(emptyList())
    val candidates: StateFlow<List<PlaceCandidate>> = _candidates.asStateFlow()

    private val _searchLoading = MutableStateFlow(false)
    val searchLoading: StateFlow<Boolean> = _searchLoading.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    private val _weatherData = MutableStateFlow(WeatherData())
    val weatherData: StateFlow<WeatherData> = _weatherData.asStateFlow()

    var preferredMapZoom by mutableStateOf<Double?>(null)
        private set

    fun setMapPreferredZoom(zoom: Double?) {
        preferredMapZoom = zoom
    }

    private fun dateTimeNow(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)
        return fmt.format(Date())
    }

    fun setIncident(value: Incident) {
        val wd = _weatherData.value
        val meta = value.meta

        // 자동 연동: 현장이 세팅될 때, 이미 불러온 날씨가 있다면 메타 데이터에 자동으로 꽂아줍니다.
        val newMeta = if (wd.temp != "-" && meta.기상_기온.isBlank()) {
            meta.copy(
                기상_기온 = "${wd.temp}°C",
                기상_풍속 = "${wd.windSpeed}m/s",
                기상_풍향 = wd.windDirStr
            )
        } else meta

        _incident.value = value.copy(meta = newMeta)
        syncMetaAddressIfBlank()
        syncDefaultDatesIfBlank()
    }

    fun updateIncidentMeta(newMeta: IncidentMeta) {
        val cur = _incident.value ?: return
        _incident.value = cur.copy(meta = newMeta)
    }

    fun updateAddress(newAddress: String) {
        val cur = _incident.value ?: return
        val newMeta = cur.meta.copy(재난발생위치 = newAddress)
        _incident.value = cur.copy(
            address = newAddress,
            meta = newMeta
        )
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

    private fun syncDefaultDatesIfBlank() {
        val cur = _incident.value ?: return
        val meta = cur.meta

        if (meta.신고접수일시.isBlank()) {
            _incident.value = cur.copy(
                meta = meta.copy(신고접수일시 = dateTimeNow())
            )
        }
    }

    fun clearIncident() {
        _incident.value = null
        _weatherData.value = WeatherData()
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
                    _searchError.value = "검색 결과가 없습니다."
                }
                _candidates.value = out
                onDone()
            } catch (e: Exception) {
                _searchError.value = "장소 검색 실패: ${e.message}"
                _candidates.value = emptyList()
            } finally {
                _searchLoading.value = false
            }
        }
    }

    fun geocodeAndApply(query: String, onSuccess: () -> Unit, onFail: (String) -> Unit) {
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
                    syncMetaAddressIfBlank()

                    fetchRealtimeWeather()
                    onSuccess()
                }
            }
        }
    }

    fun updateSceneLocationFromDrag(context: Context, latLng: LatLng) {
        val cur = _incident.value ?: return
        _incident.value = cur.copy(latitude = latLng.latitude, longitude = latLng.longitude)

        viewModelScope.launch {
            when (val out = reverseRepo.reverse(latLng.latitude, latLng.longitude)) {
                is ReverseGeocodingRepository.Outcome.Fail -> saveCurrentIncident(context)
                is ReverseGeocodingRepository.Outcome.Ok -> {
                    val now = _incident.value ?: return@launch
                    val addr = out.address
                    val newMeta = now.meta.copy(재난발생위치 = addr)
                    _incident.value = now.copy(address = addr, meta = newMeta)
                    saveCurrentIncident(context)

                    fetchRealtimeWeather()
                }
            }
        }
    }

    var dispatchMatrix by mutableStateOf<List<List<Int>>>(emptyList())
        private set
    var dispatchDepartments by mutableStateOf<List<String>>(emptyList())
        private set
    var dispatchEquipments by mutableStateOf<List<String>>(emptyList())
        private set

    fun updateDispatchMeta(departments: List<String>, equipments: List<String>) {
        dispatchDepartments = departments
        dispatchEquipments = equipments
    }

    fun updateDispatchMatrix(matrix: List<List<Int>>) { dispatchMatrix = matrix }

    fun getDispatchCount(valueToCount: Int = 1): Int {
        val m = dispatchMatrix
        if (m.isEmpty()) return 0
        var sum = 0
        m.forEach { row -> row.forEach { if (it == valueToCount) sum++ } }
        return sum
    }

    var placedVehicles by mutableStateOf<List<PlacedVehicle>>(emptyList())
        private set

    fun placeVehicle(id: String, department: String, equipment: String, latLng: LatLng) {
        val current = placedVehicles
        val idx = current.indexOfFirst { it.id == id }
        placedVehicles = if (idx >= 0) {
            current.toMutableList().apply { this[idx] = this[idx].copy(department = department, equipment = equipment, position = latLng) }
        } else {
            current + PlacedVehicle(id = id, department = department, equipment = equipment, position = latLng)
        }
    }

    fun moveVehicle(id: String, newLatLng: LatLng) {
        val current = placedVehicles
        val idx = current.indexOfFirst { it.id == id }
        if (idx < 0) return
        placedVehicles = current.toMutableList().apply { this[idx] = this[idx].copy(position = newLatLng) }
    }

    fun removeVehicle(id: String) { placedVehicles = placedVehicles.filterNot { it.id == id } }

    fun clearPlacedVehicles() { placedVehicles = emptyList() }

    private fun buildPlacementsForSave(): List<VehiclePlacement> {
        return placedVehicles.map { VehiclePlacement(it.id, it.department, it.equipment, it.position.latitude, it.position.longitude) }
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
        viewModelScope.launch { IncidentStore.upsert(context, snap) }
    }

    fun loadPastIncidents(context: Context, onLoaded: (List<Incident>) -> Unit) {
        viewModelScope.launch { onLoaded(IncidentStore.loadAll(context)) }
    }

    fun deletePastIncidents(context: Context, ids: List<String>, onDone: () -> Unit = {}) {
        viewModelScope.launch { IncidentStore.deleteMany(context, ids); onDone() }
    }

    data class StickerItem(val id: String, val department: String, val equipment: String)

    fun buildStickerQueue(): List<StickerItem> {
        val res = mutableListOf<StickerItem>()
        res.add(StickerItem("CMD_AUTO_01", "화성소방서 지휘단", "지휘차"))

        if (dispatchMatrix.isEmpty()) return res

        val deps = dispatchDepartments
        val eqs = dispatchEquipments

        for (r in dispatchMatrix.indices) {
            val dep = deps.getOrNull(r) ?: continue
            for (c in dispatchMatrix[r].indices) {
                val eq = eqs.getOrNull(c) ?: continue
                if (eq.contains("지휘")) continue

                val count = dispatchMatrix[r][c]
                for (i in 0 until count) {
                    val uid = "stk_${r}_${c}_$i"
                    res.add(StickerItem(uid, dep, eq))
                }
            }
        }
        return res
    }

    // =========================================================================
    // ✅ 기상청 실시간 지상관측(ASOS) 데이터 연동 & 근접 관측소 자동 탐색 (수도권 전역)
    // =========================================================================

    // ✅ 수도권(서울/경기/인천)을 완벽하게 덮는 8개 ASOS 관측소 망
    private val stationList = listOf(
        WeatherStation(108, "서울", 37.5714, 126.9658),       // 서울, 과천, 광명 커버
        WeatherStation(112, "인천", 37.4777, 126.6249),       // 인천, 부천, 시흥 서부 커버
        WeatherStation(119, "수원", 37.2723, 126.9853),       // 수원, 화성, 오산, 용인 커버
        WeatherStation(232, "이천", 37.2640, 127.4842),       // 이천, 여주, 광주 커버
        WeatherStation(212, "양평", 37.4886, 127.4945),       // 양평, 가평, 하남 커버
        WeatherStation(98, "동두천", 37.9019, 127.0607),      // 동두천, 연천, 포천, 양주 커버
        WeatherStation(99, "파주", 37.8859, 126.7661),        // 파주, 고양, 김포 북부 커버
        WeatherStation(201, "강화", 37.7074, 126.4463)        // 강화도 및 서해안 북부 커버
    )

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371e3
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    private fun findNearestStation(lat: Double, lng: Double): WeatherStation {
        var nearestStn = stationList[0]
        var minDistance = Double.MAX_VALUE
        for (station in stationList) {
            val dist = calculateDistance(lat, lng, station.lat, station.lng)
            if (dist < minDistance) {
                minDistance = dist
                nearestStn = station
            }
        }
        return nearestStn
    }

    private fun convertWindDirToStr(wd: Double): String {
        val directions = arrayOf("북", "북북동", "북동", "동북동", "동", "동남동", "남동", "남남동", "남", "남남서", "남서", "서남서", "서", "서북서", "북서", "북북서", "북")
        val index = Math.round((wd % 360) / 22.5).toInt()
        return directions[index]
    }

    fun fetchRealtimeWeather() {
        val curIncident = _incident.value
        val targetStation = if (curIncident?.latitude != null && curIncident.longitude != null) {
            findNearestStation(curIncident.latitude, curIncident.longitude)
        } else {
            stationList.find { it.stnId == 119 } ?: stationList[0] // 기본값: 수원(119)
        }

        viewModelScope.launch {
            try {
                // 안전하게 1시간 전 정각 데이터로 강제 변환하여 요청
                val cal = Calendar.getInstance()
                cal.add(Calendar.HOUR_OF_DAY, -1)
                val tmFormat = SimpleDateFormat("yyyyMMddHH00", Locale.KOREA)
                val tm = tmFormat.format(cal.time)

                val responseBody = RetrofitClient.kmaService.getStationWeather(
                    time = tm,
                    stnId = targetStation.stnId,
                    help = 1
                )
                val rawText = responseBody.string()

                val lines = rawText.lines().filter { it.isNotBlank() && !it.startsWith("#") }
                if (lines.isNotEmpty()) {
                    val lastData = lines.last().trim().split(Regex("\\s+"))

                    // 기온(TA) 11번 인덱스 확정 적용
                    val wd = lastData.getOrNull(2)?.toDoubleOrNull() ?: 0.0
                    val ws = lastData.getOrNull(3) ?: "-"
                    val ta = lastData.getOrNull(11) ?: "-"

                    // 결측치(-9.0, -99.0 등) 필터링
                    val finalTa = if (ta.startsWith("-9") || ta == "-9.0") "-" else ta
                    val finalWs = if (ws.startsWith("-9")) "-" else ws
                    val finalWd = if (wd < 0.0 || wd > 360.0) 0.0 else wd

                    val dirStr = if (finalWs == "-") "-" else convertWindDirToStr(finalWd)

                    _weatherData.value = WeatherData(
                        temp = finalTa,
                        windSpeed = finalWs,
                        windDir = finalWd,
                        windDirStr = dirStr,
                        stationName = targetStation.name
                    )

                    // 현재 열려있는 현장 정보(메타)에도 실시간 기상 자동 업데이트
                    if (curIncident != null) {
                        val meta = curIncident.meta
                        _incident.value = curIncident.copy(
                            meta = meta.copy(
                                기상_기온 = if (finalTa != "-") "${finalTa}°C" else meta.기상_기온,
                                기상_풍속 = if (finalWs != "-") "${finalWs}m/s" else meta.기상_풍속,
                                기상_풍향 = if (dirStr != "-") dirStr else meta.기상_풍향
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}