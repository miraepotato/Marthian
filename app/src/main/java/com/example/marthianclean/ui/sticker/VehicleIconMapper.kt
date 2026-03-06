package com.example.marthianclean.ui.sticker

import com.example.marthianclean.R

/**
 * ✅ 프로젝트 전역 차량 아이콘 및 지능형 명칭 생성
 */
object VehicleIconMapper {

    fun iconResForEquip(equip: String): Int {
        val e = equip.trim().lowercase()

        return when {
            e.contains("펌프") -> R.drawable.ic_vehicle_pump
            e.contains("탱크") -> R.drawable.ic_vehicle_tank
            e.contains("화학") || e.contains("haz") -> R.drawable.ic_vehicle_hazmat
            e.contains("고가") || e.contains("사다리") || e.contains("ladder") -> R.drawable.ic_vehicle_ladder
            e.contains("굴절") || e.contains("articul") -> R.drawable.ic_vehicle_articulating_ladder
            e.contains("무인") || e.contains("방수") || e.contains("파괴") || e.contains("water") -> R.drawable.ic_vehicle_water_breaker
            e.contains("굴삭") || e.contains("포크") || e.contains("excava") -> R.drawable.ic_vehicle_excavator
            e.contains("구급") || e.contains("ambul") -> R.drawable.ic_vehicle_ambulance
            e.contains("구조") || e.contains("rescue") -> R.drawable.ic_vehicle_rescue
            e.contains("지휘차") || e.contains("command") -> R.drawable.ic_vehicle_command
            e.contains("회복") || e.contains("버스") || e.contains("recovery") || e.contains("bus") -> R.drawable.ic_vehicle_recovery_bus
            else -> R.drawable.ic_vehicle_equipment
        }
    }

    /**
     * ✅ [마션 1.0 핵심] 지능형 차량 라벨 생성
     * - 지휘단, 구조대, 조사는 본서명(예: 화성구조)으로 표기
     * - 나머지는 소속 센터명(예: 향남펌프, 팔탄탱크)으로 자동 파싱
     */
    fun customVehicleLabel(stationName: String, department: String, equipment: String): String {
        val sName = stationName.replace("소방서", "").trim()

        // 1. 본서 직할 부서인지 판별
        val isHqUnit = department.contains("소방서") ||
                department.contains("구조대") ||
                department.contains("대응단") ||
                department.contains("조사") ||
                equipment.contains("지휘") // 지휘차는 무조건 본서 소속

        // 2. 접두사 결정 (본서명 vs 센터명)
        val prefix = if (isHqUnit) {
            sName
        } else {
            department.replace("119", "")
                .replace("안전", "")
                .replace("센터", "")
                .replace("지역대", "")
                .replace("출동대", "")
                .trim()
        }

        // 3. 장비명 축약 (예: '구조공작차' -> '구조공작', '물탱크' -> '탱크')
        val equipShort = equipment.replace("차", "")
            .replace("물탱크", "탱크")
            .trim()

        return "$prefix$equipShort"
    }

    fun deptLabel(raw: String): String {
        val base = normalizeDeptName(raw)
        return "(${shortDept(base)})"
    }

    private fun normalizeDeptName(raw: String): String {
        return raw.trim().replace("소방서", "").replace("센터", "센터").replace("지역대", "지역대")
    }

    private fun shortDept(raw: String): String {
        val noParen = raw.replace(Regex("\\(.*?\\)"), "").trim()
        return noParen.removeSuffix("센터").removeSuffix("지역대").removeSuffix("구조대").removeSuffix("지휘단").trim().ifBlank { raw.trim() }
    }
}