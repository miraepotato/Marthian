package com.example.marthianclean.ui.sticker

import androidx.annotation.DrawableRes
import com.example.marthianclean.R

@DrawableRes
fun iconResForEquip(equip: String): Int {
    val e = equip.replace(" ", "").trim()
    return when {
        e.contains("펌프") -> R.drawable.ic_vehicle_pump
        e.contains("탱크") -> R.drawable.ic_vehicle_tank
        e.contains("구급") -> R.drawable.ic_vehicle_ambulance
        e.contains("지휘") -> R.drawable.ic_vehicle_command
        e.contains("화학") -> R.drawable.ic_vehicle_hazmat
        e.contains("회복지원") -> R.drawable.ic_vehicle_recovery_bus
        e.contains("고가사다리") -> R.drawable.ic_vehicle_ladder
        e.contains("굴절") -> R.drawable.ic_vehicle_articulating_ladder
        e.contains("구조공작") -> R.drawable.ic_vehicle_rescue
        e.contains("무인방수파괴") -> R.drawable.ic_vehicle_water_breaker
        e.contains("장비운반") -> R.drawable.ic_vehicle_equipment
        e.contains("포클레인") -> R.drawable.ic_vehicle_excavator
        else -> R.drawable.ic_vehicle_excavator // 마지막 안전장치(일단 포클레인으로 fallback)
    }
}

fun normalizeDeptName(raw: String): String {
    val s = raw.trim()
    return if (s == "구조대") "화성구조대" else s
}

fun shortDept(raw: String): String {
    val d = normalizeDeptName(raw)
        .replace("센터", "")
        .replace("지휘단", "지휘")
        .replace("구조대", "화성구조대")
        .trim()

    // 너무 길면 앞 2~3글자
    return when {
        d.length <= 3 -> d
        else -> d.take(2)
    }
}

fun deptTagForSticker(dept: String): String {
    val d = normalizeDeptName(dept)
    return if (d == "화성구조대") "(화성)" else "(${shortDept(d)})"
}
