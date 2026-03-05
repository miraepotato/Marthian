package com.example.marthianclean.ui.field

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldSelectScreen(
    onStationSelected: (String) -> Unit, // 소방서 터치 시 주소 검색으로 넘어가는 람다
    onPastIncidents: () -> Unit          // 지난 현장 보기 람다
) {
    val stationNames = listOf(
        "가평소방서", "고양소방서", "과천소방서", "광명소방서", "광주소방서",
        "구리소방서", "군포소방서", "김포소방서", "남양주소방서", "동두천소방서",
        "부천소방서", "분당소방서", "성남소방서", "송탄소방서", "수원남부소방서",
        "수원소방서", "수지소방서", "시흥소방서", "안산소방서", "안성소방서",
        "안양소방서", "양주소방서", "양평소방서", "여주소방서", "연천소방서",
        "오산소방서", "용인서부소방서", "용인소방서", "의왕소방서", "의정부소방서",
        "이천소방서", "일산소방서", "파주소방서", "평택소방서", "포천소방서",
        "하남소방서", "화성소방서"
    ).sorted()

    Scaffold(
        containerColor = Color(0xFF0E0E0E),
        topBar = {
            TopAppBar(
                title = { Text("Marthian (마션) 홈", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0E0E0E))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // 1. 지난 현장 버튼 (상단 배치)
            Button(
                onClick = onPastIncidents,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1C1C1C),
                    contentColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E2E))
            ) {
                Text("📂 지난 현장 불러오기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color(0xFF2E2E2E))
            Spacer(modifier = Modifier.height(24.dp))

            // 2. 새로운 현장 생성 영역 (소방서 다이렉트 선택)
            Text(
                text = "🚨 새로운 현장 생성 (관할 소방서 선택)",
                color = Color(0xFFFF8C00), // MarsOrange
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "소방서를 터치하면 즉시 주소 검색 화면으로 진입합니다.",
                color = Color.Gray,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 35개 서 그리드 리스트
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(stationNames) { name ->
                    StationItemCard(name = name) {
                        onStationSelected(name)
                    }
                }
            }
        }
    }
}

@Composable
private fun StationItemCard(name: String, onClick: () -> Unit) {
    val isHwaseong = name == "화성소방서"
    val bgColor = if (isHwaseong) Color(0xFFFF8C00).copy(alpha = 0.1f) else Color(0xFF1C1C1C)
    val borderColor = if (isHwaseong) Color(0xFFFF8C00) else Color(0xFF2E2E2E)
    val textColor = if (isHwaseong) Color(0xFFFF8C00) else Color.White

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable { onClick() }
            .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name,
                color = textColor,
                fontSize = 15.sp,
                fontWeight = if (isHwaseong) FontWeight.ExtraBold else FontWeight.Medium
            )
        }
    }
}