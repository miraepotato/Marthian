@file:OptIn(com.naver.maps.map.compose.ExperimentalNaverMapApi::class)

package com.example.marthianclean.ui.situation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

    // ✅ Incident가 들어오면 카메라 이동 + 마커 위치 세팅
    LaunchedEffect(incident) {
        incident?.let { inc ->
            val pos = LatLng(inc.latitude, inc.longitude)
            markerState.position = pos
            cameraPositionState.move(CameraUpdate.scrollTo(pos))
            cameraPositionState.move(CameraUpdate.zoomTo(16.0))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        NaverMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            if (incident != null) {
                Marker(
                    state = markerState,
                    captionText = "현장"
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
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
