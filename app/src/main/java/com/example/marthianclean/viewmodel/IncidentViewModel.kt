package com.example.marthianclean.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.marthianclean.BuildConfig
import com.example.marthianclean.data.IncidentStore
import com.example.marthianclean.data.FireStationCoords
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
import org.json.JSONArray // ✅ JSON 파싱용 Import 추가
import java.io.InputStreamReader // ✅ 파일 읽기용 Import 추가
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
    val sky: String = "-",
    val stationName: String = "-"
)

data class WeatherStation(
    val stnId: Int,
    val name: String,
    val lat: Double,
    val lng: Double
)

// =========================================================
// ✅ [Marthian 2.0] 거리순 추천 및 차량 검색을 위한 데이터 모델
// =========================================================
data class MarthianVehicle(
    val originalCallSign: String,
    val marthianName: String,
    val type: String,
    val isLifeSafety: Boolean
)

data class MarthianDepartment(
    val station: String,
    val deptName: String,
    val lat: Double,
    val lng: Double,
    val vehicles: List<MarthianVehicle>,
    var distance: Double = 0.0
)

class IncidentViewModel : ViewModel() {

    var selectedStationName: String = ""

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

    // =========================================================
    // ✅ [Marthian 2.0] 부서 거리순 추천 상태 관리 (StateFlow)
    // =========================================================
    private val _allDepartments = MutableStateFlow<List<MarthianDepartment>>(emptyList())
    val allDepartments: StateFlow<List<MarthianDepartment>> = _allDepartments.asStateFlow()

    private val _recommendedDepartments = MutableStateFlow<List<MarthianDepartment>>(emptyList())
    val recommendedDepartments: StateFlow<List<MarthianDepartment>> = _recommendedDepartments.asStateFlow()

    fun setMapPreferredZoom(zoom: Double?) {
        preferredMapZoom = zoom
    }

    private fun dateTimeNow(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)
        return fmt.format(Date())
    }

    fun setIncident(value: Incident) {
        _incident.value = value

        // ✅ [잔상 해결] 새로운 현장 검색 시, 이전 현장의 매트릭스와 차량 배치 데이터를 완전히 초기화합니다.
        dispatchMatrix = emptyList()
        dispatchDepartments = emptyList()
        dispatchEquipments = emptyList()
        placedVehicles = emptyList()

        syncMetaAddressIfBlank()
        syncDefaultDatesIfBlank()
        fetchRealtimeWeather()
        updateRecommendations() // ✅ 좌표가 바뀌었으니 거리순 추천 즉시 업데이트
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
        fetchRealtimeWeather()
        updateRecommendations() // ✅ 현장 복구 시 추천 목록 업데이트
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
        _recommendedDepartments.value = emptyList() // ✅ 추천 목록도 초기화

        // ✅ [잔상 해결] 명시적 초기화 시에도 매트릭스 잔상을 완벽히 지웁니다.
        dispatchMatrix = emptyList()
        dispatchDepartments = emptyList()
        dispatchEquipments = emptyList()
        placedVehicles = emptyList()
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
                    updateRecommendations() // ✅ 좌표가 바뀌었으니 거리순 추천 즉시 업데이트
                    onSuccess()
                }
            }
        }
    }

    fun updateSceneLocationFromDrag(context: Context, latLng: LatLng) {
        val cur = _incident.value ?: return
        _incident.value = cur.copy(latitude = latLng.latitude, longitude = latLng.longitude)

        updateRecommendations() // ✅ 드래그로 핀 위치가 바뀔 때마다 실시간 거리순 재계산

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

    fun toggleMatrixCell(r: Int, c: Int) {
        if (r !in dispatchMatrix.indices || c !in dispatchMatrix[r].indices) return
        val currentVal = dispatchMatrix[r][c]
        val newVal = if (currentVal == 0) 1 else 0

        val mutableMatrix = dispatchMatrix.map { it.toMutableList() }.toMutableList()
        mutableMatrix[r][c] = newVal
        dispatchMatrix = mutableMatrix
    }

    fun updateDispatchMatrix(matrix: List<List<Int>>) { dispatchMatrix = matrix }

    fun getDispatchCount(valueToCount: Int = 1): Int {
        val m = dispatchMatrix
        if (m.isEmpty()) return 0
        var sum = 0
        m.forEach { row -> row.forEach { if (it == valueToCount) sum++ } }
        return sum
    }

    /**
     * ✅ [Marthian 2.0] 동적 편성 엔진 (부서명 교정 로직 포함)
     */
    fun setupDynamicDispatch(context: Context, stationName: String, incidentLatLng: LatLng) {
        val validStationName = if (stationName.isNotBlank()) stationName else "관할소방서"
        val shortStationName = validStationName.replace("소방서", "").trim()
        val operationalUnits = mutableListOf<Pair<String, String>>()

        // 1. 관할 구조대 우선 추가
        val rescue = FireStationCoords.getCentersForStation(validStationName).find { it.contains("구조대") }
        if (rescue != null) {
            // ✅ [핵심 수정] "119구조대"를 "화성119구조대"로 자동 교정해서 데이터 매칭 오류 해결!
            val fixedRescueName = if (rescue == "119구조대" || rescue == "구조대") {
                "${shortStationName}119구조대"
            } else {
                rescue
            }
            operationalUnits.add(validStationName to fixedRescueName)
        }

        // 2. 행정/지휘 부서 제외 및 실전 부서 탐색
        val allOtherUnits = mutableListOf<Triple<String, String, Double>>()
        FireStationCoords.stationHqMap.keys.forEach { sName ->
            val shortSName = sName.replace("소방서", "").trim()
            val units = FireStationCoords.getCentersForStation(sName)
            units.forEach { uName ->
                val isExcluded = uName.contains("지휘단") || uName.contains("대응단") || uName == sName

                if (!isExcluded && (uName.contains("센터") || uName.contains("구조대") || uName.contains("지역대"))) {

                    // ✅ [핵심 수정] 인근 타 소방서 구조대(예: 수원119구조대)도 이름 자동 교정
                    val fixedUName = if (uName == "119구조대" || uName == "구조대") {
                        "${shortSName}119구조대"
                    } else {
                        uName
                    }

                    if (!operationalUnits.any { it.first == sName && it.second == fixedUName }) {
                        // 거리 계산은 기존 좌표록의 이름(uName)으로, 화면 표출은 교정된 이름(fixedUName)으로!
                        val unitLatLng = FireStationCoords.getCenterLatLng(uName, sName)
                        if (unitLatLng.latitude > 0.0) {
                            val dist = calculateDistance(incidentLatLng.latitude, incidentLatLng.longitude, unitLatLng.latitude, unitLatLng.longitude)
                            allOtherUnits.add(Triple(sName, fixedUName, dist))
                        }
                    }
                }
            }
        }

        // 거리순으로 가까운 센터 7개 추가 (구조대 포함 총 8개 부서)
        val nearbyUnits = allOtherUnits.sortedBy { it.third }.take(7).map { it.first to it.second }
        val finalUnits = operationalUnits + nearbyUnits
        val finalDepts = finalUnits.map { it.second }.toMutableList()

        // ✅ 항상 마지막에 '민간' 부서 강제 추가
        if (!finalDepts.contains("민간")) {
            finalDepts.add("민간")
        }

        // 차량 기본 헤더
        val fixedEquipments = listOf(
            "펌프", "구조공작", "장비운반", "탱크", "화학", "굴절", "고가", "무인파괴", "내폭화학", "구급", "포크레인"
        )

        dispatchDepartments = finalDepts
        dispatchEquipments = fixedEquipments
        dispatchMatrix = List(finalDepts.size) { List(fixedEquipments.size) { 0 } }
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

    fun getPlacedCount(): Int = placedVehicles.size

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

    fun buildStickerQueue(valueToInclude: Int = 1): List<StickerItem> {
        val out = ArrayList<StickerItem>(64)

        // ✅ [고정] 선택된 소방서의 지휘차는 매트릭스 선택 여부와 무관하게 무조건 1번으로 추가
        val hqName = if (selectedStationName.isNotBlank()) selectedStationName else "관할소방서"
        val shortHqName = hqName.replace("소방서", "").trim()
        out.add(StickerItem(id = "auto_cmd", department = hqName, equipment = "${shortHqName}지휘"))

        val depts = dispatchDepartments
        val equips = dispatchEquipments
        val m = dispatchMatrix

        if (depts.isEmpty() || equips.isEmpty() || m.isEmpty()) return out

        val rMax = minOf(m.size, depts.size)
        for (r in 0 until rMax) {
            val row = m[r]
            val cMax = minOf(row.size, equips.size)
            for (c in 0 until cMax) {
                if (row[c] == valueToInclude) {
                    out.add(StickerItem(id = "r${r}_c${c}", department = depts[r], equipment = equips[c]))
                }
            }
        }
        return out
    }

    // =========================================================================
    // ✅ [Marthian 2.0] JSON 데이터 로딩 및 파싱 (거리순 추천의 핵심)
    // =========================================================================

    fun loadFireData(context: Context) {
        viewModelScope.launch {
            try {
                val inputStream = context.assets.open("fire_data.json")
                val reader = InputStreamReader(inputStream)
                val jsonString = reader.readText()
                reader.close()

                val jsonArray = JSONArray(jsonString)
                val parsedList = mutableListOf<MarthianDepartment>()

                for (i in 0 until jsonArray.length()) {
                    val stationObj = jsonArray.getJSONObject(i)
                    val stationName = stationObj.optString("name", "")
                    val centersArray = stationObj.optJSONArray("centers") ?: continue

                    for (j in 0 until centersArray.length()) {
                        val centerObj = centersArray.getJSONObject(j)
                        val rawDeptName = centerObj.optString("name", "")
                        val deptLat = centerObj.optDouble("lat", 0.0)
                        val deptLng = centerObj.optDouble("lng", 0.0)

                        val marthianDeptName = formatDeptName(rawDeptName)
                        val vehiclesArray = centerObj.optJSONArray("vehicles") ?: continue

                        val vehicleList = mutableListOf<MarthianVehicle>()
                        for (k in 0 until vehiclesArray.length()) {
                            val vObj = vehiclesArray.getJSONObject(k)
                            val type = vObj.optString("type", "")
                            val callSign = vObj.optString("callSign", "")

                            val (mName, isLifeSafety) = formatVehicleName(marthianDeptName, type, callSign)
                            vehicleList.add(MarthianVehicle(callSign, mName, type, isLifeSafety))
                        }

                        parsedList.add(
                            MarthianDepartment(
                                station = stationName,
                                deptName = marthianDeptName,
                                lat = deptLat,
                                lng = deptLng,
                                vehicles = vehicleList
                            )
                        )
                    }
                }

                _allDepartments.value = parsedList
                updateRecommendations() // 데이터 로드 직후 1차 추천 계산

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun formatDeptName(rawName: String): String {
        var name = rawName.replace("119안전센터", "센터")
            .replace("119지역대", "지역대")
            .replace("119출동대", "출동대")
            .replace("안전센터", "센터")

        if (name.contains("구조대")) {
            val region = name.replace("구조대", "").replace("119", "").trim()
            name = "${region}구조대"
        }
        return name
    }

    private fun formatVehicleName(deptName: String, vType: String, callSign: String): Pair<String, Boolean> {
        val location = deptName.replace(Regex("센터|지역대|출동대|구조대"), "").trim()

        if (vType.contains("생활구조") || callSign.contains("생활안전")) {
            return Pair("${location}생활안전", true)
        }

        val mapping = mapOf(
            "구조공작" to "구조공작",
            "장비운반" to "장비운반",
            "펌프" to "펌프",
            "탱크" to "탱크",
            "화학" to "화학",
            "구급" to "구급",
            "고가" to "고가",
            "굴절" to "굴절",
            "조명" to "조연",
            "조연" to "조연",
            "미니펌프" to "미니펌프"
        )

        var finalType = "기타"
        for ((key, value) in mapping) {
            if (callSign.contains(key) || vType.contains(key)) {
                finalType = value
                break
            }
        }

        val number = callSign.firstOrNull { it.isDigit() }?.toString() ?: ""
        return Pair("${location}${number}${finalType}", false)
    }

    private fun updateRecommendations() {
        val currentLat = _incident.value?.latitude ?: return
        val currentLng = _incident.value?.longitude ?: return

        // 1. 전체 부서 데이터에 현재 위치 기준 '거리'를 전부 계산해서 덮어씌웁니다 (검색 시 0.0km 문제 해결)
        val updatedAll = _allDepartments.value.map { dept ->
            val dist = calculateDistance(currentLat, currentLng, dept.lat, dept.lng) / 1000.0
            dept.copy(distance = dist)
        }
        _allDepartments.value = updatedAll

        // 2. 추천 리스트 갱신 (UI에서 중복 제외 후 10개를 자를 수 있도록 정렬만 해둡니다)
        _recommendedDepartments.value = updatedAll.sortedBy { it.distance }
    }

    // =========================================================================
    // ✅ 실시간 기상 관측 데이터 연동 (수도권)
    // =========================================================================

    private val stationList = listOf(
        WeatherStation(108, "서울", 37.5714, 126.9658),
        WeatherStation(112, "인천", 37.4777, 126.6249),
        WeatherStation(119, "수원", 37.2723, 126.9853),
        WeatherStation(232, "이천", 37.2640, 127.4842),
        WeatherStation(212, "양평", 37.4886, 127.4945),
        WeatherStation(98, "동두천", 37.9019, 127.0607),
        WeatherStation(99, "파주", 37.8859, 126.7661),
        WeatherStation(201, "강화", 37.7074, 126.4463)
    )

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371e3
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
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
            stationList.find { it.stnId == 119 } ?: stationList[0]
        }

        viewModelScope.launch {
            try {
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

                    val wd = if (lastData.size > 2) lastData[2].toDoubleOrNull() ?: 0.0 else 0.0
                    val ws = if (lastData.size > 3) lastData[3] else "-"
                    val ta = if (lastData.size > 11) lastData[11] else "-"
                    val caTotStr = if (lastData.size > 30) lastData[30] else "-9"

                    val cloudCover = caTotStr.toDoubleOrNull()?.toInt() ?: -9
                    val skyState = when {
                        cloudCover in 0..2 -> "맑음"
                        cloudCover in 3..5 -> "구름조금"
                        cloudCover in 6..8 -> "구름많음"
                        cloudCover in 9..10 -> "흐림"
                        else -> "-"
                    }

                    val finalTa = if (ta.startsWith("-9") || ta == "-9.0") "-" else ta
                    val finalWs = if (ws.startsWith("-9")) "-" else ws
                    val finalWd = if (wd < 0.0 || wd > 360.0) 0.0 else wd
                    val dirStr = if (finalWs == "-") "-" else convertWindDirToStr(finalWd)

                    _weatherData.value = WeatherData(
                        temp = finalTa,
                        windSpeed = finalWs,
                        windDir = finalWd,
                        windDirStr = dirStr,
                        sky = skyState,
                        stationName = targetStation.name
                    )

                    if (curIncident != null) {
                        val meta = curIncident.meta
                        _incident.value = curIncident.copy(
                            meta = meta.copy(
                                기상_기온 = if (finalTa != "-") "${finalTa}℃" else meta.기상_기온,
                                기상_풍속 = if (finalWs != "-") "${finalWs}m/s" else meta.기상_풍속,
                                기상_풍향 = if (dirStr != "-") dirStr else meta.기상_풍향,
                                기상_날씨 = if (skyState != "-") skyState else meta.기상_날씨
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