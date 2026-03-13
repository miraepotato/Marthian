package com.example.marthianclean.ui.field

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.marthianclean.data.IncidentStore
import com.example.marthianclean.model.Incident
import com.example.marthianclean.viewmodel.HistoryViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val MarsOrange = Color(0xFFFF8C00)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PastIncidentsScreen(
    onBack: () -> Unit,
    onOpenIncident: (Incident) -> Unit,
    viewModel: HistoryViewModel = viewModel() // HistoryViewModel 주입
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 뷰모델 상태 구독
    val incidents by viewModel.incidents.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val selectedIds = viewModel.selectedIds
    val selectionMode = selectedIds.isNotEmpty()

    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 파일 내보내기 런처
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val selected = viewModel.getSelectedIncidents()
                IncidentStore.exportToUri(context, uri, selected)
                viewModel.clearSelection()
            }.onFailure {
                // 에러 발생 시 처리 (필요시 뷰모델에 에러 전달)
            }
        }
    }

    // 초기 데이터 로드
    LaunchedEffect(Unit) {
        viewModel.reload(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectionMode) "선택됨: ${selectedIds.size}" else "지난 현장",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectionMode) viewModel.clearSelection() else onBack()
                    }) {
                        Text("←", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    if (selectionMode) {
                        // 선택 모드일 때: 추출 및 삭제
                        TextButton(onClick = {
                            val ts = SimpleDateFormat("yyyyMMdd_HHmm", Locale.KOREA).format(Date())
                            createDocLauncher.launch("marthian_backup_$ts.json")
                        }) {
                            Text("추출", color = MarsOrange, fontWeight = FontWeight.Bold)
                        }

                        TextButton(onClick = { showDeleteConfirm = true }) {
                            Text("삭제", color = Color(0xFFFF6666), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // 일반 모드일 때: 새로고침
                        TextButton(onClick = { viewModel.reload(context) }) {
                            Text("새로고침", color = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { pad ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MarsOrange
                    )
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(errorMessage ?: "알 수 없는 에러", color = Color(0xFFFF8888))
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = { viewModel.reload(context) }) {
                            Text("다시 시도")
                        }
                    }
                }
                incidents.isEmpty() -> {
                    Text(
                        text = "저장된 지난 현장이 없습니다.",
                        color = Color(0xFFBBBBBB),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(incidents, key = { it.id }) { inc ->
                            val isSelected = selectedIds.contains(inc.id)

                            PastIncidentRow(
                                incident = inc,
                                selected = isSelected,
                                onToggleSelect = { viewModel.toggleSelection(inc.id) },
                                onOpen = {
                                    if (selectionMode) {
                                        viewModel.toggleSelection(inc.id)
                                    } else {
                                        onOpenIncident(inc)
                                    }
                                }
                            )
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }

    // 삭제 확인 다이얼로그
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("삭제하시겠습니까?") },
            text = { Text("선택된 ${selectedIds.size}개 현장을 삭제합니다.\n(복구 불가)") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteSelected(context)
                }) {
                    Text("삭제", color = Color(0xFFFF6666), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("취소")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PastIncidentRow(
    incident: Incident,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    onOpen: () -> Unit
) {
    val bg = if (selected) Color(0xFF2A2A2A) else Color(0xFF141414)
    val border = if (selected) MarsOrange else Color(0xFF2B2B2B)

    // 날짜 텍스트 결정
    val dt = incident.meta.신고접수일시.ifBlank { incident.startTime }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = { onOpen() },
                onLongClick = { onToggleSelect() }
            )
            .padding(14.dp)
    ) {
        Text(
            text = incident.address.takeIf { it.isNotBlank() } ?: "(주소 없음)",
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Text(text = dt, color = Color(0xFFB0B0B0))
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth()
                .background(border)
        )
    }
}