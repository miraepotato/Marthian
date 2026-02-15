package com.example.marthianclean.ui.field

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MarsOrange = Color(0xFFFF8C00)

@Composable
fun IncidentEditHubScreen(
    onEditMatrix: () -> Unit,
    onEditInfo: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(20.dp)
    ) {
        // ✅ TopAppBar 대신 수동 헤더 (Experimental API 불필요)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "현장 편집", color = Color.White)

            TextButton(onClick = onBack) {
                Text(text = "닫기", color = MarsOrange)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onEditMatrix,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1C1C1C),
                contentColor = Color.White
            )
        ) {
            Text("출동대 편성 (매트릭스)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onEditInfo,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1C1C1C),
                contentColor = Color.White
            )
        ) {
            Text("현장 정보 수정")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "왼쪽 가장자리에서 오른쪽으로 슬라이드하면 이 화면이 열립니다.",
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}
