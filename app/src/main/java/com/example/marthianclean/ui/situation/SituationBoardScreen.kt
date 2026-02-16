@file:OptIn(com.naver.maps.map.compose.ExperimentalNaverMapApi::class)

package com.example.marthianclean.ui.situation

import android.content.Context
import android.graphics.PointF
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.marthianclean.ui.sticker.VehicleIconMapper
import com.example.marthianclean.viewmodel.IncidentViewModel
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.compose.*
import com.naver.maps.map.overlay.OverlayImage
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private val MarsOrange = Color(0xFFFF8C00)
private val BorderGray = Color(0xFF2E2E2E)
private val TextPrimary = Color(0xFFF0F0F0)

private data class DragStickerState(
    val active: Boolean = false,
    val item: IncidentViewModel.StickerItem? = null,
    val windowPos: Offset = Offset.Zero
)

private fun strongVibrate(context: Context) {
    val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    vibrator ?: return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // 35~45ms 정도가 “확실한 진입감” 좋음
        vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(40)
    }
}

@Composable
fun SituationBoardScreen(
    incidentViewModel: IncidentViewModel,
    onEdit: () -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current

    val incident by incidentViewModel.incident.collectAsState()

    val cameraPositionState = rememberCameraPositionState()
    val markerState = remember { MarkerState() }
    var mapLoaded by remember { mutableStateOf(false) }

    var isSatellite by remember { mutableStateOf(true) }

    val lat = incident?.latitude
    val lng = incident?.longitude

    var naverMapObj by remember { mutableStateOf<com.naver.maps.map.NaverMap?>(null) }
    var mapRectInWindow by remember { mutableStateOf<Rect?>(null) }

    var dragState by remember { mutableStateOf(DragStickerState()) }

    // ===== 좌슬라이딩 엣지: 더 쉽게 =====
    val triggerPx = with(density) { 80.dp.toPx() } // ✅ 130 -> 80
    val edgeWidth = 28.dp                         // ✅ 20 -> 28
    var dragAccumX by remember { mutableStateOf(0f) }
    var dragAccumY by remember { mutableStateOf(0f) }
    var lastPos by remember { mutableStateOf(Offset.Zero) }

    // =============================
    // 트레이 큐
    // =============================
    val stickerQueue = remember(
        incidentViewModel.dispatchMatrix,
        incidentViewModel.dispatchDepartments,
        incidentViewModel.dispatchEquipments
    ) {
        incidentViewModel.buildStickerQueue()
    }

    val placedIds = remember(incidentViewModel.placedVehicles) {
        incidentViewModel.placedVehicles.map { it.id }.toSet()
    }
    val notPlaced = remember(stickerQueue, placedIds) {
        stickerQueue.filterNot { placedIds.contains(it.id) }
    }

    val placedCount = incidentViewModel.placedVehicles.size
    val totalToPlace = stickerQueue.size
    val remainingToPlace = max(0, totalToPlace - placedCount)

    val showTray = remainingToPlace > 0

    // ✅ 트레이가 “처음” 등장하면 줌인 1회
    var didZoomForTray by remember { mutableStateOf(false) }
    LaunchedEffect(showTray, mapLoaded, lat, lng) {
        if (!didZoomForTray && showTray && mapLoaded && lat != null && lng != null) {
            didZoomForTray = true
            cameraPositionState.animate(
                update = CameraUpdate.zoomTo(18.0), // ✅ 16 -> 18 (체감 2배 느낌)
                durationMs = 450
            )
        }
    }

    val iconSize: Dp = 42.dp

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

    fun dropCurrentStickerIfPossible() {
        val mapRect = mapRectInWindow ?: return
        val mapObj = naverMapObj ?: return
        val item = dragState.item ?: return

        val dropPos = dragState.windowPos
        val insideMap =
            dropPos.x in mapRect.left..mapRect.right &&
                    dropPos.y in mapRect.top..mapRect.bottom
        if (!insideMap) return

        val localX = (dropPos.x - mapRect.left).toFloat()
        val localY = (dropPos.y - mapRect.top).toFloat()

        val latLng = mapObj.projection.fromScreenLocation(PointF(localX, localY))

        incidentViewModel.placeVehicle(
            id = item.id,
            department = item.department,
            equipment = item.equipment,
            latLng = latLng
        )

        // ✅ 드롭 순간: 확인감
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // =============================
        // 1) 지도
        // =============================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    mapRectInWindow = coords.boundsInWindow()
                }
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
                    isLocationButtonEnabled = false,
                    isRotateGesturesEnabled = false,
                    isTiltGesturesEnabled = false
                ),
                onMapLoaded = { mapLoaded = true }
            ) {
                MapEffect(Unit) { map ->
                    naverMapObj = map
                }

                // 현장 마커
                if (lat != null && lng != null) {
                    Marker(
                        state = markerState,
                        captionText = "현장"
                    )
                }

                // ✅ 배치된 차량: 아이콘 + "(부서)" 캡션
                incidentViewModel.placedVehicles.forEach { pv ->
                    val iconRes = VehicleIconMapper.iconResForEquip(pv.equipment)
                    val label = VehicleIconMapper.deptLabel(pv.department)

                    if (iconRes != 0) {
                        Marker(
                            state = rememberMarkerState(position = pv.position),
                            icon = OverlayImage.fromResource(iconRes),
                            width = iconSize,
                            height = iconSize,
                            captionText = label,
                            captionColor = Color.White,
                            captionHaloColor = Color.Black
                        )
                    } else {
                        Marker(
                            state = rememberMarkerState(position = pv.position),
                            captionText = label,
                            captionColor = Color.White,
                            captionHaloColor = Color.Black
                        )
                    }
                }
            }
        }

        // =============================
        // 2) 좌슬라이딩 엣지 레이어 (더 쉽게)
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
                            dragAccumX += dragAmount
                            val dy = abs(change.position.y - lastPos.y)
                            dragAccumY += dy
                            lastPos = change.position

                            // ✅ 판정 완화: 2.0 -> 1.3
                            val isMostlyHorizontal = dragAccumX > dragAccumY * 1.3f
                            if (isMostlyHorizontal && dragAccumX >= triggerPx) {
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
        // 4) 상단 우측 버튼
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
            ) { Text(if (isSatellite) "SAT" else "BASIC") }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = {
                    incidentViewModel.clearIncident()
                    incidentViewModel.clearPlacedVehicles()
                    onExit()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1C1C1C),
                    contentColor = MarsOrange
                )
            ) { Text("EXIT") }
        }

        // =============================
        // 5) 하단 트레이: ✅ 아이콘 + (부서)만
        // =============================
        if (showTray) {
            val scroll = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "배치 남음: $remainingToPlace", color = Color.White)
                    Spacer(Modifier.width(10.dp))
                    Text(text = "(${placedCount}/${totalToPlace})", color = MarsOrange)
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scroll),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    notPlaced.forEach { item: IncidentViewModel.StickerItem ->
                        DraggableTraySticker(
                            item = item,
                            deptLabel = VehicleIconMapper.deptLabel(item.department),
                            iconRes = VehicleIconMapper.iconResForEquip(item.equipment),
                            onDragStart = { windowPos: Offset ->
                                // ✅ “진입” 확실하게: 진동 + 하프틱 2연타
                                strongVibrate(context)
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                view.performHapticFeedback(HapticFeedbackConstants.DRAG_START)

                                dragState = DragStickerState(
                                    active = true,
                                    item = item,
                                    windowPos = windowPos
                                )
                            },
                            onDragMove = { windowPos: Offset ->
                                dragState = dragState.copy(windowPos = windowPos)
                            },
                            onDragEnd = {
                                dropCurrentStickerIfPossible()
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                dragState = DragStickerState(active = false)
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }
        }

        // =============================
        // 6) 드래그 중 공중 프리뷰: ✅ 아이콘 + (부서)
        // =============================
        if (dragState.active && dragState.item != null) {
            val item = dragState.item!!
            val iconRes = VehicleIconMapper.iconResForEquip(item.equipment)
            Box(
                modifier = Modifier.offset {
                    IntOffset(
                        dragState.windowPos.x.roundToInt() - 70,
                        dragState.windowPos.y.roundToInt() - 28
                    )
                }
            ) {
                TrayChip(
                    iconRes = iconRes,
                    text = VehicleIconMapper.deptLabel(item.department)
                )
            }
        }
    }
}

@Composable
private fun DraggableTraySticker(
    item: IncidentViewModel.StickerItem,
    deptLabel: String,
    iconRes: Int,
    onDragStart: (windowPos: Offset) -> Unit,
    onDragMove: (windowPos: Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var chipTopLeftInWindow by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                val rect = coords.boundsInWindow()
                chipTopLeftInWindow = Offset(rect.left, rect.top)
            }
            .pointerInput(item.id) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        onDragStart(chipTopLeftInWindow + startOffset)
                    },
                    onDrag = { change, _ ->
                        onDragMove(chipTopLeftInWindow + change.position)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            }
    ) {
        TrayChip(iconRes = iconRes, text = deptLabel)
    }
}

@Composable
private fun TrayChip(
    iconRes: Int,
    text: String
) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .wrapContentWidth()
            .background(Color(0xFF0E0E0E), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconRes != 0) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
        }

        Text(
            text = text,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}
