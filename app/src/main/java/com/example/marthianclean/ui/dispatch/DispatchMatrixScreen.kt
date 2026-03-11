@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.marthianclean.ui.dispatch

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.marthianclean.viewmodel.BlackboardViewModel
import com.example.marthianclean.viewmodel.IncidentViewModel
import com.example.marthianclean.viewmodel.MarthianDepartment
import com.example.marthianclean.viewmodel.DisasterMode
import com.naver.maps.geometry.LatLng

private val BackgroundBlack = Color(0xFF0E0E0E)
private val TextWhite = Color(0xFFFFFFFF)
private val OrangePrimary = Color(0xFFFF9800)
private val BorderGray = Color(0xFF2E2E2E)
private val SurfaceDark = Color(0xFF1C1C1C)
private val CellWidth = 150.dp

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

    // 🚀 모드 선택창 상태
    var showModeDialog by remember { mutableStateOf(false) }
    val currentMode by incidentViewModel.currentMode.collectAsState()

    val depts = incidentViewModel.dispatchDepartments

    LaunchedEffect(Unit) {
        if (!initialized) {
            if (incidentViewModel.allDepartments.value.isEmpty()) {
                incidentViewModel.loadFireData(context)
            }

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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "나가기", color = OrangePrimary, fontSize = 16.sp,
                    modifier = Modifier.border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp).clickable { onBack() }
                )

                Spacer(modifier = Modifier.weight(1f))

                // 🚀 핵심: 특수재난 모드 전환 버튼
                Box(
                    modifier = Modifier
                        .background(if(currentMode == DisasterMode.NORMAL) Color.Transparent else OrangePrimary, RoundedCornerShape(4.dp))
                        .border(1.dp, OrangePrimary, RoundedCornerShape(4.dp))
                        .clickable { showModeDialog = true }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = when(currentMode) {
                            DisasterMode.NORMAL -> "⚠️ 특수재난 모드"
                            DisasterMode.WATER -> "⚓ 수난구조 모드"
                            DisasterMode.APARTMENT -> "🏢 공동주택 모드"
                            else -> "모드 선택"
                        },
                        color = if(currentMode == DisasterMode.NORMAL) OrangePrimary else BackgroundBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

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
                                            .height(56.dp)
                                            .padding(end = 8.dp)
                                            .background(if (isSelected) OrangePrimary else Color.Transparent, RoundedCornerShape(6.dp))
                                            .border(1.dp, if(isSelected) Color.Transparent else BorderGray, RoundedCornerShape(6.dp))
                                            .clickable {
                                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                                if (isSelected) selectedCallsigns.remove(vehicle.callsign)
                                                else selectedCallsigns.add(vehicle.callsign)

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
                                            maxLines = 2,
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

        // 🚀 특수재난 선택 다이얼로그
        if (showModeDialog) {
            ModeSelectionDialog(
                onModeSelected = {
                    incidentViewModel.setDisasterMode(it)
                    showModeDialog = false
                },
                onDismiss = { showModeDialog = false }
            )
        }

        if (showAddDialog) {
            AddDispatchDialog(
                incidentViewModel = incidentViewModel,
                onDismiss = { showAddDialog = false },
                onDepartmentSelected = { selectedDept ->
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                    val newDepts = incidentViewModel.dispatchDepartments.toMutableList()
                    val newMatrix = incidentViewModel.dispatchMatrix.map { it.toMutableList() }.toMutableList()

                    if (!newDepts.contains(selectedDept.deptName)) {
                        val privateIdx = newDepts.indexOf("민간")
                        val insertIdx = if (privateIdx != -1) privateIdx else newDepts.size

                        newDepts.add(insertIdx, selectedDept.deptName)

                        val newRow = MutableList(incidentViewModel.dispatchEquipments.size) { 0 }

                        newMatrix.add(insertIdx, newRow)
                        incidentViewModel.updateDispatchMeta(newDepts, incidentViewModel.dispatchEquipments)
                        incidentViewModel.updateDispatchMatrix(newMatrix)
                    }

                    showAddDialog = false
                }
            )
        }
    }
}

// 🚀 특수재난 모드 선택 다이얼로그
@Composable
fun ModeSelectionDialog(
    onModeSelected: (DisasterMode) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SurfaceDark,
            border = BorderStroke(1.dp, BorderGray)
        ) {
            Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                Text("특수재난 전술 모드 선택", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))

                // 모드별 버튼들
                ModeButton("🚒 일반 화재/구조", DisasterMode.NORMAL, onModeSelected)
                ModeButton("⚓ 수난구조 (수색/범위)", DisasterMode.WATER, onModeSelected)
                ModeButton("🏢 공동주택 (인명수색)", DisasterMode.APARTMENT, onModeSelected)

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("취소", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun ModeButton(label: String, mode: DisasterMode, onSelected: (DisasterMode) -> Unit) {
    Button(
        onClick = { onSelected(mode) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BorderGray),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(label, color = TextWhite, textAlign = TextAlign.Left, modifier = Modifier.fillMaxWidth())
    }
}

/* ================= 거리순 추천 및 검색 다이얼로그 ================= */
@Composable
fun AddDispatchDialog(
    incidentViewModel: IncidentViewModel,
    onDismiss: () -> Unit,
    onDepartmentSelected: (MarthianDepartment) -> Unit
) {
    val allDepts by incidentViewModel.allDepartments.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val currentDepts = incidentViewModel.dispatchDepartments

    val normalizeName: (String) -> String = { name ->
        name.replace("119", "").replace("안전", "").replace(" ", "")
    }
    val normalizedCurrentDepts = currentDepts.map { normalizeName(it) }

    val cleanQuery = searchQuery.replace("\\s".toRegex(), "")

    val displayList = if (cleanQuery.isBlank()) {
        allDepts.filter { dept ->
            !normalizedCurrentDepts.contains(normalizeName(dept.deptName))
        }.sortedBy { it.distance }.take(10)
    } else {
        allDepts.filter { dept ->
            !normalizedCurrentDepts.contains(normalizeName(dept.deptName)) &&
                    (normalizeName(dept.deptName).contains(cleanQuery, ignoreCase = true) ||
                            normalizeName(dept.station).contains(cleanQuery, ignoreCase = true))
        }.sortedBy { it.distance }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1C1C1C),
            border = BorderStroke(1.dp, Color(0xFF2E2E2E))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .padding(16.dp)
            ) {
                Text("인근 출동대 추가", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("부서명 검색 (예: 평택, 구조)", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF9800),
                        unfocusedBorderColor = Color(0xFF2E2E2E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFFFF9800)
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { keyboardController?.hide() }
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (cleanQuery.isBlank()) "📍 현장 근거리 우선 추천 (Top 10)" else "🔍 검색 결과",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (allDepts.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFFF9800))
                    }
                } else if (displayList.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("검색된 부서가 없습니다.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(displayList) { dept ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onDepartmentSelected(dept) }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(dept.deptName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(dept.station, color = Color.Gray, fontSize = 12.sp)
                                }
                                Text(String.format("%.1f km", dept.distance), color = Color(0xFFFF9800), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            HorizontalDivider(color = Color(0xFF2E2E2E), thickness = 1.dp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E2E)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("닫기", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}