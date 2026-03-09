package com.example.marthianclean.data

import android.content.Context
import android.util.Log
import com.example.marthianclean.model.FireStation
import com.example.marthianclean.model.SafetyCenter
import com.example.marthianclean.model.FireVehicle
import com.naver.maps.geometry.LatLng
import org.json.JSONArray
import java.io.InputStreamReader

/**
 * ✅ [Blackboard 1.0] 현장 자원 데이터 공급 레포지토리
 * - 정제된 JSON 데이터 로드 및 민간 포크레인 가상 부서 자동 주입 로직 포함
 */
class FireResourceRepository(private val context: Context) {

    private val TAG = "FireResourceRepository"

    /**
     * ✅ 소방서 이름으로 상세 데이터 조회
     */
    fun getStationByName(stationName: String): FireStation? {
        val all: List<FireStation> = loadAllStations()
        return all.find { it.name == stationName }
    }

    /**
     * ✅ 특정 부서(센터/구조대 등)의 정보 및 차량 리스트 조회
     */
    fun findCenterInfo(stationName: String, centerName: String): SafetyCenter? {
        val station = getStationByName(stationName) ?: return null
        return station.centers.find { it.name == centerName }
    }

    /**
     * ✅ JSON 데이터 로드 및 데이터 전처리 (포크레인 가상 부서 포함)
     */
    fun loadAllStations(): List<FireStation> {
        val stations = mutableListOf<FireStation>()
        try {
            val inputStream = context.assets.open("fire_data.json")
            val reader = InputStreamReader(inputStream)
            val jsonText = reader.readText()
            val jsonArray = JSONArray(jsonText)

            for (i in 0 until jsonArray.length()) {
                val sObj = jsonArray.getJSONObject(i)
                val sName = sObj.getString("name")

                // 본서 좌표 (Coords 맵에서 참조)
                val sLatLng = FireStationCoords.stationHqMap[sName] as? LatLng ?: LatLng(0.0, 0.0)

                val centersArray = sObj.getJSONArray("centers")
                val centers = mutableListOf<SafetyCenter>()

                for (j in 0 until centersArray.length()) {
                    val cObj = centersArray.getJSONObject(j)
                    val cName = cObj.getString("name")

                    // 1. 행정 부서 제외 및 현업 부서 필터링
                    if (!isOperationalUnit(cName)) continue

                    val centerKey = "${sName}::${cName}"
                    val cLatLng = FireStationCoords.stationCentersMap[centerKey] as? LatLng ?: LatLng(0.0, 0.0)

                    val vehiclesArray = cObj.getJSONArray("vehicles")
                    val vehicles = mutableListOf<FireVehicle>()

                    for (k in 0 until vehiclesArray.length()) {
                        val vObj = vehiclesArray.getJSONObject(k)
                        vehicles.add(
                            FireVehicle(
                                type = vObj.optString("type", "-"),
                                callSign = vObj.optString("callSign", "-"),
                                plateNumber = vObj.optString("plateNumber", "-"), // 토글 식별자
                                status = vObj.optString("status", "대기")
                            )
                        )
                    }

                    centers.add(
                        SafetyCenter(
                            name = cName,
                            location = cLatLng,
                            vehicles = vehicles
                        )
                    )
                }

                // 2. [마션 1.0 핵심] 민간 포크레인 가상 부서 강제 주입
                // 실제 데이터에는 없지만 현장에서 민간 장비 편성을 위해 생성 (1호~5호)
                val excavatorCenter = createVirtualExcavatorCenter(sName, sLatLng)
                centers.add(excavatorCenter)

                stations.add(
                    FireStation(
                        name = sName,
                        location = sLatLng,
                        centers = centers
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "데이터 로드 중 에러 발생: ${e.message}")
        }
        return stations
    }

    /**
     * ✅ 출동 가능 부서 판별 키워드 (조사, 대응단, 출동대 포함)
     */
    private fun isOperationalUnit(deptName: String): Boolean {
        val operationalKeywords = listOf("센터", "구조대", "지역대", "지휘단", "대응단", "조사", "출동대", "구조공작", "구조활동")
        val administrativeKeywords = listOf("행정", "예방", "민원", "지원", "교육", "검사")

        if (administrativeKeywords.any { deptName.contains(it) }) return false
        return operationalKeywords.any { deptName.contains(it) }
    }

    /**
     * ✅ 민간 임차 장비(포크레인) 관리를 위한 가상 부서 생성
     * - 각 본서 소속으로 배치되며, 1호기부터 5호기까지 개별 편성 가능하도록 생성
     */
    private fun createVirtualExcavatorCenter(stationName: String, location: LatLng): SafetyCenter {
        val sNameShort = stationName.replace("소방서", "").trim()
        val excavatorVehicles = mutableListOf<FireVehicle>()

        // 1호부터 5호까지 가상 차량 생성
        for (i in 1..5) {
            excavatorVehicles.add(
                FireVehicle(
                    type = "포크레인",
                    callSign = "${sNameShort}포크레인 $i",
                    plateNumber = "VIRTUAL-${sNameShort}-EXCAVATOR-$i", // 편성 관리를 위한 고유 ID
                    status = "대기"
                )
            )
        }

        return SafetyCenter(
            name = "${sNameShort}포크레인",
            location = location,
            vehicles = excavatorVehicles
        )
    }
}