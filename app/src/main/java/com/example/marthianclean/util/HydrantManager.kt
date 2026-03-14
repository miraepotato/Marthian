package com.example.marthianclean.util

import android.content.Context
import android.util.Log
import com.example.marthianclean.model.Hydrant
import org.json.JSONArray

object HydrantManager {
    private const val TAG = "HydrantManager"

    fun loadHwaseongHydrants(context: Context): List<Hydrant> {
        val hydrantList = mutableListOf<Hydrant>()
        try {
            // 1. assets 폴더에서 파일 읽기
            val jsonString = context.assets.open("hydrants.json")
                .bufferedReader().use { it.readText() }

            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)

                // 2. '화성시' 데이터만 필터링
                if (obj.optString("SIGUN_NM") == "화성시") {
                    val lat = obj.optString("REFINE_WGS84_LAT").toDoubleOrNull()
                    val lng = obj.optString("REFINE_WGS84_LOGT").toDoubleOrNull()

                    // 좌표가 정상인 데이터만 추가
                    if (lat != null && lng != null) {
                        hydrantList.add(
                            Hydrant(
                                id = obj.optString("FACLT_NO"),
                                name = obj.optString("FACLT_DEVICE_DETAIL_DIV_NM"),
                                lat = lat,
                                lng = lng,
                                type = obj.optString("FACLT_TYPE_CD")
                            )
                        )
                    }
                }
            }
            Log.d(TAG, "화성시 소화전 로드 성공: ${hydrantList.size}개")
        } catch (e: Exception) {
            Log.e(TAG, "데이터 로드 실패: ${e.message}")
            e.printStackTrace()
        }
        return hydrantList
    }
}