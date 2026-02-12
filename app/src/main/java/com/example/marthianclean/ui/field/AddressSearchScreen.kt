package com.example.marthianclean.ui.field

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressSearchScreen(
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    var address by remember { mutableStateOf(TextFieldValue("")) }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                ),
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(60.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // 검색창
                TextField(
                    value = address,
                    onValueChange = { address = it },
                    placeholder = {
                        Text(
                            text = "주소를 입력하세요",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(30.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1C1C1C),
                        unfocusedContainerColor = Color(0xFF1C1C1C),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color(0xFFFF8C00)
                    ),
                    modifier = Modifier
                        .weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // 주황 돋보기 버튼
                Surface(
                    color = Color(0xFFFF8C00),
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier
                        .size(56.dp)
                ) {
                    IconButton(onClick = onDone) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.Black
                        )
                    }
                }
            }
        }
    }
}
