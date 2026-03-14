package com.example.marthianclean.ui.field

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldSelectScreen(
    onNewIncident: (String) -> Unit, // 이름 변경: onStationSelected -> onNewIncident
    onPastIncidents: () -> Unit
) {
    // 임시로 저장할 내 소방서 상태 (추후 SharedPreferences 등 영구 저장 로직 연동 필요)
    var myStation by remember { mutableStateOf("화성소방서") }
    var showDialog by remember { mutableStateOf(false) }

    val stationNames = listOf(
        "가평소방서", "고양소방서", "과천소방서", "광명소방서", "광주소방서",
        "구리소방서", "군포소방서", "김포소방서", "남양주소방서", "동두천소방서",
        "부천소방서", "분당소방서", "성남소방서", "송탄소방서", "수원남부소방서",
        "수원소방서", "시흥소방서", "안산소방서", "안성소방서",
        "안양소방서", "양주소방서", "양평소방서", "여주소방서", "연천소방서",
        "오산소방서", "용인서부소방서", "용인소방서", "의왕소방서", "의정부소방서",
        "이천소방서", "일산소방서", "파주소방서", "평택소방서", "포천소방서",
        "하남소방서", "화성소방서"
    ).sorted()

    Scaffold(
        containerColor = Color(0xFF0E0E0E),
        topBar = {
            TopAppBar(
                title = { Text("Martian. V2", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0E0E0E)),
                actions = {
                    // 우상단 설정 아이콘 추가
                    IconButton(onClick = { showDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "소방서 설정",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center, // 세로 중앙 정렬
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 새로운 현장 진입 버튼
            Button(
                onClick = { onNewIncident(myStation) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF8C00), // MarsOrange
                    contentColor = Color.White
                )
            ) {
                Text("새로운 현장 진입", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. 지난 현장 버튼
            Button(
                onClick = onPastIncidents,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1C1C1C),
                    contentColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E2E))
            ) {
                Text("지난 현장 불러오기📂", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "현재 설정된 소방서: $myStation",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }

        // 소방서 선택 다이얼로그 (다크 테마 적용)
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                containerColor = Color(0xFF1C1C1C), // 다이얼로그 배경색 다크 그레이
                titleContentColor = Color.White,    // 타이틀 색상 흰색
                textContentColor = Color.White,     // 본문 텍스트 색상 흰색
                title = { Text("내 소방서 설정", fontWeight = FontWeight.Bold) },
                text = {
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(stationNames) { name ->
                            Text(
                                text = name,
                                fontSize = 16.sp,
                                color = Color.White, // 리스트 아이템 텍스트 색상 흰색 명시
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        myStation = name
                                        showDialog = false
                                    }
                                    .padding(vertical = 12.dp)
                            )
                            HorizontalDivider(color = Color(0xFF2E2E2E), thickness = 0.5.dp) // 구분선 어둡게
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("닫기", color = Color(0xFFFF8C00)) // 오렌지색 확인 버튼
                    }
                }
            )
        }
    }
}