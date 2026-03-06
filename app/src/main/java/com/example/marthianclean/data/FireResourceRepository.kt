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
 * ✅ [마션 1.0 직무발명 타겟] 현장 지휘 최적화 데이터 레포지토리
 * - 필수 부서(본서, 구조대, 화재조사) 및 근거리 부서 자동 선별을 위한 데이터 공급
 */
class FireResourceRepository(private val context: Context) {

    private val TAG = "FireResourceRepository"

    // 1. 소방서 이름으로 데이터 필터링
    fun getStationByName(stationName: String): FireStation? {
        val all: List<FireStation> = loadAllStations()
        return all.find { it.name == stationName }
    }

    /**
     * ✅ [신규] 특정 부서의 상세 데이터(차량 정보 포함)를 가져오는 함수
     * 매트릭스 구성 시 부서명만으로 실제 차량 리스트를 매칭할 때 사용
     */
    fun findCenterInfo(stationName: String, centerName: String): SafetyCenter? {
        val station = getStationByName(stationName) ?: return null
        return station.centers.find { it.name == centerName }
    }

    // 2. JSON 로드 및 현업 부서 필터링 로직 포함
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

                val sLatLng = FireStationCoords.stationHqMap[sName] as? LatLng ?: LatLng(0.0, 0.0)

                val centersArray = sObj.getJSONArray("centers")
                val centers = mutableListOf<SafetyCenter>()

                for (j in 0 until centersArray.length()) {
                    val cObj = centersArray.getJSONObject(j)
                    val cName = cObj.getString("name")

                    // ✅ 필수 부서 및 현업 부서 필터링 (화재조사, 대응단 등 포함)
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
                                plateNumber = vObj.optString("plateNumber", "-"),
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
     * ✅ 출동 가능 부서 판별 키워드 업데이트
     * - 필수 편성 대상인 '조사', '대응단' 키워드를 추가하여 데이터 누락 방지
     */
    private fun isOperationalUnit(deptName: String): Boolean {
        // '출동대' 키워드 추가
        val operationalKeywords = listOf("센터", "구조대", "지역대", "지휘단", "대응단", "조사", "출동대", "구조공작", "구조활동")
        val administrativeKeywords = listOf("행정", "예방", "민원", "지원", "교육", "검사")

        if (administrativeKeywords.any { deptName.contains(it) }) return false

        return operationalKeywords.any { deptName.contains(it) }
    }
}