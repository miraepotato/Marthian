package com.example.marthianclean.util

import com.example.marthianclean.model.SafetyCenter

object MatrixLayoutManager {
    // 각 센터의 차량 대수 중 가장 큰 값을 찾아 매트릭스의 '행(Row)' 높이를 결정합니다.
    // 이것이 형님이 말씀하신 '최대공약수' 기반 동적 배치 로직입니다.
    fun calculateMaxRows(centers: List<SafetyCenter>): Int {
        if (centers.isEmpty()) return 0
        return centers.maxOf { it.vehicles.size }
    }

    // 사고 지점과 가장 가까운 센터 4개를 정렬하여 반환
    fun getPriorityCenters(centers: List<SafetyCenter>, incidentLoc: com.naver.maps.geometry.LatLng): List<SafetyCenter> {
        return centers.sortedBy { center ->
            // 거리 계산 로직 (Haversine 공식 등 적용)
            0.0 // 임시값
        }
    }
}