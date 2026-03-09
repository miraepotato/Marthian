package com.example.marthianclean.ui.field

import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun MapPreviewScreen(
    addressText: String,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current

    // 🔸 기본 위치: 화성소방서(향남읍) fallback
    val defaultLatLng = remember { LatLng(37.13106, 126.92053) } // :contentReference[oaicite:3]{index=3}

    var targetLatLng by remember { mutableStateOf<LatLng?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var resolvedLabel by remember { mutableStateOf("") }

    val cameraPositionState: CameraPositionState = rememberCameraPositionState()

    // ✅ 스텝2: 주소 → 좌표 (최소 구현: Android Geocoder)
    LaunchedEffect(addressText) {
        isLoading = true
        resolvedLabel = ""

        val result = withContext(Dispatchers.IO) {
            runCatching {
                val geocoder = Geocoder(context, Locale.KOREA)
                val list = geocoder.getFromLocationName(addressText, 1)
                val first = list?.firstOrNull()
                if (first != null) LatLng(first.latitude, first.longitude) else null
            }.getOrNull()
        }

        targetLatLng = result ?: defaultLatLng

        // 카메라 이동
        cameraPositionState.position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(
            targetLatLng!!,
            16f
        )

        resolvedLabel = if (result != null) "검색 위치" else "기본 위치(화성소방서)"
        isLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ✅ 스텝1: 실제 지도 렌더링
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            val pin = targetLatLng ?: defaultLatLng
            Marker(
                state = MarkerState(position = pin),
                title = resolvedLabel.ifBlank { "현장 위치" },
                snippet = addressText
            )
        }

        // 상단 텍스트(디버그/확인용)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp)
                .background(Color(0x88000000))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = "지도 미리보기",
                color = Color.White,
                fontSize = 16.sp
            )
            Text(
                text = addressText,
                color = Color.White,
                fontSize = 13.sp
            )
            if (resolvedLabel.isNotBlank()) {
                Text(
                    text = resolvedLabel,
                    color = Color(0xFFFF9800),
                    fontSize = 12.sp
                )
            }
        }

        // 로딩 오버레이 (스텝3에서 더 예쁘게 다듬을 예정)
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x55000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "지도/좌표 불러오는 중…",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // 🔹 하단 확정 버튼 (기존 그대로)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
                .background(Color(0xFFFF9800))
                .clickable { onConfirm() }
                .padding(horizontal = 32.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "현장 확정",
                color = Color.Black,
                fontSize = 18.sp
            )
        }
    }
}
