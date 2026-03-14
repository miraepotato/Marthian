package com.example.marthianclean.ui.banner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp  // dp 에러 해결을 위한 임포트
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ui.theme.Color.kt에 있는 색상을 사용합니다.
import com.example.marthianclean.ui.theme.MarsOrange
import com.example.marthianclean.ui.theme.MarsLightOrange

@Composable
fun BannerScreen(
    onFinished: () -> Unit
) {
    // 5초 동안 화성 로고를 보여준 뒤 메인 지도로 이동합니다.
    LaunchedEffect(Unit) {
        delay(5000L)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // 배경 화성 애니메이션 그래픽
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = center
            val radius = size.minDimension * 0.26f

            // 코로나 안개 효과
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MarsLightOrange.copy(alpha = 0.45f),
                        MarsOrange.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 2.2f
                ),
                radius = radius * 2.2f,
                center = center
            )

            // 내부 검은 원 (일식 효과)
            drawCircle(color = Color.Black, radius = radius, center = center)

            // 주황색 실선 테두리
            drawCircle(
                color = MarsOrange,
                radius = radius,
                center = center,
                style = Stroke(width = 6f)
            )
        }

        // 중앙 텍스트 로고
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Martian V2.0",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MarsOrange
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "produced by Choe CheolHwan",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}