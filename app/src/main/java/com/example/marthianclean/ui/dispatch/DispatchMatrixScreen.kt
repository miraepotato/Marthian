@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.marthianclean.ui.dispatch

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marthianclean.viewmodel.BlackboardViewModel
import com.example.marthianclean.viewmodel.IncidentViewModel
import com.naver.maps.geometry.LatLng

private val BackgroundBlack = Color(0xFF0E0E0E)
private val TextWhite = Color(0xFFFFFFFF)
private val OrangePrimary = Color(0xFFFF9800)
private val BorderGray = Color(0xFF2E2E2E)
private val CellWidth = 150.dp // ✅ 긴 이름도 여유 있게 들어가는 사이즈

@Composable
fun DispatchMatrixScreen(
    incidentViewModel: IncidentViewModel,
    blackboardViewModel: BlackboardViewModel,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    var initialized by remember { mutableStateOf(false) }
    val selectedCallsigns = remember { mutableStateListOf<String>() }
    var showAddDialog by remember { mutableStateOf(false) }

    val depts = incidentViewModel.dispatchDepartments

    LaunchedEffect(Unit) {
        if (!initialized) {
            val currentIncident = incidentViewModel.incident.value
            val vmDepts = incidentViewModel.dispatchDepartments

            if (vmDepts.isEmpty() && currentIncident != null) {
                val stationName = incidentViewModel.selectedStationName
                val latLng = LatLng(currentIncident.latitude, currentIncident.longitude)
                incidentViewModel.setupDynamicDispatch(context, stationName, latLng)
            }
            initialized = true
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundBlack)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 상단 툴바
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "나가기", color = OrangePrimary, fontSize = 16.sp,
                    modifier = Modifier.border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp).clickable { onBack() }
                )
                Text(
                    text = "편성 완료", color = BackgroundBlack, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(OrangePrimary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 20.dp, vertical = 10.dp).clickable {
                            incidentViewModel.setMapPreferredZoom(18.0)
                            onDone()
                        }
                )
            }

            // 헤더
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("부서명", color = OrangePrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(130.dp))
                Text("보유 차량 목록", color = OrangePrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = BorderGray, thickness = 1.dp)

            // 리스트
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(depts) { dept ->
                    val vehicles = blackboardViewModel.getVehiclesForCenter(dept)

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dept, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(130.dp)
                        )

                        Row(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                            if (vehicles.isEmpty()) {
                                Text("차량 데이터 없음", color = Color.DarkGray, fontSize = 14.sp)
                            } else {
                                vehicles.forEach { vehicle ->
                                    val isSelected = selectedCallsigns.contains(vehicle.callsign)

                                    Box(
                                        modifier = Modifier
                                            .width(CellWidth)
                                            .height(56.dp) // 두 줄을 위해 높이 고정
                                            .padding(end = 8.dp)
                                            .background(if (isSelected) OrangePrimary else Color.Transparent, RoundedCornerShape(6.dp))
                                            .clickable {
                                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                                if (isSelected) selectedCallsigns.remove(vehicle.callsign)
                                                else selectedCallsigns.add(vehicle.callsign)

                                                // ✅ 미등록 차량 터치 시 자동 확장 로직
                                                var c = incidentViewModel.dispatchEquipments.indexOf(vehicle.type)
                                                if (c == -1) {
                                                    val newEquips = incidentViewModel.dispatchEquipments.toMutableList()
                                                    newEquips.add(vehicle.type)
                                                    incidentViewModel.updateDispatchMeta(incidentViewModel.dispatchDepartments, newEquips)

                                                    val newMatrix = incidentViewModel.dispatchMatrix.map { it.toMutableList().apply { add(0) } }
                                                    incidentViewModel.updateDispatchMatrix(newMatrix)
                                                    c = newEquips.lastIndex
                                                }

                                                val r = incidentViewModel.dispatchDepartments.indexOf(dept)
                                                if (r != -1) {
                                                    val currentMatrix = incidentViewModel.dispatchMatrix.map { it.toMutableList() }.toMutableList()
                                                    currentMatrix[r][c] = if (!isSelected) currentMatrix[r][c] + 1 else maxOf(0, currentMatrix[r][c] - 1)
                                                    incidentViewModel.updateDispatchMatrix(currentMatrix)
                                                }
                                            }
                                            .padding(horizontal = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = vehicle.callsign,
                                            color = if (isSelected) BackgroundBlack else TextWhite,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2, // ✅ 긴 텍스트 2줄 지원
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = BorderGray, thickness = 0.5.dp)
                }
            }
        }

        // 추가 편성 버튼
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 16.dp).height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = BackgroundBlack),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("+ 추가 편성", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        // 추가 편성 다이얼로그 (내 소방서 먼저, 그다음 가나다순 정렬)
        if (showAddDialog) {
            val allCenters = blackboardViewModel.getAllCenters(incidentViewModel.selectedStationName)
            val availableCenters = allCenters.filter { !depts.contains(it) }

            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                containerColor = Color(0xFF1C1C1C),
                title = { Text("인근 출동대 추가", color = TextWhite, fontWeight = FontWeight.Bold) },
                text = {
                    if (availableCenters.isEmpty()) {
                        Text("추가할 수 있는 부서가 없습니다.", color = Color.Gray)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                            items(availableCenters) { center ->
                                Text(
                                    text = center, fontSize = 16.sp, color = TextWhite,
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                                        // ✅ 부서를 맨 마지막의 '민간' 바로 위에 삽입
                                        val newDepts = incidentViewModel.dispatchDepartments.toMutableList()
                                        val privateIdx = newDepts.indexOf("민간")
                                        if (privateIdx != -1) newDepts.add(privateIdx, center)
                                        else newDepts.add(center)

                                        incidentViewModel.updateDispatchMeta(newDepts, incidentViewModel.dispatchEquipments)

                                        val newMatrix = incidentViewModel.dispatchMatrix.map { it.toMutableList() }.toMutableList()
                                        val newRow = MutableList(incidentViewModel.dispatchEquipments.size) { 0 }
                                        if (privateIdx != -1) newMatrix.add(privateIdx, newRow)
                                        else newMatrix.add(newRow)

                                        incidentViewModel.updateDispatchMatrix(newMatrix)
                                        showAddDialog = false
                                    }.padding(vertical = 12.dp)
                                )
                                HorizontalDivider(color = BorderGray, thickness = 0.5.dp)
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showAddDialog = false }) { Text("닫기", color = OrangePrimary) } }
            )
        }
    }
}