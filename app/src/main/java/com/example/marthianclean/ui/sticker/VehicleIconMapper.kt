package com.example.marthianclean.ui.sticker

import com.example.marthianclean.R

/**
 * ✅ [Blackboard 1.0] 프로젝트 전역 차량 아이콘 및 무선호출명 기반 라벨 생성
 */
object VehicleIconMapper {

    /**
     * ✅ 차량 종류에 따른 아이콘 리소스 매핑
     * - 형님이 직접 제작한 11종의 고유 아이콘 최우선 매칭
     * - 지휘차 일괄 적용 및 생활안전차/장비운반차 통합 로직 포함
     */
    fun iconResForEquip(equip: String): Int {
        val e = equip.trim().lowercase()

        return when {
            // 1. 신규 추가 및 보강 차량 (가장 구체적인 키워드부터 매칭)
            e.contains("내폭") -> R.drawable.ic_vehicle_explosion_proof
            e.contains("험지") -> R.drawable.ic_vehicle_rough_terrain
            e.contains("산불") -> R.drawable.ic_vehicle_forest_fire
            e.contains("조사") -> R.drawable.ic_vehicle_investigation
            e.contains("안전지원") -> R.drawable.ic_vehicle_safety_support
            e.contains("조명") -> R.drawable.ic_vehicle_lighting
            e.contains("배연") -> R.drawable.ic_vehicle_smoke_extraction
            e.contains("산악") -> R.drawable.ic_vehicle_mountain_rescue
            e.contains("제독") -> R.drawable.ic_vehicle_decontamination
            e.contains("굴삭") || e.contains("포크") || e.contains("excava") -> R.drawable.ic_vehicle_excavator

            // 2. 통합 및 공용 아이콘
            e.contains("지휘") || e.contains("command") -> R.drawable.ic_vehicle_command
            e.contains("생활") || e.contains("장비운반") -> R.drawable.ic_vehicle_equipment

            // 3. 기존 표준 차량
            e.contains("펌프") -> R.drawable.ic_vehicle_pump
            e.contains("탱크") -> R.drawable.ic_vehicle_tank
            e.contains("화학") || e.contains("haz") -> R.drawable.ic_vehicle_hazmat
            e.contains("고가") || e.contains("사다리") || e.contains("ladder") -> R.drawable.ic_vehicle_ladder
            e.contains("굴절") || e.contains("articul") -> R.drawable.ic_vehicle_articulating_ladder
            e.contains("무인") || e.contains("방수") || e.contains("파괴") -> R.drawable.ic_vehicle_water_breaker
            e.contains("구급") || e.contains("ambul") -> R.drawable.ic_vehicle_ambulance
            e.contains("구조") || e.contains("rescue") -> R.drawable.ic_vehicle_rescue
            e.contains("회복") || e.contains("버스") || e.contains("recovery") -> R.drawable.ic_vehicle_recovery_bus

            else -> R.drawable.ic_vehicle_equipment
        }
    }

    /**
     * ✅ [마션 1.0 핵심] 지능형 차량 라벨 생성
     * - 형님의 요청에 따라 '무선호출명(Call Sign)'이 있으면 최우선으로 사용
     * - 무선호출명이 없을 경우에만 기존의 지능형 라벨링 로직으로 백업
     */
    fun customVehicleLabel(
        callSign: String,
        stationName: String,
        department: String,
        equipment: String
    ): String {
        // 1. 무선호출명(예: 향남1펌프, 수지조명)이 있다면 즉시 반환
        if (callSign.isNotBlank()) return callSign.trim()

        // 2. 무선호출명이 없는 경우 백업 로직 (수동 추가 부서 등)
        val sName = stationName.replace("소방서", "").trim()

        // 본서 직할 여부 판별
        val isHqUnit = department.contains("소방서") ||
                department.contains("구조대") ||
                department.contains("대응단") ||
                department.contains("조사") ||
                equipment.contains("지휘")

        val prefix = if (isHqUnit) {
            sName
        } else {
            department.replace("119", "")
                .replace("안전", "")
                .replace("센터", "")
                .replace("지역대", "")
                .trim()
        }

        val equipShort = equipment.replace("차", "")
            .replace("물탱크", "탱크")
            .trim()

        return "$prefix$equipShort"
    }

    /**
     * ✅ 부서 정보 라벨 (매트릭스 좌측 열 표시용)
     */
    fun deptLabel(raw: String): String {
        val base = normalizeDeptName(raw)
        return "(${shortDept(base)})"
    }

    private fun normalizeDeptName(raw: String): String {
        return raw.trim()
            .replace("소방서", "")
            .replace("센터", "센터")
            .replace("지역대", "지역대")
    }

    private fun shortDept(raw: String): String {
        val noParen = raw.replace(Regex("\\(.*?\\)"), "").trim()
        return noParen.removeSuffix("센터")
            .removeSuffix("지역대")
            .removeSuffix("구조대")
            .removeSuffix("지휘단")
            .trim()
            .ifBlank { raw.trim() }
    }
}