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
import org.json.JSONArray
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers

// =========================================================
// ✅ [Marthian 2.0] 특수재난 모드 및 현황판 데이터 모델 추가
// =========================================================

enum class DisasterMode {
    NORMAL,      // 일반 화재/구조
    WATER,       // 수난 구조 (수색/범위)
    APARTMENT    // 공동주택 (인명수색)
}

enum class SearchStatus {
    WAITING,     // 수색 전 (미확인)
    SEARCHING,   // 수색 중
    COMPLETED,   // 수색 완료 (안전)
    DANGER       // 요구조자 발생/위험
}

data class ApartmentData(
    val totalFloors: Int = 0,
    val lines: Int = 0,
    val gridStatus: Map<String, SearchStatus> = emptyMap()
)

data class WaterSearchZone(
    val day: Int,           // 수색 일차
    val radiusMeter: Double, // 반경 (m)
    val center: LatLng       // 중심점
)

data class WaterData(
    val commandPost: LatLng? = null, // 지휘소(CP) 위치
    val searchZones: List<WaterSearchZone> = emptyList(),
    val isSatellite: Boolean = true
)

// =========================================================
// 기존 데이터 모델
// =========================================================

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

    private val _allDepartments = MutableStateFlow<List<MarthianDepartment>>(emptyList())
    val allDepartments: StateFlow<List<MarthianDepartment>> = _allDepartments.asStateFlow()

    private val _recommendedDepartments = MutableStateFlow<List<MarthianDepartment>>(emptyList())
    val recommendedDepartments: StateFlow<List<MarthianDepartment>> = _recommendedDepartments.asStateFlow()

    // =========================================================
    // ✅ [Marthian 2.0] 특수재난 모드 상태 관리
    // =========================================================

    private val _currentMode = MutableStateFlow(DisasterMode.NORMAL)
    val currentMode: StateFlow<DisasterMode> = _currentMode.asStateFlow()

    // 브리핑용 화면 잠금 상태 (터치 방지)
    var isBriefingLocked by mutableStateOf(false)
        private set

    private val _apartmentData = MutableStateFlow(ApartmentData())
    val apartmentData: StateFlow<ApartmentData> = _apartmentData.asStateFlow()

    private val _waterData = MutableStateFlow(WaterData())
    val waterData: StateFlow<WaterData> = _waterData.asStateFlow()

    fun setDisasterMode(mode: DisasterMode) {
        _currentMode.value = mode
    }

    fun toggleBriefingLock() {
        isBriefingLocked = !isBriefingLocked
    }

    // 🏢 공동주택 데이터 컨트롤
    fun setupApartmentGrid(floors: Int, lines: Int) {
        _apartmentData.value = ApartmentData(totalFloors = floors, lines = lines)
    }

    fun toggleApartmentSearchStatus(cellId: String) {
        if (isBriefingLocked) return // 잠금 시 조작 불가
        val currentMap = _apartmentData.value.gridStatus.toMutableMap()
        val currentStatus = currentMap[cellId] ?: SearchStatus.WAITING

        val nextStatus = when (currentStatus) {
            SearchStatus.WAITING -> SearchStatus.SEARCHING
            SearchStatus.SEARCHING -> SearchStatus.COMPLETED
            SearchStatus.COMPLETED -> SearchStatus.DANGER
            SearchStatus.DANGER -> SearchStatus.WAITING
        }
        currentMap[cellId] = nextStatus
        _apartmentData.value = _apartmentData.value.copy(gridStatus = currentMap)
    }

    // ⚓ 수난구조 데이터 컨트롤
    fun setWaterCommandPost(latLng: LatLng) {
        if (isBriefingLocked) return
        _waterData.value = _waterData.value.copy(commandPost = latLng)
    }

    fun addWaterSearchZone(day: Int, radiusMeter: Double, center: LatLng) {
        if (isBriefingLocked) return
        val currentZones = _waterData.value.searchZones.toMutableList()
        currentZones.add(WaterSearchZone(day, radiusMeter, center))
        _waterData.value = _waterData.value.copy(searchZones = currentZones)
    }

    // =========================================================
    // 기존 로직 유지
    // =========================================================

    fun setMapPreferredZoom(zoom: Double?) {
        preferredMapZoom = zoom
    }

    private fun dateTimeNow(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)
        return fmt.format(Date())
    }

    fun setIncident(value: Incident) {
        val isNewIncident = _incident.value?.id != value.id
        _incident.value = value

        if (isNewIncident) {
            dispatchMatrix = emptyList()
            dispatchDepartments = emptyList()
            dispatchEquipments = emptyList()
            placedVehicles = emptyList()
        }

        syncMetaAddressIfBlank()
        syncDefaultDatesIfBlank()
        fetchRealtimeWeather()
        updateRecommendations()
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
        updateRecommendations()
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
        _recommendedDepartments.value = emptyList()

        _currentMode.value = DisasterMode.NORMAL
        isBriefingLocked = false
        _apartmentData.value = ApartmentData()
        _waterData.value = WaterData()

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
                    updateRecommendations()
                    onSuccess()
                }
            }
        }
    }

    fun updateSceneLocationFromDrag(context: Context, latLng: LatLng) {
        val cur = _incident.value ?: return
        _incident.value = cur.copy(latitude = latLng.latitude, longitude = latLng.longitude)

        updateRecommendations()

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

    fun setupDynamicDispatch(context: Context, stationName: String, incidentLatLng: LatLng) {
        val validStationName = if (stationName.isNotBlank()) stationName else "관할소방서"
        val shortStationName = validStationName.replace("소방서", "").trim()
        val operationalUnits = mutableListOf<Pair<String, String>>()

        val rescue = FireStationCoords.getCentersForStation(validStationName).find { it.contains("구조대") }
        val fixedRescueName = if (rescue != null) {
            if (rescue == "119구조대" || rescue == "구조대") "${shortStationName}구조대" else rescue
        } else {
            "${shortStationName}구조대"
        }
        operationalUnits.add(validStationName to fixedRescueName)

        val allOtherUnits = mutableListOf<Triple<String, String, Double>>()
        FireStationCoords.stationHqMap.keys.forEach { sName ->
            val shortSName = sName.replace("소방서", "").trim()
            val units = FireStationCoords.getCentersForStation(sName)
            units.forEach { uName ->
                val isExcluded = uName.contains("지휘단") || uName.contains("대응단") || uName == sName

                if (!isExcluded && (uName.contains("센터") || uName.contains("구조대") || uName.contains("지역대"))) {

                    val fixedUName = if (uName == "119구조대" || uName == "구조대") {
                        "${shortSName}구조대"
                    } else {
                        uName
                    }

                    if (!operationalUnits.any { it.first == sName && it.second == fixedUName }) {
                        val unitLatLng = FireStationCoords.getCenterLatLng(uName, sName)
                        if (unitLatLng.latitude > 0.0) {
                            val dist = calculateDistance(incidentLatLng.latitude, incidentLatLng.longitude, unitLatLng.latitude, unitLatLng.longitude)
                            allOtherUnits.add(Triple(sName, fixedUName, dist))
                        }
                    }
                }
            }
        }

        val nearbyUnits = allOtherUnits.sortedBy { it.third }.take(7).map { it.first to it.second }
        val finalDepts = (operationalUnits + nearbyUnits).map { it.second }.toMutableList()
        if (!finalDepts.contains("민간")) finalDepts.add("민간")

        val fixedEquipments = listOf(
            "펌프", "탱크", "구급", "구조공작", "장비운반", "생활안전", "화학", "굴절", "고가", "무인파괴", "내폭화학", "포크레인"
        )

        // ✅ [기억력 복원 로직] 이전 매트릭스 상태와 현재 배치된 차량을 모두 검사
        val previousMatrix = dispatchMatrix
        val previousDepts = dispatchDepartments
        val previousEquips = dispatchEquipments

        dispatchDepartments = finalDepts
        dispatchEquipments = fixedEquipments

        val newMatrix = List(finalDepts.size) { r ->
            List(fixedEquipments.size) { c ->
                val currentDept = finalDepts[r]
                val currentEquip = fixedEquipments[c]

                // 1. 지도에 이미 꽂혀있는가?
                val isPlaced = placedVehicles.any { it.department == currentDept && it.equipment == currentEquip }

                // 2. 이전에 체크를 해두었는가?
                val prevR = previousDepts.indexOf(currentDept)
                val prevC = previousEquips.indexOf(currentEquip)
                val wasSelected = if (prevR >= 0 && prevC >= 0 && previousMatrix.size > prevR && previousMatrix[prevR].size > prevC) {
                    previousMatrix[prevR][prevC] == 1
                } else false

                if (isPlaced || wasSelected) 1 else 0
            }
        }

        dispatchMatrix = newMatrix
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
                updateRecommendations()

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

    // ✅ [핵심 개선] 차량 데이터 짬처리 현상 완벽 차단. 각 장비의 타입을 놓치지 않고 추출합니다.
    private fun formatVehicleName(deptName: String, vType: String, callSign: String): Pair<String, Boolean> {
        val location = deptName.replace(Regex("센터|지역대|출동대|구조대"), "").trim()
        val rawStr = "${vType}_${callSign}".lowercase()

        // 1. 생활안전/생활구조 최우선 매칭 (장비운반으로 넘어가지 못하게 방어)
        if (rawStr.contains("생활구조") || rawStr.contains("생활안전")) {
            return Pair("${location}생활안전", true)
        }

        // 2. 정확한 장비 타입 추출 (화학, 버스 등이 누락되는 현상 완벽 방지)
        val finalType = when {
            rawStr.contains("내폭") -> "내폭화학"
            rawStr.contains("화학") -> "화학"
            rawStr.contains("구조") -> "구조공작"
            rawStr.contains("장비") -> "장비운반"
            rawStr.contains("버스") -> "버스"
            rawStr.contains("회복") -> "회복"
            rawStr.contains("펌프") -> "펌프"
            rawStr.contains("탱크") -> "탱크"
            rawStr.contains("구급") -> "구급"
            rawStr.contains("고가") || rawStr.contains("사다리") -> "고가"
            rawStr.contains("굴절") -> "굴절"
            rawStr.contains("조명") -> "조명"
            rawStr.contains("조연") -> "조연"
            rawStr.contains("배연") -> "배연"
            rawStr.contains("미니펌프") -> "미니펌프"
            rawStr.contains("무인파괴") -> "무인파괴"
            rawStr.contains("포크레인") || rawStr.contains("굴삭") -> "포크레인"
            rawStr.contains("지휘") -> "지휘"
            rawStr.contains("산불") -> "산불"
            rawStr.contains("험지") -> "험지"
            rawStr.contains("조사") -> "조사"
            else -> {
                // 매칭 실패 시 원본 타입에서 불필요한 단어만 제거하여 고유 명칭 유지
                val fallback = vType.replace("차", "").replace("소방", "").trim()
                if (fallback.isBlank()) "기타" else fallback
            }
        }

        val number = callSign.firstOrNull { it.isDigit() }?.toString() ?: ""
        return Pair("${location}${number}${finalType}", false)
    }

    private fun updateRecommendations() {
        val currentLat = _incident.value?.latitude ?: return
        val currentLng = _incident.value?.longitude ?: return

        val updatedAll = _allDepartments.value.map { dept ->
            val dist = calculateDistance(currentLat, currentLng, dept.lat, dept.lng) / 1000.0
            dept.copy(distance = dist)
        }
        _allDepartments.value = updatedAll

        _recommendedDepartments.value = updatedAll.sortedBy { it.distance }
    }

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

        viewModelScope.launch(Dispatchers.IO) {
            val cal = Calendar.getInstance()
            var success = false

            for (i in 0 until 3) {
                try {
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
                        val ta = if (lastData.size > 11) lastData[11] else "-9"

                        if (ta != "-9" && ta != "-9.0" && !ta.startsWith("-99")) {
                            parseAndApplyWeather(lastData, targetStation.name)
                            success = true
                            break
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                cal.add(Calendar.HOUR_OF_DAY, -1)
            }
        }
    }

    private fun parseAndApplyWeather(data: List<String>, stnName: String) {
        val curIncident = _incident.value
        val wd = if (data.size > 2) data[2].toDoubleOrNull() ?: 0.0 else 0.0
        val ws = if (data.size > 3) data[3] else "-"
        val ta = if (data.size > 11) data[11] else "-"
        val caTotStr = if (data.size > 30) data[30] else "-9"

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
        val dirStr = if (finalWs == "-") "-" else convertWindDirToStr(wd)

        _weatherData.value = WeatherData(
            temp = finalTa,
            windSpeed = finalWs,
            windDir = wd,
            windDirStr = dirStr,
            sky = skyState,
            stationName = stnName
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
}