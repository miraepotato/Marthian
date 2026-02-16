package com.example.marthianclean.ui.field

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.marthianclean.model.IncidentMeta

private val BgBlack = Color(0xFF0E0E0E)
private val CardDark = Color(0xFF151515)
private val BorderGray = Color(0xFF2E2E2E)
private val TextPrimary = Color(0xFFF0F0F0)
private val AccentOrange = Color(0xFFFF9800)

private val NeonRed = Color(0xFFFF1744)
private val NeonOrange = Color(0xFFFF9100)

private data class FireTypeOption(val label: String, val value: String)

private val FireTypeOptions = listOf(
    FireTypeOption("공장", "FACTORY"),
    FireTypeOption("창고", "WAREHOUSE"),
    FireTypeOption("단독주택", "SINGLE_HOUSE"),
    FireTypeOption("공동주택", "APARTMENT"),
    FireTypeOption("상업시설", "COMMERCIAL"),
    FireTypeOption("위험물제조소", "HAZMAT_PLANT"),
    FireTypeOption("산림", "FOREST"),
    FireTypeOption("돈사", "PIG_FARM"),
    FireTypeOption("우사", "COW_FARM"),
    FireTypeOption("계사", "CHICKEN_FARM"),
    FireTypeOption("비닐하우스", "GREENHOUSE"),
    FireTypeOption("기타", "OTHER"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentInfoEditScreen(
    initialMeta: IncidentMeta,
    onBack: () -> Unit,
    onSave: (IncidentMeta) -> Unit,
) {
    var meta by remember(initialMeta) { mutableStateOf(initialMeta) }

    var fireTypeExpanded by remember { mutableStateOf(false) }
    val selectedOption = remember(meta.fireType) {
        FireTypeOptions.firstOrNull { it.value == meta.fireType } ?: FireTypeOptions.last()
    }

    val scroll = rememberScrollState()

    Scaffold(
        containerColor = BgBlack,
        topBar = {
            TopAppBar(
                title = { Text("현장 정보 수정", color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgBlack),
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("나가기", color = TextPrimary) }
                },
                actions = {
                    Button(
                        onClick = { onSave(meta) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentOrange,
                            contentColor = Color.Black
                        )
                    ) { Text("저장") }
                    Spacer(Modifier.width(10.dp))
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scroll)
                .padding(14.dp)
        ) {
            MatrixCard {
                MatrixTextRow(
                    label = "신고접수",
                    value = meta.신고접수,
                    onChange = { meta = meta.copy(신고접수 = it) }
                )
                MatrixTextRow(
                    label = "재난발생위치",
                    value = meta.재난발생위치,
                    onChange = { meta = meta.copy(재난발생위치 = it) }
                )

                MatrixRow(label = "처종") {
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { fireTypeExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Text(selectedOption.label, color = TextPrimary)
                            Spacer(Modifier.weight(1f))
                            Text("▼", color = TextPrimary)
                        }

                        DropdownMenu(
                            expanded = fireTypeExpanded,
                            onDismissRequest = { fireTypeExpanded = false }
                        ) {
                            FireTypeOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt.label) },
                                    onClick = {
                                        meta = meta.copy(fireType = opt.value)
                                        fireTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 화재 원인
                MatrixTextRow(
                    label = "화재 원인",
                    value = meta.화재원인,
                    onChange = { meta = meta.copy(화재원인 = it) },
                    singleLine = false
                )

                // ✅ 추가: 초진/완진
                MatrixTextRow(
                    label = "초진시간",
                    value = meta.초진시간,
                    onChange = { meta = meta.copy(초진시간 = it) }
                )
                MatrixTextRow(
                    label = "완진시간",
                    value = meta.완진시간,
                    onChange = { meta = meta.copy(완진시간 = it) }
                )

                DividerRow()

                MatrixTextRow(
                    label = "기상-날씨",
                    value = meta.기상_날씨,
                    onChange = { meta = meta.copy(기상_날씨 = it) }
                )
                MatrixTextRow(
                    label = "기상-기온",
                    value = meta.기상_기온,
                    onChange = { meta = meta.copy(기상_기온 = it) }
                )
                MatrixTextRow(
                    label = "기상-풍향풍속",
                    value = meta.기상_풍향풍속,
                    onChange = { meta = meta.copy(기상_풍향풍속 = it) }
                )

                DividerRow()

                MatrixTextRow(
                    label = "선착대 도착시간",
                    value = meta.선착대도착시간,
                    onChange = { meta = meta.copy(선착대도착시간 = it) }
                )

                MatrixTextRow(
                    label = "인명피해현황",
                    labelColor = NeonRed,
                    value = meta.인명피해현황,
                    onChange = { meta = meta.copy(인명피해현황 = it) },
                    singleLine = false,
                    valueColor = NeonRed
                )
                MatrixTextRow(
                    label = "재산피해현황",
                    value = meta.재산피해현황,
                    onChange = { meta = meta.copy(재산피해현황 = it) },
                    singleLine = false
                )
                MatrixTextRow(
                    label = "대원피해현황",
                    labelColor = NeonOrange,
                    value = meta.대원피해현황,
                    onChange = { meta = meta.copy(대원피해현황 = it) },
                    singleLine = false,
                    valueColor = NeonOrange
                )

                DividerRow()

                Text(
                    "유관기관 현황",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                MatrixTextRow(
                    label = "경찰",
                    value = meta.유관기관_경찰,
                    onChange = { meta = meta.copy(유관기관_경찰 = it) }
                )
                MatrixTextRow(
                    label = "시청",
                    value = meta.유관기관_시청,
                    onChange = { meta = meta.copy(유관기관_시청 = it) }
                )
                MatrixTextRow(
                    label = "한전",
                    value = meta.유관기관_한전,
                    onChange = { meta = meta.copy(유관기관_한전 = it) }
                )
                MatrixTextRow(
                    label = "도시가스",
                    value = meta.유관기관_도시가스,
                    onChange = { meta = meta.copy(유관기관_도시가스 = it) }
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun MatrixCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray, MaterialTheme.shapes.medium)
            .background(CardDark)
            .padding(12.dp),
        content = content
    )
}

@Composable
private fun DividerRow() {
    Spacer(Modifier.height(10.dp))
    HorizontalDivider(color = BorderGray)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun MatrixRow(
    label: String,
    labelWidth: Dp = 120.dp,
    labelColor: Color = TextPrimary,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = labelColor, modifier = Modifier.width(labelWidth))
        Box(Modifier.weight(1f)) { trailing() }
    }
}

@Composable
private fun MatrixTextRow(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    singleLine: Boolean = true,
    labelColor: Color = TextPrimary,
    valueColor: Color = TextPrimary
) {
    MatrixRow(label = label, labelColor = labelColor) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = valueColor,
                unfocusedTextColor = valueColor,
                focusedBorderColor = AccentOrange,
                unfocusedBorderColor = BorderGray,
                cursorColor = AccentOrange
            )
        )
    }
}
