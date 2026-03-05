package com.example.marthianclean.logic

import com.example.marthianclean.model.FireVehicle
import com.example.marthianclean.model.SafetyCenter
import com.example.marthianclean.util.MatrixCalculator

/**
 * 블랙보드 상황판의 동적 매트릭스를 최종 조립하는 엔진입니다.
 * 35개 소방서의 각기 다른 자원 분포를 하나의 정형화된 그리드로 변환합니다.
 */
class BlackboardMatrixEngine {

    /**
     * [형님 핵심 로직]
     * 각 센터의 차량 리스트를 매트릭스 규격(maxRows)에 맞춰 2차원 리스트로 재구성합니다.
     * 차량이 부족한 칸은 null을 채워 넣어 그리드의 정렬을 유지합니다.
     * * @param centers 매트릭스에 표시할 안전센터 리스트
     * @return List<List<FireVehicle?>> -> [센터인덱스][차량인덱스] 형태의 2차원 배열
     */
    fun buildMatrix(centers: List<SafetyCenter>): List<List<FireVehicle?>> {
        if (centers.isEmpty()) return emptyList()

        // 1. 모든 센터 중 가장 많은 차량 대수 산출 (그리드의 전체 행 높이 결정)
        val maxRows = MatrixCalculator.calculateMaxRows(centers)

        // 2. 각 센터(열)를 순회하며 차량 데이터 배치
        return centers.map { center ->
            val columnVehicles = mutableListOf<FireVehicle?>()

            for (i in 0 until maxRows) {
                // 센터가 보유한 차량을 순서대로 넣고, 남는 칸은 null(빈 셀) 처리
                // 이를 통해 매트릭스의 모든 열이 동일한 높이를 가지게 됨
                val vehicle = center.vehicles.getOrNull(i)
                columnVehicles.add(vehicle)
            }

            columnVehicles
        }
    }

    /**
     * 특정 차종(예: 펌프, 탱크)만 필터링하여 특수 매트릭스를 구성할 때 사용합니다.
     */
    fun buildFilteredMatrix(
        centers: List<SafetyCenter>,
        targetTypes: List<String>
    ): List<List<FireVehicle?>> {
        val filteredCenters = centers.map { center ->
            center.copy(vehicles = center.vehicles.filter { it.type in targetTypes })
        }
        return buildMatrix(filteredCenters)
    }
}