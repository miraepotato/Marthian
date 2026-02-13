package com.example.marthianclean.ui.field

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.marthianclean.model.Incident

private val MarsOrange = Color(0xFFFF8C00)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressSearchScreen(
    onDone: (Incident) -> Unit,
    onBack: () -> Unit
) {
    var addressText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    fun doSearch() {
        // ✅ TODO: 나중에 Google Places/Geocoding으로 위경도 교체
        val mockLat = 37.566535
        val mockLng = 126.9779692

        val incident = Incident(
            address = if (addressText.isBlank()) "서울시청(임시)" else addressText.trim(),
            latitude = mockLat,
            longitude = mockLng
        )

        // ✅ 검색 후 키보드 내리기
        focusManager.clearFocus(force = true)

        onDone(incident)
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->

        // ✅ 검색바 위 빈 공간 터치 시 키보드 자동 숨김
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(padding)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus(force = true)
                    })
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                OutlinedTextField(
                    value = addressText,
                    onValueChange = { addressText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("주소/상호를 입력하세요", color = Color.Gray) },
                    singleLine = true,

                    // ✅ 엔터(검색) 누르면 바로 검색
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { doSearch() }
                    ),

                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF121212),
                        unfocusedContainerColor = Color(0xFF121212),
                        focusedBorderColor = Color(0xFF2A2A2A),
                        unfocusedBorderColor = Color(0xFF2A2A2A),
                        cursorColor = MarsOrange
                    )
                )

                Button(
                    onClick = { doSearch() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1C1C1C),
                        contentColor = MarsOrange
                    )
                ) {
                    Text("검색")
                }
            }
        }
    }
}
