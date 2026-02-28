package com.example.marthianclean.ui.field

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.marthianclean.model.IncidentMeta
import java.text.SimpleDateFormat
import java.util.*

private val BgBlack = Color(0xFF0E0E0E)
private val CardDark = Color(0xFF151515)
private val BorderGray = Color(0xFF2E2E2E)
private val TextPrimary = Color(0xFFF0F0F0)
private val AccentOrange = Color(0xFFFF9800)
private val NeonRed = Color(0xFFFF1744)
private val NeonOrange = Color(0xFFFF9100)

private val ResponseStages = listOf("해당 사항 없음", "1단계", "2단계", "3단계")
private val WeatherOptions = listOf("맑음", "비", "눈", "흐림")
private val WindDirections = listOf("동", "서", "남", "북", "동남", "동북", "서북", "서남", "북서", "북동", "남서", "남동")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentInfoEditScreen(
    initialMeta: IncidentMeta,
    onBack: () -> Unit,
    onSave: (IncidentMeta) -> Unit,
) {
    var meta by remember(initialMeta) {
        mutableStateOf(initialMeta.copy(
            신고접수일시 = if (initialMeta.신고접수일시.isBlank())
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).format(Date())
            else initialMeta.신고접수일시
        ))
    }

    var stageExpanded by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var targetField by remember { mutableStateOf("") }

    val scroll = rememberScrollState()

    Scaffold(
        containerColor = BgBlack,
        topBar = {
            TopAppBar(
                title = { Text("재난 정보 입력", color = TextPrimary, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgBlack),
                navigationIcon = { TextButton(onClick = onBack) { Text("나가기", color = TextPrimary) } },
                actions = {
                    Button(
                        onClick = { onSave(meta) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange, contentColor = Color.Black)
                    ) { Text("저장", fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(10.dp))
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(scroll).padding(14.dp)) {
            MatrixCard {
                TimeSelectRow(label = "신고접수 일시", value = meta.신고접수일시) {
                    targetField = "신고"; showTimePicker = true
                }
                MatrixTextRow(label = "재난발생위치", value = meta.재난발생위치, onChange = { meta = meta.copy(재난발생위치 = it) })

                MatrixRow(label = "대응단계") {
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { stageExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(meta.대응단계.ifBlank { "해당 사항 없음" }, color = TextPrimary)
                            Spacer(Modifier.weight(1f)); Text("▼", color = TextPrimary)
                        }
                        ThemeedDropdownMenu(expanded = stageExpanded, onDismissRequest = { stageExpanded = false }) {
                            ResponseStages.forEach { stage ->
                                DropdownMenuItem(text = { Text(stage, color = TextPrimary) }, onClick = { meta = meta.copy(대응단계 = stage); stageExpanded = false })
                            }
                        }
                    }
                }

                MatrixTextRow(label = "화재 원인", value = meta.화재원인, onChange = { meta = meta.copy(화재원인 = it) }, singleLine = false)

                TimeSelectRow(label = "초진시간", value = meta.초진시간) { targetField = "초진"; showTimePicker = true }
                TimeSelectRow(label = "완진시간", value = meta.완진시간) { targetField = "완진"; showTimePicker = true }

                DividerRow()
                WeatherSection(meta = meta, onMetaChange = { meta = it })
                DividerRow()

                MatrixTextRow(label = "선착대 도착시간", value = meta.선착대도착시간, onChange = { meta = meta.copy(선착대도착시간 = it) })
                MatrixTextRow(label = "인명피해", labelColor = NeonRed, valueColor = NeonRed, value = meta.인명피해현황, onChange = { meta = meta.copy(인명피해현황 = it) })
                MatrixTextRow(label = "재산피해", labelColor = NeonRed, valueColor = NeonRed, value = meta.재산피해현황, onChange = { meta = meta.copy(재산피해현황 = it) })
                MatrixTextRow(label = "대원피해", labelColor = NeonOrange, valueColor = NeonOrange, value = meta.대원피해현황, onChange = { meta = meta.copy(대원피해현황 = it) })

                DividerRow()
                // ✅ 소방력 인원 입력 (숫자 키보드 띄우기)
                MatrixTextRow(
                    label = "소방력 인원(명)",
                    value = meta.소방력_인원,
                    onChange = { meta = meta.copy(소방력_인원 = it) },
                    keyboardType = KeyboardType.Number
                )
                DividerRow()

                Text("유관기관 현황", color = TextPrimary, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 6.dp))
                MatrixTextRow(label = "경찰", value = meta.유관기관_경찰, onChange = { meta = meta.copy(유관기관_경찰 = it) })
                MatrixTextRow(label = "시청", value = meta.유관기관_시청, onChange = { meta = meta.copy(유관기관_시청 = it) })
                MatrixTextRow(label = "한전", value = meta.유관기관_한전, onChange = { meta = meta.copy(유관기관_한전 = it) })
                MatrixTextRow(label = "도시가스", value = meta.유관기관_도시가스, onChange = { meta = meta.copy(유관기관_도시가스 = it) })
                MatrixTextRow(label = "산불진화대(화성시)", value = meta.유관기관_산불진화대_화성시, onChange = { meta = meta.copy(유관기관_산불진화대_화성시 = it) })
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    if (showTimePicker) {
        WheelDateTimePickerDialog(
            initialValue = when(targetField) {
                "신고" -> meta.신고접수일시
                "초진" -> meta.초진시간
                "완진" -> meta.완진시간
                else -> ""
            },
            onDismiss = { showTimePicker = false },
            onConfirm = { pickedTime ->
                meta = when(targetField) {
                    "신고" -> meta.copy(신고접수일시 = pickedTime)
                    "초진" -> meta.copy(초진시간 = pickedTime)
                    "완진" -> meta.copy(완진시간 = pickedTime)
                    else -> meta
                }
                showTimePicker = false
            }
        )
    }
}

@Composable
private fun WeatherSection(meta: IncidentMeta, onMetaChange: (IncidentMeta) -> Unit) {
    var weatherExpanded by remember { mutableStateOf(false) }
    var showTempPicker by remember { mutableStateOf(false) }
    var showWindPicker by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        Text("기상 정보 (날씨/기온/바람)", color = AccentOrange, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) {
                OutlinedButton(onClick = { weatherExpanded = true }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("날씨: ${meta.기상_날씨}", color = TextPrimary)
                }
                ThemeedDropdownMenu(expanded = weatherExpanded, onDismissRequest = { weatherExpanded = false }) {
                    WeatherOptions.forEach { w ->
                        DropdownMenuItem(text = { Text(w, color = TextPrimary) }, onClick = { onMetaChange(meta.copy(기상_날씨 = w)); weatherExpanded = false })
                    }
                }
            }
            OutlinedButton(onClick = { showTempPicker = true }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text("기온: ${meta.기상_기온}", color = TextPrimary)
            }
            OutlinedButton(onClick = { showWindPicker = true }, modifier = Modifier.weight(1.3f), contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text("바람: ${meta.기상_풍향} ${meta.기상_풍속}", color = TextPrimary)
            }
        }
    }
    if (showTempPicker) {
        TempPickerDialog(initialValue = meta.기상_기온, onDismiss = { showTempPicker = false }, onConfirm = { pickedTemp -> onMetaChange(meta.copy(기상_기온 = pickedTemp)); showTempPicker = false })
    }
    if (showWindPicker) {
        WindPickerDialog(initialDir = meta.기상_풍향, initialSpeed = meta.기상_풍속, onDismiss = { showWindPicker = false }, onConfirm = { dir, speed -> onMetaChange(meta.copy(기상_풍향 = dir, 기상_풍속 = speed)); showWindPicker = false })
    }
}

@Composable
fun ThemeedDropdownMenu(expanded: Boolean, onDismissRequest: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest, modifier = Modifier.background(BgBlack, RoundedCornerShape(8.dp)).border(1.dp, AccentOrange, RoundedCornerShape(8.dp)), content = content)
}

@Composable
fun TempPickerDialog(initialValue: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val tempRange = (-40..40).map { "${it}°C" }
    var selected by remember { mutableStateOf(initialValue.ifBlank { "0°C" }) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = BgBlack, border = BorderStroke(1.dp, AccentOrange), modifier = Modifier.padding(10.dp)) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("기온 선택", color = AccentOrange, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(20.dp))
                SnappingWheelPicker(items = tempRange, initialItem = selected, onItemSelected = { selected = it })
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("취소", color = Color.Gray) }
                    Button(onClick = { onConfirm(selected) }, colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)) { Text("확인", color = Color.Black) }
                }
            }
        }
    }
}

@Composable
fun WindPickerDialog(initialDir: String, initialSpeed: String, onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    val speedRange = (0..40).map { "${it}m/s" }
    var dir by remember { mutableStateOf(initialDir.ifBlank { "북" }) }
    var speed by remember { mutableStateOf(initialSpeed.ifBlank { "0m/s" }) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = BgBlack, border = BorderStroke(1.dp, AccentOrange), modifier = Modifier.padding(10.dp)) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("풍향 및 풍속 선택", color = AccentOrange, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    SnappingWheelPicker(items = WindDirections, initialItem = dir, onItemSelected = { dir = it }, width = 90.dp)
                    SnappingWheelPicker(items = speedRange, initialItem = speed, onItemSelected = { speed = it }, width = 90.dp)
                }
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("취소", color = Color.Gray) }
                    Button(onClick = { onConfirm(dir, speed) }, colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)) { Text("확인", color = Color.Black) }
                }
            }
        }
    }
}

@Composable
fun WheelDateTimePickerDialog(initialValue: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val calendar = Calendar.getInstance().apply {
        try { val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).parse(initialValue); if (date != null) time = date } catch (e: Exception) { }
    }
    val years = (2024..2030).map { it.toString() }
    val months = (1..12).map { String.format("%02d", it) }
    val days = (1..31).map { String.format("%02d", it) }
    val hours = (0..23).map { String.format("%02d", it) }
    val minutes = (0..59).map { String.format("%02d", it) }

    var y by remember { mutableStateOf(calendar.get(Calendar.YEAR).toString()) }
    var m by remember { mutableStateOf(String.format("%02d", calendar.get(Calendar.MONTH) + 1)) }
    var d by remember { mutableStateOf(String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))) }
    var h by remember { mutableStateOf(String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY))) }
    var min by remember { mutableStateOf(String.format("%02d", calendar.get(Calendar.MINUTE))) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = BgBlack, border = BorderStroke(1.dp, AccentOrange), modifier = Modifier.padding(10.dp)) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("시간 선택", color = AccentOrange, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    SnappingWheelPicker(items = years, initialItem = y, onItemSelected = { y = it }, width = 60.dp)
                    Text("-", color = Color.Gray)
                    SnappingWheelPicker(items = months, initialItem = m, onItemSelected = { m = it }, width = 50.dp)
                    Text("-", color = Color.Gray)
                    SnappingWheelPicker(items = days, initialItem = d, onItemSelected = { d = it }, width = 50.dp)
                    Spacer(Modifier.width(10.dp))
                    SnappingWheelPicker(items = hours, initialItem = h, onItemSelected = { h = it }, width = 50.dp)
                    Text(":", color = Color.Gray)
                    SnappingWheelPicker(items = minutes, initialItem = min, onItemSelected = { min = it }, width = 50.dp)
                }
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("취소", color = Color.Gray) }
                    Button(onClick = { onConfirm("$y-$m-$d $h:$min") }, colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)) { Text("확인", color = Color.Black) }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SnappingWheelPicker(items: List<String>, initialItem: String, onItemSelected: (String) -> Unit, modifier: Modifier = Modifier, width: Dp = 80.dp) {
    val initialIndex = items.indexOf(initialItem).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val index = listState.firstVisibleItemIndex
            if (index in items.indices) { onItemSelected(items[index]) }
        }
    }
    Box(modifier = modifier.height(150.dp).width(width), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().height(50.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)))
        LazyColumn(state = listState, flingBehavior = flingBehavior, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 50.dp)) {
            items(items.size) { index ->
                val isSelected = listState.firstVisibleItemIndex == index
                Box(modifier = Modifier.height(50.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(text = items[index], color = if (isSelected) AccentOrange else Color.DarkGray, fontSize = if (isSelected) 22.sp else 16.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun TimeSelectRow(label: String, value: String, onClick: () -> Unit) {
    MatrixRow(label = label) {
        Box(modifier = Modifier.fillMaxWidth().border(1.dp, BorderGray, RoundedCornerShape(4.dp)).clickable { onClick() }.padding(16.dp)) {
            Text(text = value.ifBlank { "시간 선택" }, color = if(value.isBlank()) Color.Gray else TextPrimary, fontSize = 16.sp)
        }
    }
}

@Composable
private fun MatrixCard(content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().border(1.dp, BorderGray, MaterialTheme.shapes.medium).background(CardDark).padding(12.dp), content = content)
}
@Composable
private fun DividerRow() { Spacer(Modifier.height(10.dp)); HorizontalDivider(color = BorderGray); Spacer(Modifier.height(10.dp)) }
@Composable
private fun MatrixRow(label: String, labelWidth: Dp = 120.dp, labelColor: Color = TextPrimary, trailing: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, color = labelColor, modifier = Modifier.width(labelWidth), fontSize = 16.sp)
        Box(Modifier.weight(1f)) { trailing() }
    }
}

// ✅ 키보드 타입 지정을 위한 파라미터 추가
@Composable
private fun MatrixTextRow(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    singleLine: Boolean = true,
    labelColor: Color = TextPrimary,
    valueColor: Color = TextPrimary,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    MatrixRow(label = label, labelColor = labelColor) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = valueColor, unfocusedTextColor = valueColor, focusedBorderColor = AccentOrange, unfocusedBorderColor = BorderGray, cursorColor = AccentOrange)
        )
    }
}