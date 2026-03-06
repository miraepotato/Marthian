package com.example.marthianclean.ui.sticker

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TextPrimary = Color(0xFFF0F0F0)

/**
 * ✅ 트레이/프리뷰 공용 칩
 */
@Composable
fun VehicleStickerChip(
    department: String,
    equipment: String,
    modifier: Modifier = Modifier
) {
    val iconRes = VehicleIconMapper.iconResForEquip(equipment)
    val label = VehicleIconMapper.deptLabel(department)

    Row(
        modifier = modifier
            .height(40.dp)
            .background(Color(0xFF0E0E0E), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = label,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

/**
 * ✅ 지도/상황판 배치용 실제 차량 스티커
 */
@Composable
fun VehicleMapSticker(
    stationName: String,
    unitName: String,
    equipment: String,
    modifier: Modifier = Modifier
) {
    val iconRes = VehicleIconMapper.iconResForEquip(equipment)

    // ✅ 에러 수정: 업그레이드된 customVehicleLabel 함수 호출 및 파라미터(equipment) 추가 전달
    val customFullLabel = VehicleIconMapper.customVehicleLabel(stationName, unitName, equipment)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 차량 아이콘
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(32.dp)
        )

        Spacer(Modifier.height(2.dp))

        // 2. 지능형 커스텀 라벨 (예: 화성구조, 향남펌프 등)
        Box(
            modifier = Modifier
                .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = customFullLabel,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
        }
    }
}