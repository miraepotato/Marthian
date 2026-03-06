package com.example.marthianclean.util

import com.naver.maps.geometry.LatLng
import kotlin.math.*

/**
 * ✅ [마션 1.0 직무발명 타겟] 화재 현장 면적 산출 유틸리티
 * - GPS 좌표(위도, 경도) 기반 다각형 면적 계산
 * - 제곱미터(㎡) 및 평수 동시 변환 지원
 */
object AreaCalculator {

    private const val EARTH_RADIUS = 6378137.0 // 지구 반지름 (미터)
    private const val PYUNG_FACTOR = 3.305785  // 1평 = 3.305785㎡

    /**
     * ✅ 위도/경도 리스트를 받아 면적(㎡) 산출
     * - 구면 다각형 면적 공식(Spherical Area Formula) 사용
     */
    fun calculateAreaSquareMeters(points: List<LatLng>): Double {
        if (points.size < 3) return 0.0

        var area = 0.0
        for (i in points.indices) {
            val p1 = points[i]
            val p2 = points[(i + 1) % points.size]

            val rad1Lat = Math.toRadians(p1.latitude)
            val rad2Lat = Math.toRadians(p2.latitude)
            val deltaLon = Math.toRadians(p2.longitude - p1.longitude)

            // 구면 삼각법 기반의 간이 면적 산출 로직
            area += deltaLon * (2.0 + sin(rad1Lat) + sin(rad2Lat))
        }

        area = abs(area * EARTH_RADIUS * EARTH_RADIUS / 2.0)
        return area
    }

    /**
     * ✅ 제곱미터(㎡)를 평수로 변환
     */
    fun convertToPyung(sqMeters: Double): Double {
        return sqMeters / PYUNG_FACTOR
    }

    /**
     * ✅ UI 표출용 포맷팅 (예: "150.5㎡ / 45.5평")
     */
    fun getFormattedArea(points: List<LatLng>): String {
        val sqMeters = calculateAreaSquareMeters(points)
        val pyung = convertToPyung(sqMeters)

        return if (sqMeters == 0.0) {
            "면적 측정 필요"
        } else {
            String.format("%.1f㎡ / %.1f평", sqMeters, pyung)
        }
    }
}