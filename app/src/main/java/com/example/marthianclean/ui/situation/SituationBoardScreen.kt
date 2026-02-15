@file:OptIn(com.naver.maps.map.compose.ExperimentalNaverMapApi::class)

package com.example.marthianclean.ui.situation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.marthianclean.viewmodel.IncidentViewModel
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.compose.*

private val MarsOrange = Color(0xFFFF8C00)

@Composable
fun SituationBoardScreen(
    incidentViewModel: IncidentViewModel,
    onExit: () -> Unit
) {
    val incident by incidentViewModel.incident.collectAsState()

    val cameraPositionState = rememberCameraPositionState()
    val markerState = remember { MarkerState() }

    var mapLoaded by remember { mutableStateOf(false) }

    // ✅ 위성/일반 토글 (기본: 위성)
    var isSatellite by remember { mutableStateOf(true) }

    // ✅ 핵심: 좌표 변화에 정확히 반응 (incident 객체 자체가 아니라 lat/lng)
    val lat = incident?.latitude
    val lng = incident?.longitude

    LaunchedEffect(lat, lng) {
        if (lat != null && lng != null) {
            val pos = LatLng(lat, lng)

            // 마커 위치 갱신
            markerState.position = pos

            // ✅ 카메라 이동은 animate가 더 안정적
            cameraPositionState.animate(
                update = CameraUpdate.scrollTo(pos),
                durationMs = 700
            )
            cameraPositionState.animate(
                update = CameraUpdate.zoomTo(16.0),
                durationMs = 300
            )

            android.util.Log.e("MAP_DEBUG", "CAMERA MOVED to lat=$lat lng=$lng")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        NaverMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = if (isSatellite) MapType.Satellite else MapType.Basic
            ),
            uiSettings = MapUiSettings(
                isZoomControlEnabled = true,
                isCompassEnabled = false,
                isLocationButtonEnabled = false
            ),
            onMapLoaded = { mapLoaded = true }
        ) {
            // ✅ incident 있을 때만 마커 표시
            if (lat != null && lng != null) {
                Marker(
                    state = markerState,
                    captionText = "현장"
                )
            }
        }

        // ✅ 로딩 오버레이
        if (!mapLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "지도 로딩 중…", color = Color.White)
            }
        }

        // ✅ 상단 우측: 위성 토글 + EXIT
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { isSatellite = !isSatellite },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1C1C1C),
                    contentColor = Color.White
                )
            ) {
                Text(if (isSatellite) "SAT" else "BASIC")
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = {
                    incidentViewModel.clearIncident()
                    onExit()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1C1C1C),
                    contentColor = MarsOrange
                )
            ) {
                Text("EXIT")
            }
        }
    }
}
