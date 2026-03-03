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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marthianclean.viewmodel.IncidentViewModel

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DispatchMatrixScreen(
    incidentViewModel: IncidentViewModel,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val view = LocalView.current
    val focusManager = LocalFocusManager.current

    val hScroll = rememberScrollState()
    val vScroll = rememberScrollState()

    val defaultDepartments = listOf(
        "향남센터", "양감지역대", "남양센터", "서신지역대", "제부지역대",
        "팔탄센터", "장안센터", "새솔센터", "화성구조대",
        "정남센터", "반송센터", "태안센터", "마도지역대",
        "매송지역대", "목동센터",
        "안중센터(평택)", "포승센터(평택)", "서탄센터(송탄)",
        "세교센터(오산)", "청학센터(오산)", "반월센터(안산)"
    )

    // ✅ 지휘차 항목 영구 삭제 (현장당 1대는 자동 배치되므로)
    val defaultEquipments = listOf(
        "펌프차", "탱크차", "화학차", "고가사다리차",
        "굴절차", "무인방수파괴", "구조공작차",
        "장비운반차", "구급차",
        "포크레인", "회복지원버스"
    )

    val departments = remember { mutableStateListOf<String>() }
    val equipments = remember { mutableStateListOf<String>() }
    val matrix = remember { mutableStateListOf<SnapshotStateList<Int>>() }

    fun syncAllToVm() {
        incidentViewModel.updateDispatchMeta(
            departments = departments.toList(),
            equipments = equipments.toList()
        )
        incidentViewModel.updateDispatchMatrix(
            matrix = matrix.map { it.toList() }
        )
    }

    fun loadFromVmOrDefault() {
        val vmDepts = incidentViewModel.dispatchDepartments
        val vmEquips = incidentViewModel.dispatchEquipments
        val vmMatrix = incidentViewModel.dispatchMatrix

        val useDepts = if (vmDepts.isNotEmpty()) vmDepts else defaultDepartments
        var useEquips = if (vmEquips.isNotEmpty()) vmEquips else defaultEquipments
        var useMatrix = vmMatrix

        // ✅ 과거에 저장된 현장 데이터에 '지휘차'가 남아있다면 자동으로 걸러내고 표를 깔끔하게 잘라냅니다.
        if (useEquips.contains("지휘차")) {
            val removeIdx = useEquips.indexOf("지휘차")
            useEquips = useEquips.filterIndexed { index, _ -> index != removeIdx }
            if (useMatrix.isNotEmpty()) {
                useMatrix = useMatrix.map { row -> row.filterIndexed { index, _ -> index != removeIdx } }
            }
        }

        departments.clear()
        departments.addAll(useDepts)

        equipments.clear()
        equipments.addAll(useEquips)

        matrix.clear()
        val matrixOk =
            useMatrix.isNotEmpty() &&
                    useMatrix.size == useDepts.size &&
                    useMatrix.all { it.size == useEquips.size }

        if (matrixOk) {
            useMatrix.forEach { row ->
                matrix.add(row.toMutableStateList())
            }
        } else {
            repeat(useDepts.size) {
                matrix.add(
                    mutableStateListOf<Int>().apply {
                        repeat(useEquips.size) { add(0) }
                    }
                )
            }
        }

        if (vmDepts.isEmpty() || vmEquips.isEmpty() || vmMatrix.isEmpty()) {
            syncAllToVm()
        }
    }

    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!initialized) {
            loadFromVmOrDefault()
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
                    .border(1.dp, BorderGray)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .clickable { onBack() }
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "완료",
                color = OrangePrimary,
                fontSize = 16.sp,
                modifier = Modifier
                    .border(1.dp, BorderGray)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .clickable {
                        syncAllToVm()
                        incidentViewModel.setMapPreferredZoom(18.0)
                        focusManager.clearFocus()
                        onDone()
                    }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // 1) 본문 그리드
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

                        matrix[r].forEachIndexed { c, value ->
                            StatusCell(value) {
                                matrix[r][c] = (value + 1) % 3
                                syncAllToVm()
                            }
                        }

                        // ✅ [핵심 버그 수정] 헤더에 있는 "차량 추가" 버튼 너비만큼 바디에도 빈칸을 채워줘야 스크롤 시 밀리지 않습니다!!
                        Spacer(modifier = Modifier.width(ColumnWidth).height(CellHeight))
                    }
                }
                Spacer(modifier = Modifier.height(CellHeight))
            }

            // 2) 상단 고정 헤더행
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

            // 3) 좌측 고정 부서열
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

            // 4) 좌상단 코너 덮기
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
            Text(text, color = TextPrimary, fontSize = 15.sp)
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
        Text(text, color = OrangePrimary, fontSize = 15.sp)
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
        Text(label, color = Color.Black, fontSize = 16.sp)
    }
}