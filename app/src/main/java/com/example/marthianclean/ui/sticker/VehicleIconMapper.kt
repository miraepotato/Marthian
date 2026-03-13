package com.example.marthianclean.ui.sticker

import com.example.marthianclean.R

object VehicleIconMapper {
    fun iconResForEquip(equip: String): Int {
        val e = equip.trim()
        return when {
            e.contains("회복지원버스") -> R.drawable.ic_vehicle_recovery_bus
            e.contains("화학차") -> R.drawable.ic_vehicle_hazmat
            e.contains("구조공작차") -> R.drawable.ic_vehicle_rescue
            e.contains("펌프차") -> R.drawable.ic_vehicle_pump
            e.contains("미니펌프차") -> R.drawable.ic_vehicle_pump // 미니 전용 아이콘 없으면 펌프로
            e.contains("험지펌프") -> R.drawable.ic_vehicle_rough_terrain
            e.contains("산불진화차") -> R.drawable.ic_vehicle_forest_fire
            e.contains("탱크차") -> R.drawable.ic_vehicle_tank
            e.contains("고가차") -> R.drawable.ic_vehicle_ladder
            e.contains("굴절차") -> R.drawable.ic_vehicle_articulating_ladder
            e.contains("구급차") -> R.drawable.ic_vehicle_ambulance
            e.contains("조명차") -> R.drawable.ic_vehicle_lighting
            e.contains("배연차") -> R.drawable.ic_vehicle_smoke_extraction
            e.contains("무인방수파괴차") -> R.drawable.ic_vehicle_water_breaker
            e.contains("지휘차") -> R.drawable.ic_vehicle_command
            e.contains("지원차") -> R.drawable.ic_vehicle_equipment
            e.contains("장비운반차") -> R.drawable.ic_vehicle_equipment
            e.contains("생활안전차") -> R.drawable.ic_vehicle_equipment
            e.contains("제독차") -> R.drawable.ic_vehicle_decontamination
            else -> R.drawable.ic_vehicle_equipment
        }
    }

    fun customVehicleLabel(
        callSign: String,
        stationName: String,
        department: String,
        equipment: String
    ): String {
        // ✅ '기타'로 짬처리된 데이터가 들어와도 무시하고 아래 로직에서 재조립함
        val cleanCallSign = if (callSign.contains("기타")) "" else callSign.trim()
        if (cleanCallSign.isNotBlank()) return cleanCallSign

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

        // ✅ 정밀 텍스트 매칭 (버스 우선순위 상향)
        equipShort = when {
            equipShort.contains("생활안전") -> "생활안전"
            equipShort.contains("내폭화학") -> "내폭화학"
            equipShort.contains("화학") -> "화학"
            equipShort.contains("버스") -> "버스"
            equipShort.contains("회복") -> "회복"
            equipShort.contains("구조공작") -> "구조공작"
            equipShort.contains("장비운반") -> "장비운반"
            equipShort.contains("기타") -> "장비운반"
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