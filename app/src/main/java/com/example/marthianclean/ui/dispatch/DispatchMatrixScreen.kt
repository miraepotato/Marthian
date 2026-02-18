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

    // ✅ 스크롤 동기화
    val hScroll = rememberScrollState()
    val vScroll = rememberScrollState()

    // ✅ 기본값(처음 1회만 쓰일 값)
    val defaultDepartments = listOf(
        "향남센터", "양감지역대", "남양센터", "서신지역대", "제부지역대",
        "팔탄센터", "장안센터", "새솔센터", "화성구조대",
        "정남센터", "반송센터", "태안센터", "마도지역대",
        "매송지역대", "목동센터",
        "안중센터(평택)", "포승센터(평택)", "서탄센터(송탄)",
        "세교센터(오산)", "청학센터(오산)", "반월센터(안산)"
    )
    val defaultEquipments = listOf(
        "펌프차", "탱크차", "화학차", "고가사다리차",
        "굴절차", "무인방수파괴", "구조공작차",
        "장비운반차", "구급차"
    )

    // ✅ 화면 상태
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
        val useEquips = if (vmEquips.isNotEmpty()) vmEquips else defaultEquipments

        departments.clear()
        departments.addAll(useDepts)

        equipments.clear()
        equipments.addAll(useEquips)

        matrix.clear()
        val matrixOk =
            vmMatrix.isNotEmpty() &&
                    vmMatrix.size == useDepts.size &&
                    vmMatrix.all { it.size == useEquips.size }

        if (matrixOk) {
            vmMatrix.forEach { row ->
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

        // ✅ VM이 비어 있어서 default로 만든 경우에만 1회 저장
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
        // 상단 버튼 바
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

                        // ✅ 출동대 편성 완료 후, 상황판에서 줌인 유지(복원)용
                        incidentViewModel.setMapPreferredZoom(18.0)
                        focusManager.clearFocus()
                        onDone()
                    }
            )
        }

        // ✅ 틀고정 레이아웃(정렬 완전 고정)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            /*
             * 1) 본문 그리드(수평/수직 스크롤)
             *    - 헤더행/좌측열은 공간만 비워둠
             */
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(vScroll)
                    .horizontalScroll(hScroll)
            ) {
                // 상단 헤더행 공간(정확히 HeaderHeight)
                Spacer(modifier = Modifier.height(HeaderHeight))

                // 데이터 행
                departments.forEachIndexed { r, _ ->
                    Row {
                        // 좌측 부서열 공간(정확히 RowHeaderWidth, CellHeight)
                        Spacer(
                            modifier = Modifier
                                .width(RowHeaderWidth)
                                .height(CellHeight)
                        )

                        matrix[r].forEachIndexed { c, value ->
                            StatusCell(value) {
                                matrix[r][c] = (value + 1) % 3
                                syncAllToVm()
                            }
                        }
                    }
                }

                // 하단 "센터 추가" 공간(정확히 CellHeight)
                Spacer(modifier = Modifier.height(CellHeight))
            }

            /*
             * 2) 상단 고정 헤더행(수평만 스크롤)
             */
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HeaderHeight)
                    .background(BackgroundBlack)
            ) {
                // 좌상단 코너(빈칸)
                Box(
                    modifier = Modifier
                        .width(RowHeaderWidth)
                        .height(HeaderHeight)
                )

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

            /*
             * 3) 좌측 고정 부서열(수직만 스크롤)
             */
            Column(
                modifier = Modifier
                    .width(RowHeaderWidth)
                    .fillMaxHeight()
                    .background(BackgroundBlack)
            ) {
                // 헤더행 공간
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

            // 4) 좌상단 코너 덮기(깔끔)
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
