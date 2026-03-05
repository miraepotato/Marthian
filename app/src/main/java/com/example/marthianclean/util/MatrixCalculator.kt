package com.example.marthianclean.util

import com.example.marthianclean.model.FireStation
import com.example.marthianclean.model.SafetyCenter
import com.example.marthianclean.model.FireVehicle
import com.naver.maps.geometry.LatLng
import kotlin.math.*

/**
 * [블랙보드 & 마션 통합 계산 결과 모델]
 */
data class MatrixResult(
    val matrix: List<List<Int>>,
    val departments: List<String>,
    val equipments: List<String>
)

/**
 * 블랙보드 동적 매트릭스의 레이아웃과 자원 배치를 계산하는 엔진입니다.
 * 35개 소방서의 각기 다른 자원 규모를 '최대공약수' 원리로 규격화합니다.
 */
object MatrixCalculator {

    /**
     * ✅ [핵심 함수] 소방서 전체 데이터를 분석하여 UI용 매트릭스 데이터를 생성합니다.
     * 뷰모델에서 이 함수를 호출하여 화면에 표를 그립니다.
     */
    fun calculateMatrix(station: FireStation): MatrixResult {
        val centers = station.centers
        val departments = centers.map { it.name }

        // 1. 해당 소방서에 존재하는 모든 차량 종류를 수집 (중복 제거)
        val allTypes = centers.flatMap { it.vehicles }.map { it.type }.distinct()

        // 2. 전술적 우선순위에 따라 컬럼(장비 종류) 정렬
        val equipments = sortTypesByPriority(allTypes)

        // 3. 매트릭스 데이터 생성 (행: 센터별, 열: 장비별 보유 대수)
        val matrix = centers.map { center ->
            equipments.map { type ->
                center.vehicles.count { it.type == type }
            }
        }

        return MatrixResult(matrix, departments, equipments)
    }

    /**
     * [형님 로직] 차종 문자열 리스트를 전술 우선순위에 따라 정렬합니다.
     */
    private fun sortTypesByPriority(types: List<String>): List<String> {
        val priorityMap = mapOf(
            "지휘차" to 1,
            "펌프차" to 2,
            "탱크차" to 3,
            "화학차" to 4,
            "구조공작차" to 5,
            "구급차" to 6
        )
        // 매트릭스 상단에 전술 차량이 먼저 오도록 정렬
        return types.sortedBy { type ->
            // "펌프차(기타)" 같은 경우도 "펌프차"로 인식하게 처리
            val key = priorityMap.keys.find { type.contains(it) }
            priorityMap[key] ?: 99
        }
    }

    /**
     * [형님 로직] 선택된 소방서 내 모든 센터 중 가장 많은 차량 대수를 찾아 반환합니다.
     */
    fun calculateMaxRows(centers: List<SafetyCenter>): Int {
        if (centers.isEmpty()) return 0
        return centers.maxOf { it.vehicles.size }
    }

    /**
     * 사고 지점(incidentLoc)과 각 안전센터 간의 직선 거리를 계산하여
     * 가장 가까운 순서대로 센터 리스트를 정렬합니다.
     */
    fun getCentersSortedByDistance(
        centers: List<SafetyCenter>,
        incidentLoc: LatLng
    ): List<SafetyCenter> {
        return centers.sortedBy { center ->
            calculateDistance(incidentLoc, center.location)
        }
    }

    /**
     * Haversine 공식을 이용한 두 좌표 간의 실거리(km) 계산
     */
    private fun calculateDistance(loc1: LatLng, loc2: LatLng): Double {
        val r = 6371.0 // 지구 반지름 (km)
        val dLat = Math.toRadians(loc2.latitude - loc1.latitude)
        val dLon = Math.toRadians(loc2.longitude - loc1.longitude)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(loc1.latitude)) * cos(Math.toRadians(loc2.latitude)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * 개별 차량 리스트를 우선순위에 따라 정렬합니다.
     */
    fun sortVehiclesByTacticalPriority(vehicles: List<FireVehicle>): List<FireVehicle> {
        val priorityMap = mapOf(
            "지휘차" to 1,
            "펌프차" to 2,
            "탱크차" to 3,
            "화학차" to 4,
            "구조공작차" to 5,
            "구급차" to 6
        )
        return vehicles.sortedBy { vehicle ->
            val key = priorityMap.keys.find { vehicle.type.contains(it) }
            priorityMap[key] ?: 99
        }
    }
}