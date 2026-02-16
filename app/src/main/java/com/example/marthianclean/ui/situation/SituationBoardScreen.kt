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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.consumeAllChanges
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
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val MarsOrange = Color(0xFFFF8C00)
private val TextPrimary = Color(0xFFF0F0F0)

private data class DragPayload(
    val id: String,
    val department: String,
    val equipment: String
)

private data class DragState(
    val active: Boolean = false,
    val payload: DragPayload? = null,
    val windowPos: Offset = Offset.Zero,
    val wobble: Boolean = false
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

    var dragState by remember { mutableStateOf(DragState()) }

    // ===== 좌슬라이딩 엣지 =====
    val triggerPx = with(density) { 80.dp.toPx() }
    val edgeWidth = 28.dp
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

    // ✅ 심벌 크기 고정
    val iconSize: Dp = 42.dp

    // ✅ 초기 카메라: 트레이 있으면 줌18, 아니면 줌16
    var didInitialCam by remember { mutableStateOf(false) }
    LaunchedEffect(lat, lng, mapLoaded, showTray) {
        if (!didInitialCam && mapLoaded && lat != null && lng != null) {
            didInitialCam = true
            val pos = LatLng(lat, lng)
            markerState.position = pos

            cameraPositionState.animate(
                update = CameraUpdate.scrollTo(pos),
                durationMs = 700
            )
            cameraPositionState.animate(
                update = CameraUpdate.zoomTo(if (showTray) 18.0 else 16.0),
                durationMs = 320
            )
        }
    }

    // ✅ 트레이 등장 시 마지막으로 줌18 1회
    var didZoomForTray by remember { mutableStateOf(false) }
    LaunchedEffect(showTray) {
        if (!showTray) didZoomForTray = false
    }
    LaunchedEffect(showTray, mapLoaded, didInitialCam) {
        if (!didZoomForTray && showTray && mapLoaded && didInitialCam) {
            didZoomForTray = true
            delay(120)
            cameraPositionState.animate(
                update = CameraUpdate.zoomTo(18.0),
                durationMs = 450
            )
        }
    }

    fun hapticArm() {
        strongVibrate(context)
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        view.performHapticFeedback(HapticFeedbackConstants.DRAG_START)
    }

    fun persistNow() {
        // ✅ 배치/편성 변경 시 즉시 저장 (선택사항이지만 형님 요구 기능에 유리)
        incidentViewModel.saveCurrentIncident(context)
    }

    fun dropPayloadIfPossible() {
        val mapRect = mapRectInWindow ?: return
        val mapObj = naverMapObj ?: return
        val payload = dragState.payload ?: return

        val dropPos = dragState.windowPos
        val insideMap =
            dropPos.x in mapRect.left..mapRect.right &&
                    dropPos.y in mapRect.top..mapRect.bottom
        if (!insideMap) return

        val localX = (dropPos.x - mapRect.left).toFloat()
        val localY = (dropPos.y - mapRect.top).toFloat()
        val latLng = mapObj.projection.fromScreenLocation(PointF(localX, localY))

        // ✅✅ 핵심: upsertPlacement 같은 함수 쓰지 말고, 현재 VM의 placeVehicle로 통일
        incidentViewModel.placeVehicle(
            id = payload.id,
            department = payload.department,
            equipment = payload.equipment,
            latLng = latLng
        )

        persistNow()
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    // ✅ 지도 위 가까운 배치 차량 찾기(롱프레스 위치 기준)
    fun findNearestPlacedPayload(localPosInMap: Offset): DragPayload? {
        val mapObj = naverMapObj ?: return null
        val threshold = with(density) { 52.dp.toPx() }

        var best: DragPayload? = null
        var bestDist = Float.MAX_VALUE

        for (pv in incidentViewModel.placedVehicles) {
            val pt = mapObj.projection.toScreenLocation(pv.position)
            val dx = pt.x - localPosInMap.x
            val dy = pt.y - localPosInMap.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < bestDist) {
                bestDist = dist
                best = DragPayload(pv.id, pv.department, pv.equipment)
            }
        }

        return if (bestDist <= threshold) best else null
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
                .pointerInput(mapLoaded, incidentViewModel.placedVehicles) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)

                        val longPressChange = awaitLongPressOrCancellation(down.id)
                            ?: return@awaitEachGesture

                        val payload = findNearestPlacedPayload(longPressChange.position)
                            ?: return@awaitEachGesture

                        val mapRect = mapRectInWindow ?: return@awaitEachGesture

                        hapticArm()
                        dragState = DragState(
                            active = true,
                            payload = payload,
                            windowPos = Offset(
                                mapRect.left + longPressChange.position.x,
                                mapRect.top + longPressChange.position.y
                            ),
                            wobble = true
                        )

                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break

                            if (change.changedToUp()) {
                                dropPayloadIfPossible()
                                dragState = DragState(active = false)
                                break
                            }

                            change.consumeAllChanges()

                            val mapRect2 = mapRectInWindow ?: continue
                            dragState = dragState.copy(
                                windowPos = Offset(
                                    mapRect2.left + change.position.x,
                                    mapRect2.top + change.position.y
                                ),
                                wobble = true
                            )
                        }
                    }
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

                if (lat != null && lng != null) {
                    Marker(
                        state = markerState,
                        captionText = "현장"
                    )
                }

                incidentViewModel.placedVehicles.forEach { pv ->
                    key(pv.id) {
                        val st = rememberMarkerState(position = pv.position)
                        LaunchedEffect(pv.position) { st.position = pv.position }

                        val iconRes = VehicleIconMapper.iconResForEquip(pv.equipment)
                        val label = VehicleIconMapper.deptLabel(pv.department)

                        if (iconRes != 0) {
                            Marker(
                                state = st,
                                icon = OverlayImage.fromResource(iconRes),
                                width = iconSize,
                                height = iconSize,
                                captionText = label,
                                captionColor = Color.White,
                                captionHaloColor = Color.Black
                            )
                        } else {
                            Marker(
                                state = st,
                                captionText = label,
                                captionColor = Color.White,
                                captionHaloColor = Color.Black
                            )
                        }
                    }
                }
            }
        }

        // =============================
        // 2) 좌슬라이딩 엣지
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
                    // ✅ clearPlacements() 같은 미존재 함수 금지 → clearPlacedVehicles()
                    incidentViewModel.clearIncident()
                    incidentViewModel.clearPlacedVehicles()
                    persistNow()
                    onExit()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1C1C1C),
                    contentColor = MarsOrange
                )
            ) { Text("EXIT") }
        }

        // =============================
        // 5) 하단 트레이
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

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scroll),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    notPlaced.forEach { item ->
                        TrayChipDraggableAfterLongPress(
                            item = item,
                            deptLabel = VehicleIconMapper.deptLabel(item.department),
                            iconRes = VehicleIconMapper.iconResForEquip(item.equipment),
                            onLift = { windowPos ->
                                hapticArm()
                                dragState = DragState(
                                    active = true,
                                    payload = DragPayload(item.id, item.department, item.equipment),
                                    windowPos = windowPos,
                                    wobble = true
                                )
                            },
                            onMove = { windowPos ->
                                if (dragState.active && dragState.payload?.id == item.id) {
                                    dragState = dragState.copy(windowPos = windowPos, wobble = true)
                                }
                            },
                            onDrop = {
                                dropPayloadIfPossible()
                                dragState = DragState(active = false)
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }
        }

        // =============================
        // 6) 드래그 공중 프리뷰(여기만 떨림)
        // =============================
        if (dragState.active && dragState.payload != null) {
            val payload = dragState.payload!!
            val iconRes = VehicleIconMapper.iconResForEquip(payload.equipment)
            val label = VehicleIconMapper.deptLabel(payload.department)

            Box(
                modifier = Modifier.offset {
                    IntOffset(
                        dragState.windowPos.x.roundToInt() - 70,
                        dragState.windowPos.y.roundToInt() - 28
                    )
                }
            ) {
                TrayChip(iconRes = iconRes, text = label, wobble = true)
            }
        }
    }
}

@Composable
private fun TrayChipDraggableAfterLongPress(
    item: IncidentViewModel.StickerItem,
    deptLabel: String,
    iconRes: Int,
    onLift: (Offset) -> Unit,
    onMove: (Offset) -> Unit,
    onDrop: () -> Unit,
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
                detectDragGesturesAfterLongPress(
                    onDragStart = { localStart ->
                        onLift(chipTopLeftInWindow + localStart)
                    },
                    onDrag = { change, _ ->
                        onMove(chipTopLeftInWindow + change.position)
                    },
                    onDragEnd = { onDrop() },
                    onDragCancel = { onDrop() }
                )
            }
    ) {
        TrayChip(iconRes = iconRes, text = deptLabel, wobble = false)
    }
}

@Composable
private fun TrayChip(
    iconRes: Int,
    text: String,
    wobble: Boolean
) {
    var sign by remember { mutableStateOf(1f) }
    LaunchedEffect(wobble) {
        if (!wobble) {
            sign = 1f
            return@LaunchedEffect
        }
        while (true) {
            sign *= -1f
            delay(90)
        }
    }

    val rot = if (wobble) 4f * sign else 0f

    Row(
        modifier = Modifier
            .height(40.dp)
            .wrapContentWidth()
            .graphicsLayer { rotationZ = rot }
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
