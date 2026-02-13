package com.example.marthianclean.ui.situation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun SatelliteLoadingScreen(
    onFinished: () -> Unit
) {

    var dots by remember { mutableStateOf("") }

    // 랜덤 3~5초 후 이동
    LaunchedEffect(Unit) {
        val randomDelay = Random.nextLong(3000L, 5000L)
        delay(randomDelay)
        onFinished()
    }

    // ... 애니메이션
    LaunchedEffect(Unit) {
        while (true) {
            dots = when (dots) {
                "" -> "."
                "." -> ".."
                ".." -> "..."
                else -> ""
            }
            delay(400)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "SATELLITE LOADING$dots",
            color = Color.White,
            fontSize = 18.sp
        )
    }
}
