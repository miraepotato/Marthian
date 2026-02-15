package com.example.marthianclean.ui.field

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.marthianclean.model.Incident
import com.example.marthianclean.viewmodel.IncidentViewModel

private val MarsOrange = Color(0xFFFF8C00)
private val MarsDark = Color(0xFF1C1C1C)

@Composable
fun AddressSearchScreen(
    onDone: (Incident) -> Unit,
    onBack: () -> Unit,
    incidentViewModel: IncidentViewModel = viewModel()
) {
    var query by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val keyboardController = LocalSoftwareKeyboardController.current

    fun doSearch() {
        errorMessage = null
        keyboardController?.hide()

        val q = query.trim()
        if (q.isEmpty()) {
            errorMessage = "검색어를 입력해 주세요."
            return
        }

        // ✅ A 방식: 키는 ViewModel init에서 이미 1회 세팅됨
        incidentViewModel.geocodeAndApply(
            query = q,
            onSuccess = {
                val inc = incidentViewModel.incident.value
                if (inc != null) onDone(inc) else errorMessage = "Incident 생성에 실패했습니다."
            },
            onFail = { msg -> errorMessage = msg }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(20.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "주소 검색",
                color = MarsOrange,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = query,
                onValueChange = { query = it },
                label = { Text("주소 또는 장소명 입력", color = Color.Gray) },
                singleLine = true,
                textStyle = TextStyle(color = Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { doSearch() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MarsOrange,
                    unfocusedBorderColor = Color.DarkGray,
                    cursorColor = MarsOrange,
                    focusedLabelColor = MarsOrange,
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = MarsDark,
                    unfocusedContainerColor = MarsDark
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { doSearch() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MarsDark,
                    contentColor = Color.White
                )
            ) {
                Text("검색")
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onBack() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MarsDark,
                    contentColor = MarsOrange
                )
            ) {
                Text("뒤로")
            }

            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
