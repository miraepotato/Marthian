package com.example.marthianclean.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marthianclean.viewmodel.BlackboardViewModel

/**
 * [블랙보드 유니버설 매트릭스 UI]
 * 뷰모델의 데이터를 받아 소방력 배치 현황을 표 형태로 그립니다.
 */
@Composable
fun BlackboardMatrix(
    viewModel: BlackboardViewModel,
    modifier: Modifier = Modifier
) {
    val matrix by viewModel.dynamicMatrix.collectAsState()
    val departments by viewModel.matrixDepartments.collectAsState()
    val equipments by viewModel.matrixEquipments.collectAsState()

    if (departments.isEmpty() || equipments.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("소방력을 배치할 데이터를 불러오는 중입니다...", color = Color.Gray)
        }
        return
    }

    Column(modifier = modifier.padding(8.dp)) {
        // 매트릭스 테이블 헤더 (장비 이름)
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1.2f).border(0.5.dp, Color.LightGray).padding(4.dp)) {
                Text("관할/장비", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            equipments.forEach { eq ->
                Box(modifier = Modifier.weight(1f).border(0.5.dp, Color.LightGray).padding(4.dp), contentAlignment = Alignment.Center) {
                    Text(eq, fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1)
                }
            }
        }

        // 매트릭스 바디 (관할별 차량 대수)
        departments.forEachIndexed { rowIndex, dept ->
            Row(modifier = Modifier.fillMaxWidth()) {
                // 왼쪽 열 (관할센터 이름)
                Box(modifier = Modifier.weight(1.2f).border(0.5.dp, Color.LightGray).padding(4.dp)) {
                    Text(dept, fontSize = 11.sp, maxLines = 1)
                }

                // 데이터 열 (차량 대수)
                equipments.forEachIndexed { colIndex, _ ->
                    val count = matrix.getOrNull(rowIndex)?.getOrNull(colIndex) ?: 0
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(0.5.dp, Color.LightGray)
                            .background(if (count > 0) Color(0xFFFFF3E0) else Color.Transparent)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (count > 0) count.toString() else ".",
                            fontSize = 12.sp,
                            color = if (count > 0) Color.Red else Color.LightGray,
                            fontWeight = if (count > 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}