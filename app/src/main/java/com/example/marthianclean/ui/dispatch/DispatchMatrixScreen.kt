package com.example.marthianclean.ui.dispatch

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi   // ✅ 추가
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

@OptIn(ExperimentalFoundationApi::class)   // ✅ 이 한 줄이 핵심
@Composable
fun DispatchMatrixScreen() {

    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    val hScroll = rememberScrollState()
    val vScroll = rememberScrollState()

    val departments = remember {
        mutableStateListOf(
            "향남센터", "양감지역대", "남양센터", "서신지역대", "제부지역대",
            "팔탄센터", "장안센터", "새솔센터", "화성구조대",
            "정남센터", "반송센터", "태안센터", "마도지역대",
            "매송지역대", "목동센터",
            "안중센터(평택)", "포승센터(평택)", "서탄센터(송탄)",
            "세교센터(오산)", "청학센터(오산)", "반월센터(안산)"
        )
    }

    val equipments = remember {
        mutableStateListOf(
            "펌프차", "탱크차", "화학차", "고가사다리차",
            "굴절차", "무인방수파괴", "구조공작차",
            "장비운반차", "구급차"
        )
    }

    val matrix = remember {
        mutableStateListOf<SnapshotStateList<Int>>().apply {
            repeat(departments.size) {
                add(
                    mutableStateListOf<Int>().apply {
                        repeat(equipments.size) { add(0) }
                    }
                )
            }
        }
    }

    var editingDept by remember { mutableStateOf<Int?>(null) }
    var editingEquip by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(vScroll)
                .horizontalScroll(hScroll)
                .padding(8.dp)
        ) {

            /* ===== HEADER ROW ===== */
            Row {
                Box(
                    modifier = Modifier
                        .width(RowHeaderWidth)
                        .height(HeaderHeight)
                )

                equipments.forEachIndexed { i, name ->
                    EditableHeaderCell(
                        width = ColumnWidth,
                        text = name,
                        editing = editingEquip == i,
                        onLongPress = {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            editingEquip = i
                        },
                        onDone = {
                            editingEquip = null
                            focusManager.clearFocus()
                        },
                        onTextChange = { equipments[i] = it }
                    )
                }

                AddHeaderCell("차량 추가", ColumnWidth) {
                    equipments.add("신규차량")
                    matrix.forEach { it.add(0) }
                    editingEquip = equipments.lastIndex
                }
            }

            /* ===== DATA ROWS ===== */
            departments.forEachIndexed { r, dept ->
                Row {
                    EditableHeaderCell(
                        width = RowHeaderWidth,
                        text = dept,
                        editing = editingDept == r,
                        onLongPress = {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            editingDept = r
                        },
                        onDone = {
                            editingDept = null
                            focusManager.clearFocus()
                        },
                        onTextChange = { departments[r] = it }
                    )

                    matrix[r].forEachIndexed { c, value ->
                        StatusCell(value) {
                            matrix[r][c] = (value + 1) % 3
                        }
                    }
                }
            }

            /* ===== ADD ROW ===== */
            AddHeaderCell("센터 추가", RowHeaderWidth) {
                departments.add("신규센터")
                matrix.add(
                    mutableStateListOf<Int>().apply {
                        repeat(equipments.size) { add(0) }
                    }
                )
                editingDept = departments.lastIndex
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditableHeaderCell(
    width: Dp,
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
            .height(HeaderHeight)
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
fun AddHeaderCell(text: String, width: Dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(width)
            .height(HeaderHeight)
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
