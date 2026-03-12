package com.example.marthianclean.ui.sticker

import com.example.marthianclean.R

object VehicleIconMapper {
    fun iconResForEquip(equip: String): Int {
        val e = equip.trim().lowercase()
        return when {
            e.contains("내폭") -> R.drawable.ic_vehicle_explosion_proof
            e.contains("험지") -> R.drawable.ic_vehicle_rough_terrain
            e.contains("산불") -> R.drawable.ic_vehicle_forest_fire
            e.contains("조사") -> R.drawable.ic_vehicle_investigation
            e.contains("조명") -> R.drawable.ic_vehicle_lighting
            e.contains("배연") -> R.drawable.ic_vehicle_smoke_extraction
            e.contains("산악") -> R.drawable.ic_vehicle_mountain_rescue
            e.contains("제독") -> R.drawable.ic_vehicle_decontamination
            e.contains("굴삭") || e.contains("포크") || e.contains("excava") -> R.drawable.ic_vehicle_excavator
            e.contains("지휘") || e.contains("command") -> R.drawable.ic_vehicle_command

            // 생활안전, 안전지원, 장비운반 -> 장비운반 아이콘
            e.contains("생활") || e.contains("안전지원") || e.contains("장비운반") -> R.drawable.ic_vehicle_equipment

            e.contains("펌프") -> R.drawable.ic_vehicle_pump
            e.contains("탱크") -> R.drawable.ic_vehicle_tank
            e.contains("화학") || e.contains("haz") -> R.drawable.ic_vehicle_hazmat
            e.contains("고가") || e.contains("사다리") || e.contains("ladder") -> R.drawable.ic_vehicle_ladder
            e.contains("굴절") || e.contains("articul") -> R.drawable.ic_vehicle_articulating_ladder
            e.contains("무인") || e.contains("방수") || e.contains("파괴") -> R.drawable.ic_vehicle_water_breaker
            e.contains("구급") || e.contains("ambul") -> R.drawable.ic_vehicle_ambulance
            e.contains("구조공작") || (e.contains("구조") && !e.contains("구조대") && !e.contains("생활구조")) || e.contains("rescue") -> R.drawable.ic_vehicle_rescue

            // ✅ 버스 및 회복차 아이콘 명시
            e.contains("버스") || e.contains("회복") || e.contains("recovery") -> R.drawable.ic_vehicle_recovery_bus

            else -> R.drawable.ic_vehicle_equipment
        }
    }

    fun customVehicleLabel(
        callSign: String,
        stationName: String,
        department: String,
        equipment: String
    ): String {
        // ✅ 짬처리된 '기타' 데이터가 들어오면 무시하고 재조립
        if (callSign.isNotBlank() && !callSign.contains("기타")) return callSign.trim()

        val knownStations = listOf("송탄", "화성", "평택", "오산", "안성", "수원", "용인", "안산", "시흥", "향남", "장안")
        val foundStation = knownStations.find { department.contains(it) }
        val sName = foundStation ?: stationName.replace("소방서", "").trim()

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
                .filterNot { it.isDigit() }
        }

        var equipShort = equipment.replace("차", "").replace("물탱크", "탱크").trim()

        // ✅ 정밀 텍스트 매칭 (우선순위 절대 엄수)
        equipShort = when {
            equipShort.contains("생활안전") -> "생활안전"
            equipShort.contains("내폭화학") -> "내폭화학"
            equipShort.contains("화학") -> "화학"
            equipShort.contains("버스") -> "버스"
            equipShort.contains("회복") -> "회복"
            equipShort.contains("구조공작") -> "구조공작"
            equipShort.contains("장비운반") -> "장비운반"
            equipShort.contains("구조대(기타)") || equipShort.contains("(기타)") || equipShort.contains("기타") -> "장비운반"
            else -> equipShort
        }

        return "$prefix$equipShort"
    }

    fun deptLabel(raw: String): String {
        val base = raw.trim().replace("소방서", "").replace("센터", "센터").replace("지역대", "지역대")
        val noParen = base.replace(Regex("\\(.*?\\)"), "").trim()
        val short = noParen.removeSuffix("센터").removeSuffix("지역대").removeSuffix("구조대").removeSuffix("지휘단").trim()
        return "(${if (short.isBlank()) base else short})"
    }
}