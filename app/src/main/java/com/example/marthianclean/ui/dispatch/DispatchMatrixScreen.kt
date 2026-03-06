@file:OptIn(ExperimentalFoundationApi::class)

package com.example.marthianclean.ui.dispatch

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marthianclean.viewmodel.BlackboardViewModel
import com.example.marthianclean.viewmodel.IncidentViewModel
import com.naver.maps.geometry.LatLng
import kotlinx.coroutines.launch

/* COLORS */
private val BackgroundBlack = Color(0xFF0E0E0E)
private val TextPrimary = Color(0xFFF0F0F0)
private val OrangePrimary = Color(0xFFFF9800)
private val BorderGray = Color(0xFF2E2E2E)

/* SIZES */
private val HeaderHeight = 48.dp
private val CellHeight = 46.dp
private val ColumnWidth = 140.dp
private val RowHeaderWidth = 220.dp
private val CellPadding = 4.dp

@Composable
fun DispatchMatrixScreen(
    incidentViewModel: IncidentViewModel,
    blackboardViewModel: BlackboardViewModel,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val hScroll = rememberScrollState()
    val vScroll = rememberScrollState()

    // 1. 상태 동기화 유지
    val departments = remember { mutableStateListOf<String>() }
    val equipments = remember { mutableStateListOf<String>() }
    val matrix = remember { mutableStateListOf<SnapshotStateList<Int>>() }

    fun syncAllToVm() {
        incidentViewModel.updateDispatchMeta(departments.toList(), equipments.toList())
        incidentViewModel.updateDispatchMatrix(matrix.map { it.toList() })
    }

    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!initialized) {
            val currentIncident = incidentViewModel.incident.value
            val vmDepts = incidentViewModel.dispatchDepartments

            if (vmDepts.isNotEmpty()) {
                departments.addAll(vmDepts)
                equipments.addAll(incidentViewModel.dispatchEquipments)
                incidentViewModel.dispatchMatrix.forEach { row ->
                    matrix.add(row.toMutableStateList())
                }
            } else if (currentIncident != null) {
                val stationName = incidentViewModel.selectedStationName
                val latLng = LatLng(currentIncident.latitude, currentIncident.longitude)

                incidentViewModel.setupDynamicDispatch(context, stationName, latLng)

                departments.addAll(incidentViewModel.dispatchDepartments)
                equipments.addAll(incidentViewModel.dispatchEquipments)
                incidentViewModel.dispatchMatrix.forEach { row ->
                    matrix.add(row.toMutableStateList())
                }
                syncAllToVm()
            }
            initialized = true
        }
    }

    var editingDept by remember { mutableStateOf<Int?>(null) }
    var editingEquip by remember { mutableStateOf<Int?>(null) }

    // [신규] 부서 추가 다이얼로그 상태 (추후 고도화 가능)
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundBlack)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // [상단 툴바]
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "나가기",
                    color = OrangePrimary,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .clickable { onBack() }
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "완료",
                    color = OrangePrimary,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .clickable {
                            syncAllToVm()
                            incidentViewModel.setMapPreferredZoom(18.0)
                            focusManager.clearFocus()
                            onDone()
                        }
                )
            }

            // [본문 그리드 영역]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, end = 8.dp, bottom = 80.dp) // FAB 공간 확보
            ) {
                // 1) 데이터 셀 그리드 (스크롤 영역)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(vScroll)
                        .horizontalScroll(hScroll)
                ) {
                    Spacer(modifier = Modifier.height(HeaderHeight))

                    departments.forEachIndexed { r, _ ->
                        Row {
                            Spacer(modifier = Modifier.width(RowHeaderWidth).height(CellHeight))

                            if (r < matrix.size) {
                                matrix[r].forEachIndexed { c, value ->
                                    // ✅ [수정] Toggle 방식 적용
                                    StatusCell(
                                        value = value,
                                        onClick = {
                                            // 0이면 1, 1이면 0으로 즉시 토글
                                            matrix[r][c] = if (value == 0) 1 else 0
                                            syncAllToVm()
                                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(ColumnWidth).height(CellHeight))
                        }
                    }
                    Spacer(modifier = Modifier.height(CellHeight))
                }

                // 2) 상단 고정 헤더 (차량 종류)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(HeaderHeight)
                        .background(BackgroundBlack),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.width(RowHeaderWidth).fillMaxHeight())

                    Row(modifier = Modifier.horizontalScroll(hScroll)) {
                        equipments.forEachIndexed { i, name ->
                            EditableHeaderCell(
                                width = ColumnWidth,
                                height = HeaderHeight,
                                text = name,
                                editing = editingEquip == i,
                                onLongPress = {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    editingEquip = i
                                },
                                onDone = {
                                    editingEquip = null
                                    focusManager.clearFocus()
                                    syncAllToVm()
                                },
                                onTextChange = {
                                    equipments[i] = it
                                    syncAllToVm()
                                }
                            )
                        }

                        // 차량 헤더 추가 기능 (필요 시 유지)
                        AddHeaderCell(text = "+ 차량", width = ColumnWidth, height = HeaderHeight) {
                            equipments.add("신규차량")
                            matrix.forEach { it.add(0) }
                            editingEquip = equipments.lastIndex
                            syncAllToVm()
                        }
                    }
                }

                // 3) 좌측 고정 열 (부서명)
                Column(
                    modifier = Modifier
                        .width(RowHeaderWidth)
                        .fillMaxHeight()
                        .background(BackgroundBlack)
                ) {
                    Spacer(modifier = Modifier.height(HeaderHeight))

                    Column(modifier = Modifier.verticalScroll(vScroll)) {
                        departments.forEachIndexed { r, dept ->
                            EditableHeaderCell(
                                width = RowHeaderWidth,
                                height = CellHeight,
                                text = dept, // ✅ 부서명(CallSign 연동 가능) 표시
                                editing = editingDept == r,
                                onLongPress = {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    editingDept = r
                                },
                                onDone = {
                                    editingDept = null
                                    focusManager.clearFocus()
                                    syncAllToVm()
                                },
                                onTextChange = {
                                    departments[r] = it
                                    syncAllToVm()
                                }
                            )
                        }
                    }
                }

                // 4) 코너 덮개
                Box(
                    modifier = Modifier
                        .width(RowHeaderWidth)
                        .height(HeaderHeight)
                        .background(BackgroundBlack)
                )
            }
        }

        // [신규] 좌측 하단 '출동 부서 추가' 플로팅 버튼 (FAB)
        FloatingActionButton(
            onClick = {
                // 부서 추가 로직 트리거: 신규 열 생성 및 매트릭스 확장
                departments.add("신규 부서(수동)")
                matrix.add(mutableStateListOf<Int>().apply {
                    repeat(equipments.size) { add(0) }
                })
                editingDept = departments.lastIndex
                syncAllToVm()

                coroutineScope.launch {
                    vScroll.animateScrollTo(vScroll.maxValue)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp),
            containerColor = Color(0xFF2E2E2E),
            contentColor = OrangePrimary,
            shape = CircleShape
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Add, contentDescription = "부서 추가")
                Spacer(modifier = Modifier.width(8.dp))
                Text("출동 부서 추가", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StatusCell(value: Int, onClick: () -> Unit) {
    // ✅ [수정] 0: 미편성(검정), 1: 편성(주황) 토글 구조 확립
    val isChecked = value == 1
    val bgColor = if (isChecked) OrangePrimary else BackgroundBlack
    val textColor = if (isChecked) Color.Black else TextPrimary
    val label = if (isChecked) "편성" else ""

    Box(
        modifier = Modifier
            .width(ColumnWidth)
            .height(CellHeight)
            .padding(CellPadding)
            .border(1.dp, BorderGray)
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditableHeaderCell(
    width: Dp,
    height: Dp,
    text: String,
    editing: Boolean,
    onLongPress: () -> Unit,
    onDone: () -> Unit,
    onTextChange: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .padding(CellPadding)
            .border(1.dp, BorderGray)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = onLongPress
            ),
        contentAlignment = Alignment.Center
    ) {
        if (editing) {
            LaunchedEffect(Unit) { focusRequester.requestFocus() }

            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                singleLine = true,
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    lineHeight = 15.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
            )
        } else {
            Text(text = text, color = TextPrimary, fontSize = 15.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun AddHeaderCell(
    text: String,
    width: Dp,
    height: Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .padding(CellPadding)
            .border(1.dp, BorderGray)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = OrangePrimary, fontSize = 15.sp)
    }
}