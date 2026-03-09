package com.example.marthianclean.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marthianclean.viewmodel.IncidentViewModel

/**
 * [블랙보드 유니버설 매트릭스 UI]
 * IncidentViewModel의 데이터를 받아 소방력 배치 현황을 표 형태로 그립니다.
 */
@Composable
fun BlackboardMatrix(
    viewModel: IncidentViewModel, // ✅ BlackboardViewModel 대신 IncidentViewModel 참조
    modifier: Modifier = Modifier
) {
    // ✅ StateFlow가 아닌 Compose State를 직접 참조하므로 collectAsState()가 필요 없습니다.
    val matrix = viewModel.dispatchMatrix
    val departments = viewModel.dispatchDepartments
    val equipments = viewModel.dispatchEquipments

    if (departments.isEmpty() || equipments.isEmpty() || matrix.isEmpty()) {
        Box(modifier = modifier.fillMaxSize().background(Color(0xFF0E0E0E)), contentAlignment = Alignment.Center) {
            Text("편성된 소방력이 없습니다.", color = Color.Gray, fontSize = 12.sp)
        }
        return
    }

    Column(modifier = modifier.padding(8.dp).background(Color(0xFF0E0E0E))) {
        // 매트릭스 테이블 헤더 (장비 이름)
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1.2f).border(0.5.dp, Color(0xFF2E2E2E)).padding(4.dp)) {
                Text("관할/장비", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            equipments.forEach { eq ->
                Box(modifier = Modifier.weight(1f).border(0.5.dp, Color(0xFF2E2E2E)).padding(4.dp), contentAlignment = Alignment.Center) {
                    Text(eq, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1)
                }
            }
        }

        // 매트릭스 바디 (관할별 차량 대수)
        departments.forEachIndexed { rowIndex, dept ->
            Row(modifier = Modifier.fillMaxWidth()) {
                // 왼쪽 열 (관할센터 이름)
                Box(modifier = Modifier.weight(1.2f).border(0.5.dp, Color(0xFF2E2E2E)).padding(4.dp)) {
                    Text(dept, color = Color.White, fontSize = 11.sp, maxLines = 1)
                }

                // 데이터 열 (차량 대수)
                equipments.forEachIndexed { colIndex, _ ->
                    val count = matrix.getOrNull(rowIndex)?.getOrNull(colIndex) ?: 0
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(0.5.dp, Color(0xFF2E2E2E))
                            .background(if (count > 0) Color(0xFFFF9800) else Color.Transparent)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (count > 0) count.toString() else ".",
                            fontSize = 12.sp,
                            color = if (count > 0) Color.Black else Color.DarkGray,
                            fontWeight = if (count > 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}