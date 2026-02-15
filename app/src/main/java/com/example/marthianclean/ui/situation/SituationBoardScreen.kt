@file:OptIn(com.naver.maps.map.compose.ExperimentalNaverMapApi::class)

package com.example.marthianclean.ui.situation

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.marthianclean.viewmodel.IncidentViewModel
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.compose.*
import kotlin.math.abs

private val MarsOrange = Color(0xFFFF8C00)

@Composable
fun SituationBoardScreen(
    incidentViewModel: IncidentViewModel,
    onEdit: () -> Unit,   // ✅ 좌슬라이딩(좌→우) 시 허브로 이동
    onExit: () -> Unit
) {
    val incident by incidentViewModel.incident.collectAsState()

    val cameraPositionState = rememberCameraPositionState()
    val markerState = remember { MarkerState() }

    var mapLoaded by remember { mutableStateOf(false) }

    // ✅ 위성/일반 토글 (기본: 위성)
    var isSatellite by remember { mutableStateOf(true) }

    // ✅ 좌표
    val lat = incident?.latitude
    val lng = incident?.longitude

    LaunchedEffect(lat, lng) {
        if (lat != null && lng != null) {
            val pos = LatLng(lat, lng)
            markerState.position = pos

            cameraPositionState.animate(
                update = CameraUpdate.scrollTo(pos),
                durationMs = 700
            )
            cameraPositionState.animate(
                update = CameraUpdate.zoomTo(16.0),
                durationMs = 300
            )
        }
    }

    // ===== 좌슬라이딩(좌→우) “엣지 전용 레이어” =====
    val density = LocalDensity.current
    val triggerPx = with(density) { 130.dp.toPx() } // ✅ 오발 방지용 충분한 거리
    val edgeWidth = 20.dp                          // ✅ 여기만 제스처 전용 영역(지도 이벤트보다 우선)

    var dragAccumX by remember { mutableStateOf(0f) }
    var dragAccumY by remember { mutableStateOf(0f) }
    var lastPos by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // =============================
        // 1) 지도
        // =============================
        NaverMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = if (isSatellite) MapType.Satellite else MapType.Basic
            ),
            uiSettings = MapUiSettings(
                isZoomControlEnabled = true,
                isCompassEnabled = false,
                isLocationButtonEnabled = false,

                // ✅ 형님 요청: 기울기/회전 불필요 → OFF
                isRotateGesturesEnabled = false,
                isTiltGesturesEnabled = false
            ),
            onMapLoaded = { mapLoaded = true }
        ) {
            if (lat != null && lng != null) {
                Marker(
                    state = markerState,
                    captionText = "현장"
                )
            }
        }

        // =============================
        // 2) 좌슬라이딩 전용 “투명 엣지 레이어”
        //    (지도보다 위에 올라가서 이벤트를 먼저 받음)
        // =============================
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(edgeWidth)
                .align(Alignment.CenterStart)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { start ->
                            dragAccumX = 0f
                            dragAccumY = 0f
                            lastPos = start
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            // 수평 누적
                            dragAccumX += dragAmount

                            // 수직 누적(대각선/세로 드래그 오발 방지)
                            val dy = abs(change.position.y - lastPos.y)
                            dragAccumY += dy
                            lastPos = change.position

                            // ✅ 수평이 수직보다 확실히 클 때만
                            val isMostlyHorizontal = dragAccumX > dragAccumY * 2f

                            if (isMostlyHorizontal && dragAccumX >= triggerPx) {
                                // 한 번 발동하면 재발동 방지 위해 초기화
                                dragAccumX = 0f
                                dragAccumY = 0f
                                onEdit()
                            }
                        },
                        onDragEnd = {
                            dragAccumX = 0f
                            dragAccumY = 0f
                        },
                        onDragCancel = {
                            dragAccumX = 0f
                            dragAccumY = 0f
                        }
                    )
                }
        )

        // =============================
        // 3) 로딩 오버레이
        // =============================
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

        // =============================
        // 4) 상단 우측: 위성 토글 + EXIT
        // =============================
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
