package com.example.marthianclean.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 블랙보드(Blackboard) 35개 소방서 선택 인터페이스
 */
@Composable
fun StationSelector(
    onStationSelected: (String) -> Unit,
    onClose: () -> Unit
) {
    // 경기도 35개 소방서 명단 (가나다 순 정렬)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6000000)) // 반투명 검정 배경
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            // 헤더 영역
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "관할 소방서 선택 (BLACKBOARD)",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "닫기", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color(0xFF333333))
            Spacer(modifier = Modifier.height(16.dp))

            // 35개 서 그리드 리스트 (가로 3열 배치)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF2D2D2D),
        tonalElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name,
                color = if (name == "화성소방서") Color(0xFFFF5722) else Color.White, // 화성소방서 강조
                fontSize = 15.sp,
                fontWeight = if (name == "화성소방서") FontWeight.ExtraBold else FontWeight.Medium
            )
        }
    }
}