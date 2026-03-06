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
import com.example.marthianclean.R

private val TextPrimary = Color(0xFFF0F0F0)

/**
 * ✅ 트레이/목록용 차량 칩 (무선호출명 우선 표시)
 */
@Composable
fun VehicleStickerChip(
    callSign: String = "",
    stationName: String = "",
    department: String,
    equipment: String,
    modifier: Modifier = Modifier
) {
    val iconRes = VehicleIconMapper.iconResForEquip(equipment)

    // 무선호출명이 있으면 호출명으로, 없으면 지능형 라벨로 생성
    val label = VehicleIconMapper.customVehicleLabel(callSign, stationName, department, equipment)

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
 * - 형님께서 직접 제작하신 고유 아이콘들과 무선호출명이 결합됩니다.
 */
@Composable
fun VehicleMapSticker(
    callSign: String,      // 현장 무선호출명 (예: 향남펌프, 수지조명)
    stationName: String,   // 소속 소방서 (예: 화성소방서)
    unitName: String,      // 소속 부서 (예: 향남119안전센터)
    equipment: String,     // 장비 종류 (예: 펌프차, 조명차)
    modifier: Modifier = Modifier
) {
    val iconRes = VehicleIconMapper.iconResForEquip(equipment)

    // ✅ 업데이트된 Mapper 로직 적용: 4개의 파라미터를 정확히 전달
    val customFullLabel = VehicleIconMapper.customVehicleLabel(
        callSign = callSign,
        stationName = stationName,
        department = unitName,
        equipment = equipment
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 차량 아이콘 (형님께서 제작하신 신규 아이콘 반영)
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(32.dp)
        )

        Spacer(Modifier.height(2.dp))

        // 2. 무선호출명 라벨 (검정 반투명 배경에 흰색 글씨로 시인성 확보)
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