package com.example.marthianclean.ui.sticker

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val TextPrimary = Color(0xFFF0F0F0)

/**
 * ✅ 트레이/프리뷰 공용 칩
 * - 아이콘 + (부서)만 표기
 * - deptTagForSticker 같은 옛 함수명 싹 제거 -> VehicleIconMapper.deptLabel 로 통일
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
            fontWeight = FontWeight.SemiBold
        )
    }
}
