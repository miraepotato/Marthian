package com.example.marthianclean.ui.field

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.marthianclean.model.Incident
import com.example.marthianclean.viewmodel.IncidentViewModel
import com.example.marthianclean.viewmodel.PlaceCandidate

private val BgBlack = Color(0xFF0E0E0E)
private val CardDark = Color(0xFF151515)
private val BorderGray = Color(0xFF2E2E2E)
private val TextPrimary = Color(0xFFF0F0F0)
private val TextSecondary = Color(0xFFBDBDBD)
private val AccentOrange = Color(0xFFFF9800)

@Composable
fun AddressSearchScreen(
    onDone: (Incident) -> Unit,
    onBack: () -> Unit
) {
    val vm: IncidentViewModel = viewModel()

    var query by remember { mutableStateOf("") }

    val candidates by vm.candidates.collectAsState()
    val loading by vm.searchLoading.collectAsState()
    val error by vm.searchError.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("주소/장소 검색", color = TextPrimary, modifier = Modifier.weight(1f))

            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CardDark,
                    contentColor = TextPrimary
                )
            ) { Text("뒤로") }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("예: 향남로 399 / 화성소방서") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardDark,
                unfocusedContainerColor = CardDark,
                disabledContainerColor = CardDark,

                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,

                focusedBorderColor = AccentOrange,
                unfocusedBorderColor = BorderGray,

                focusedLabelColor = AccentOrange,
                unfocusedLabelColor = TextSecondary,

                cursorColor = AccentOrange
            )
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = { vm.searchPlaceCandidates(query) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentOrange,
                    contentColor = Color.Black
                )
            ) {
                Text(if (loading) "검색중…" else "검색")
            }

            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    vm.geocodeAndApply(
                        query = query,
                        onSuccess = {
                            val inc = vm.incident.value
                            if (inc != null) onDone(inc)
                        },
                        onFail = { /* 필요하면 에러 처리 확장 */ }
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CardDark,
                    contentColor = TextPrimary
                )
            ) {
                Text("주소로 바로")
            }
        }

        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Text("⚠️ $error", color = AccentOrange)
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = BorderGray)
        Spacer(Modifier.height(8.dp))

        // ✅ 문구만 10개로 변경
        Text("검색 결과 (최대 10개)", color = TextSecondary)
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(candidates) { item ->
                CandidateRowDark(
                    item = item,
                    onClick = {
                        vm.geocodeAndApply(
                            query = item.address,
                            onSuccess = {
                                val inc = vm.incident.value
                                if (inc != null) onDone(inc)
                            },
                            onFail = { /* 필요시 확장 */ }
                        )
                    }
                )
                HorizontalDivider(color = BorderGray)
            }
        }
    }
}

@Composable
private fun CandidateRowDark(
    item: PlaceCandidate,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(BgBlack)
            .padding(vertical = 12.dp)
    ) {
        Text(item.title, color = TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text(item.address, color = TextSecondary)
    }
}
