package com.example.marthianclean

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainScreen() {

    var count by remember { mutableStateOf(0) }

    // count 값에 따라 보여줄 메시지 결정
    val message = when {
        count < 3 -> "아직 시작 단계"
        count < 6 -> "슬슬 감 잡는 중"
        else -> "이제 좀 한다 🔥"
    }

    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(text = "버튼 클릭 횟수: $count")
        Text(
            text = message,
            modifier = Modifier.padding(top = 8.dp)
        )

        Button(
            onClick = { count++ },
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text(text = "눌러봐")
        }
    }
}
