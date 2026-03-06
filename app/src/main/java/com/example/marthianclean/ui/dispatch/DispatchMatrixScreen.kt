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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marthianclean.viewmodel.BlackboardViewModel
import com.example.marthianclean.viewmodel.IncidentViewModel
import com.naver.maps.geometry.LatLng
import androidx.compose.foundation.shape.RoundedCornerShape
/* COLORS */
private val BackgroundBlack = Color(0xFF0E0E0E)
private val TextPrimary = Color(0xFFF0F0F0)
private val OrangePrimary = Color(0xFFFF9800)
private val NeonGreen = Color(0xFF76FF03)
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
    blackboardViewModel: BlackboardViewModel, // 파라미터는 유지하되, 이 화면에서는 덮어쓰기 기능 제외
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val focusManager = LocalFocusManager.current

    val hScroll = rememberScrollState()
    val vScroll = rememberScrollState()

    val departments = remember { mutableStateListOf<String>() }
    val equipments = remember { mutableStateListOf<String>() }
    val matrix = remember { mutableStateListOf<SnapshotStateList<Int>>() }

    fun syncAllToVm() {
        incidentViewModel.updateDispatchMeta(departments.toList(), equipments.toList())
        incidentViewModel.updateDispatchMatrix(matrix.map { it.toList() })
    }

    var initialized by remember { mutableStateOf(false) }

    // ✅ [핵심수정] 칠판 데이터 덮어쓰기를 원천 차단하고 오직 8개 부서 연산 결과만 사용
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

                // Context 넘겨서 진짜 차량 데이터 합집합 추출
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
    ) {
        // [상단 툴바]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
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
                .padding(8.dp)
        ) {
            // 1) 데이터 셀 그리드
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
                                StatusCell(value) {
                                    matrix[r][c] = (value + 1) % 3
                                    syncAllToVm()
                                }
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

                    AddHeaderCell(
                        text = "차량 추가",
                        width = ColumnWidth,
                        height = HeaderHeight
                    ) {
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
                            text = dept,
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

                    AddHeaderCell(
                        text = "센터 추가",
                        width = RowHeaderWidth,
                        height = CellHeight
                    ) {
                        departments.add("신규센터")
                        matrix.add(
                            mutableStateListOf<Int>().apply {
                                repeat(equipments.size) { add(0) }
                            }
                        )
                        editingDept = departments.lastIndex
                        syncAllToVm()
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

@Composable
fun StatusCell(value: Int, onClick: () -> Unit) {
    val bg = when (value) {
        1 -> OrangePrimary
        2 -> NeonGreen
        else -> BackgroundBlack
    }

    val label = when (value) {
        1 -> "출동"
        2 -> "2"
        else -> ""
    }

    Box(
        modifier = Modifier
            .width(ColumnWidth)
            .height(CellHeight)
            .padding(CellPadding)
            .border(1.dp, BorderGray)
            .background(bg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = if(value > 0) Color.Black else TextPrimary, fontSize = 16.sp)
    }
}