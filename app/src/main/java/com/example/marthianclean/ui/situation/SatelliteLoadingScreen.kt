package com.example.marthianclean.ui.situation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SatelliteLoadingScreen(
    onFinished: () -> Unit
) {
    LaunchedEffect(Unit) {
        // 위성 타일/지도 로딩 체감용 (너무 길면 답답하니 600~1200ms 권장)
        delay(900)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(
                text = "위성 지도 불러오는 중…",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
