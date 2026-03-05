package com.example.marthianclean.data

import android.content.Context
import android.util.Log
import com.example.marthianclean.model.FireStation
import com.example.marthianclean.model.SafetyCenter
import com.example.marthianclean.model.FireVehicle
import com.naver.maps.geometry.LatLng
import org.json.JSONArray
import java.io.InputStreamReader

class FireResourceRepository(private val context: Context) {

    private val TAG = "FireResourceRepository"

    // 1. 소방서 이름으로 데이터 필터링
    fun getStationByName(stationName: String): FireStation? {
        val all: List<FireStation> = loadAllStations()
        return all.find { it.name == stationName }
    }

    // 2. JSON 로드 및 FireStationCoords 좌표 직접 매칭
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

                // ✅ 본서 좌표: Any 타입을 LatLng로 명시적 형변환
                val sLatLng = FireStationCoords.stationHqMap[sName] as? LatLng ?: LatLng(0.0, 0.0)

                val centersArray = sObj.getJSONArray("centers")
                val centers = mutableListOf<SafetyCenter>()

                for (j in 0 until centersArray.length()) {
                    val cObj = centersArray.getJSONObject(j)
                    val cName = cObj.getString("name")

                    // ✅ 센터 좌표: stationCentersMap에서 LatLng 타입만 골라서 가져오기 (핵심 에러 해결!)
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

                    // SafetyCenter(name, location, vehicles) 규격 일치
                    centers.add(
                        SafetyCenter(
                            name = cName,
                            location = cLatLng,
                            vehicles = vehicles
                        )
                    )
                }

                // FireStation(name, location, centers) 규격 일치
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
}