package com.example.marthianclean.ui.sticker

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BgBlack = Color(0xFF0E0E0E)
private val BorderGray = Color(0xFF2E2E2E)
private val TextPrimary = Color(0xFFF0F0F0)

@Composable
fun VehicleSticker(
    deptName: String,
    equipName: String,
    modifier: Modifier = Modifier
) {
    val iconRes = VehicleIconMapper.iconResForEquip(equipName)
    val tag = VehicleIconMapper.deptTagForSticker(deptName)

    Row(
        modifier = modifier
            .border(1.dp, BorderGray)
            .background(BgBlack)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconRes != 0) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(8.dp))
        }

        Text(
            text = "$equipName $tag".trim(),
            color = TextPrimary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}
