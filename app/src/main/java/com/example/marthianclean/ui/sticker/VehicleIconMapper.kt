package com.example.marthianclean.ui.sticker

import com.example.marthianclean.R

/**
 * ✅ 프로젝트 전역에서 "차량 아이콘 / 부서 라벨"을 여기로 통일
 * - drawable 폴더 실존 리소스명 기준 매핑
 * - 부서 표기는 "(향남)" 형태로 반환
 */
object VehicleIconMapper {

    fun iconResForEquip(equip: String): Int {
        val e = equip.trim()

        // 형님 drawable 기준:
        // ic_vehicle_pump, ic_vehicle_tank, ic_vehicle_hazmat, ic_vehicle_ladder,
        // ic_vehicle_water_breaker, ic_vehicle_excavator,
        // ic_vehicle_ambulance, ic_vehicle_rescue, ic_vehicle_recovery_bus
        return when {
            e.contains("펌프") -> R.drawable.ic_vehicle_pump
            e.contains("탱크") -> R.drawable.ic_vehicle_tank
            e.contains("화학") || e.contains("haz") -> R.drawable.ic_vehicle_hazmat
            e.contains("고가") || e.contains("사다리") || e.contains("ladder") -> R.drawable.ic_vehicle_ladder
            e.contains("굴절") || e.contains("articul") -> R.drawable.ic_vehicle_articulating_ladder
            e.contains("무인") || e.contains("방수") || e.contains("파괴") || e.contains("water") -> R.drawable.ic_vehicle_water_breaker
            e.contains("굴삭") || e.contains("excava") -> R.drawable.ic_vehicle_excavator
            e.contains("구급") || e.contains("ambul") -> R.drawable.ic_vehicle_ambulance
            e.contains("구조") || e.contains("rescue") -> R.drawable.ic_vehicle_rescue
            e.contains("회복") || e.contains("버스") || e.contains("recovery") || e.contains("bus") -> R.drawable.ic_vehicle_recovery_bus
            else -> R.drawable.ic_vehicle_equipment // 기본 아이콘(왕관/툴박스 계열)
        }
    }

    /**
     * ✅ 부서명을 괄호 라벨로 바꿔서 반환: "(향남)"
     * - "향남센터" -> "(향남)"
     * - "양감지역대" -> "(양감)"
     * - "안중센터(평택)" -> "(안중)"
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
        // 괄호 내용 제거: "안중센터(평택)" -> "안중센터"
        val noParen = raw.replace(Regex("\\(.*?\\)"), "").trim()

        // 접미어 제거
        return noParen
            .removeSuffix("센터")
            .removeSuffix("지역대")
            .removeSuffix("구조대")
            .removeSuffix("지휘단")
            .trim()
            .ifBlank { raw.trim() }
    }
}
