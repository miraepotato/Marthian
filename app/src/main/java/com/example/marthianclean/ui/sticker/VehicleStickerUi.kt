package com.example.marthianclean.ui.sticker

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        else -> R.drawable.ic_vehicle_excavator
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
        .trim()

    return when {
        d == "화성구조대" -> "화성"
        d.length <= 3 -> d
        else -> d.take(2)
    }
}

fun deptTagForSticker(dept: String): String {
    val d = normalizeDeptName(dept)
    return if (d == "화성구조대") "(화성)" else "(${shortDept(d)})"
}

@Composable
fun VehicleSticker(
    dept: String,
    equip: String,
    isWiggling: Boolean,
    modifier: Modifier = Modifier
) {
    val tag = deptTagForSticker(dept)
    val icon = iconResForEquip(equip)

    val transition = rememberInfiniteTransition(label = "wiggle")
    val rot by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rot"
    )
    val tx by transition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tx"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = equip,
            modifier = Modifier
                .size(44.dp)
                .graphicsLayer {
                    if (isWiggling) {
                        rotationZ = rot
                        translationX = tx
                    }
                }
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = tag,
            fontSize = 10.sp,
            modifier = Modifier.alpha(0.95f)
        )
    }
}
