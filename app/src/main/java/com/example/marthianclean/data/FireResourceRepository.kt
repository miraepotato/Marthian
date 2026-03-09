package com.example.marthianclean.data

import android.content.Context
import android.util.Log
import com.example.marthianclean.model.VehicleInfo
import org.json.JSONArray
import java.io.InputStreamReader

class FireResourceRepository(private val context: Context) {

    private val TAG = "FireResourceRepository"
    private val centerVehicleMap = mutableMapOf<String, MutableList<VehicleInfo>>()

    init {
        loadVehicleDataFromJson()
        addPrivateCenter() // ✅ 민간(포크레인) 부서 강제 주입
    }

    private fun loadVehicleDataFromJson() {
        try {
            val inputStream = context.assets.open("fire_data.json")
            val reader = InputStreamReader(inputStream, "UTF-8")
            val jsonText = reader.readText()
            val jsonArray = JSONArray(jsonText)

            for (i in 0 until jsonArray.length()) {
                val stationObj = jsonArray.getJSONObject(i)
                val stationName = stationObj.optString("name", "").trim()
                // ✅ "화성소방서" -> "화성" 추출
                val shortStationName = stationName.replace("소방서", "").trim()

                val centersArray = stationObj.optJSONArray("centers") ?: JSONArray()

                for (j in 0 until centersArray.length()) {
                    val centerObj = centersArray.getJSONObject(j)
                    val rawCenterName = centerObj.optString("name", "").trim()

                    // ✅ [핵심 수술 부위] JSON 데이터 원본을 읽을 때부터 구조대 이름에 소방서 이름을 강제로 붙여서 저장합니다.
                    val centerName = if (rawCenterName == "119구조대" || rawCenterName == "구조대") {
                        "${shortStationName}119구조대" // 예: 화성119구조대
                    } else {
                        rawCenterName
                    }

                    val isExcluded = centerName.contains("지휘단") || centerName.contains("대응단") || centerName == stationName

                    if (!isExcluded && centerName.isNotEmpty()) {
                        val vehiclesArray = centerObj.optJSONArray("vehicles") ?: JSONArray()
                        for (k in 0 until vehiclesArray.length()) {
                            val vehicleObj = vehiclesArray.getJSONObject(k)
                            val type = vehicleObj.optString("type", "-").trim()
                            val callsign = vehicleObj.optString("callSign", "-").trim()

                            val info = VehicleInfo(stationName, centerName, type, callsign, false)
                            val list = centerVehicleMap.getOrPut(centerName) { mutableListOf() }
                            list.add(info)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "JSON 로드 실패: ${e.message}")
        }
    }

    // 하단에 고정될 '민간' 가상 부서 생성 (포크레인)
    private fun addPrivateCenter() {
        val privateVehicles = mutableListOf<VehicleInfo>()
        for (i in 1..5) {
            privateVehicles.add(VehicleInfo("민간", "민간", "포크레인", "포크레인 ${i}호기", false))
        }
        centerVehicleMap["민간"] = privateVehicles
    }

    // 차량 정렬 로직 (구조공작, 장비운반 우선순위!)
    fun getVehiclesByCenter(centerName: String): List<VehicleInfo> {
        val vehicles = centerVehicleMap[centerName.trim()] ?: emptyList()
        val priority = listOf("펌프", "구조공작", "장비운반", "탱크", "화학", "굴절", "고가", "무인파괴", "내폭화학", "구급")

        return vehicles.sortedBy { v ->
            val idx = priority.indexOfFirst { v.type.contains(it) || v.callsign.contains(it) }
            if (idx == -1) 999 else idx
        }
    }

    // 현재 소방서를 최상단에 두고, 나머지는 가나다순 정렬
    fun getAllCentersSorted(stationName: String): List<String> {
        val trimmedStation = stationName.trim()
        val allCenters = centerVehicleMap.keys.toList()

        // 1. 내 관할 소방서의 센터들
        val myCenters = centerVehicleMap.values.flatten()
            .filter { it.station.contains(trimmedStation) || trimmedStation.contains(it.station) }
            .map { it.center }
            .distinct()
            .sorted()

        // 2. 그 외 타 지역 센터들 (민간 제외)
        val otherCenters = allCenters.filter { it !in myCenters && it != "민간" }.sorted()

        return myCenters + otherCenters
    }
}