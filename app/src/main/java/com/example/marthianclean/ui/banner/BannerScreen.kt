package com.example.marthianclean.ui.banner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val MarsOrange = Color(0xFFFF8C00)
private val MarsLightOrange = Color(0xFFFFA040)

@Composable
fun BannerScreen(
    onFinished: () -> Unit
) {
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

        Canvas(modifier = Modifier.fillMaxSize()) {

            val center = center
            val radius = size.minDimension * 0.26f

            // 코로나 안개
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MarsLightOrange.copy(alpha = 0.45f),
                        MarsOrange.copy(alpha = 0.25f),
                        MarsOrange.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 2.2f
                ),
                radius = radius * 2.2f,
                center = center
            )

            // 내부 검은 원
            drawCircle(
                color = Color.Black,
                radius = radius,
                center = center
            )

            // 주황색 실선 테두리
            drawCircle(
                color = MarsOrange,
                radius = radius,
                center = center,
                style = Stroke(width = 6f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Marthian",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MarsOrange
            )

            Text(
                text = "produced by Choe CheolHwan",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}
