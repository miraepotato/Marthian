package com.example.marthianclean.ui.sticker

import com.example.marthianclean.R

object VehicleIconMapper {

    fun iconResForEquip(equip: String): Int {
        val e = equip.trim()

        return when {
            e.contains("구급") -> R.drawable.ic_vehicle_ambulance

            e.contains("펌프") -> R.drawable.ic_vehicle_pump
            e.contains("탱크") -> R.drawable.ic_vehicle_tank

            e.contains("화학") -> R.drawable.ic_vehicle_hazmat

            e.contains("굴절") -> R.drawable.ic_vehicle_articulating_ladder
            e.contains("고가") || e.contains("사다리") -> R.drawable.ic_vehicle_ladder

            e.contains("구조") -> R.drawable.ic_vehicle_rescue

            e.contains("장비") || e.contains("운반") -> R.drawable.ic_vehicle_equipment

            e.contains("포클") || e.contains("굴삭") -> R.drawable.ic_vehicle_excavator

            e.contains("회복") || e.contains("버스") -> R.drawable.ic_vehicle_recovery_bus

            e.contains("지휘") -> R.drawable.ic_vehicle_command

            e.contains("무인방수") || e.contains("방수") || e.contains("파괴") -> R.drawable.ic_vehicle_water_breaker

            else -> 0
        }
    }

    fun normalizeDeptName(raw: String): String {
        val s = raw.trim()
        if (s.isEmpty()) return s
        if (s == "구조대") return "화성구조대"
        return s
    }

    fun shortDept(raw: String): String {
        val n = normalizeDeptName(raw)
        if (n.isBlank()) return ""
        if (n == "화성구조대") return "화성"

        return n
            .replace("센터", "")
            .replace("지역대", "")
            .replace("구조대", "구조")
            .trim()
    }

    fun deptTagForSticker(dept: String): String {
        val s = shortDept(dept)
        return if (s.isBlank()) "" else "($s)"
    }
}
