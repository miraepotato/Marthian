package com.example.marthianclean.ui.situation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

@Composable
fun SatelliteLoadingScreen(
    onFinished: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(700) // 0.7초 정도 문구 보여주고
        onFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "SATELLITE LOADING...",
            color = Color.White
        )
    }
}
