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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.sp
import com.example.marthianclean.R
import com.example.marthianclean.model.FireType
import com.example.marthianclean.model.MarkerIconMapper
import com.example.marthianclean.ui.sticker.VehicleIconMapper
import com.example.marthianclean.viewmodel.IncidentViewModel
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.compose.*
import com.naver.maps.map.overlay.OverlayImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val MarsOrange = Color(0xFFFF8C00)
private val TextPrimary = Color(0xFFF0F0F0)
private val BgBlack = Color(0xFF0E0E0E)
private val BorderGray = Color(0xFF2E2E2E)

private enum class RightPanelMode {
    NONE, HUB, BRIEFING, FORCE_STATUS
}

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

private fun vehicleScaleFor(equipment: String): Float {
    val e = equipment.trim()
    return when {
        e.contains("구조공작") || e.contains("구조") || e.contains("rescue") -> 3.0f
        e.contains("구급") || e.contains("ambul") -> 2.0f
        e.contains("장비운반") || e.contains("equipment") -> 2.0f
        e.contains("펌프") -> 2.4f
        e.contains("지휘") || e.contains("command") -> 2.4f
        e.contains("탱크") || e.contains("급수") -> 2.66f
        e.contains("포크") || e.contains("굴삭") || e.contains("excava") -> 3.2f
        e.contains("화학") || e.contains("haz") -> 4.0f
        e.contains("고가") || e.contains("사다리") || e.contains("ladder") -> 4.8f
        e.contains("굴절") || e.contains("articul") -> 4.8f
        e.contains("무인") || e.contains("방수") || e.contains("파괴") || e.contains("water") -> 6.24f
        e.contains("회복") || e.contains("버스") || e.contains("recovery") || e.contains("bus") -> 4.8f
        else -> 0.8f
    }
}

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

    var rightMode by remember { mutableStateOf(RightPanelMode.NONE) }

    var isSectorMode by remember { mutableStateOf(false) }
    var sectorTargetVehicleId by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    val cameraPositionState = rememberCameraPositionState()
    val markerState = remember { MarkerState() }
    var mapLoaded by remember { mutableStateOf(false) }

    var isSatellite by remember { mutableStateOf(true) }

    val lat = incident?.latitude
    val lng = incident?.longitude

    var naverMapObj by remember { mutableStateOf<com.naver.maps.map.NaverMap?>(null) }
    var mapRectInWindow by remember { mutableStateOf<Rect?>(null) }

    var dragState by remember { mutableStateOf(DragState()) }

    var sceneDragActive by remember { mutableStateOf(false) }
    var sceneDragWindowPos by remember { mutableStateOf(Offset.Zero) }

    val triggerPx = with(density) { 80.dp.toPx() }
    val edgeWidth = 56.dp
    val edgePx = with(density) { edgeWidth.toPx() }

    var dragAccumX by remember { mutableStateOf(0f) }
    var dragAccumY by remember { mutableStateOf(0f) }
    var lastPos by remember { mutableStateOf(Offset.Zero) }

    val stickerQueue = incidentViewModel.buildStickerQueue()
    val placedIds = incidentViewModel.placedVehicles.map { it.id }.toSet()
    val notPlaced = stickerQueue.filterNot { placedIds.contains(it.id) }

    val placedCount = incidentViewModel.placedVehicles.size
    val totalToPlace = stickerQueue.size
    val remainingToPlace = max(0, totalToPlace - placedCount)
    val showTray = remainingToPlace > 0

    val sceneIconBaseSize: Dp = 90.dp

    var didInitialCam by remember { mutableStateOf(false) }
    LaunchedEffect(lat, lng, mapLoaded, showTray, incidentViewModel.preferredMapZoom) {
        if (!didInitialCam && mapLoaded && lat != null && lng != null) {
            didInitialCam = true
            val pos = LatLng(lat, lng)
            markerState.position = pos
            cameraPositionState.animate(
                update = CameraUpdate.scrollTo(pos),
                animation = CameraAnimation.Easing,
                durationMs = 700
            )
            val targetZoom = incidentViewModel.preferredMapZoom ?: (if (showTray) 18.0 else 16.0)
            cameraPositionState.animate(
                update = CameraUpdate.zoomTo(targetZoom),
                animation = CameraAnimation.Easing,
                durationMs = 320
            )
        }
    }

    var didZoomForTray by remember { mutableStateOf(false) }
    LaunchedEffect(showTray, mapLoaded, didInitialCam, incidentViewModel.preferredMapZoom) {
        if (incidentViewModel.preferredMapZoom != null) return@LaunchedEffect
        if (!didZoomForTray && showTray && mapLoaded && didInitialCam) {
            didZoomForTray = true
            delay(120)
            cameraPositionState.animate(
                update = CameraUpdate.zoomTo(18.0),
                animation = CameraAnimation.Easing,
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
        incidentViewModel.saveCurrentIncident(context)
    }

    fun dropPayloadIfPossible() {
        val mapRect = mapRectInWindow ?: return
        val mapObj = naverMapObj ?: return
        val payload = dragState.payload ?: return

        val dropPos = dragState.windowPos
        val insideMap = dropPos.x in mapRect.left..mapRect.right && dropPos.y in mapRect.top..mapRect.bottom
        if (!insideMap) return

        val localX = (dropPos.x - mapRect.left).toFloat()
        val localY = (dropPos.y - mapRect.top).toFloat()
        val latLng = mapObj.projection.fromScreenLocation(PointF(localX, localY))

        incidentViewModel.placeVehicle(
            id = payload.id,
            department = payload.department,
            equipment = payload.equipment,
            latLng = latLng
        )
        persistNow()
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

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

    fun isNearSceneMarker(localPosInMap: Offset): Boolean {
        val mapObj = naverMapObj ?: return false
        val ilat = incident?.latitude ?: return false
        val ilng = incident?.longitude ?: return false
        val scenePos = LatLng(ilat, ilng)
        val pt = mapObj.projection.toScreenLocation(scenePos)
        val dx = pt.x - localPosInMap.x
        val dy = pt.y - localPosInMap.y
        val dist = sqrt(dx * dx + dy * dy)
        val threshold = with(density) { 60.dp.toPx() }
        return dist <= threshold
    }

    fun dropSceneIfPossible() {
        val mapRect = mapRectInWindow ?: return
        val mapObj = naverMapObj ?: return
        val dropPos = sceneDragWindowPos
        val insideMap = dropPos.x in mapRect.left..mapRect.right && dropPos.y in mapRect.top..mapRect.bottom
        if (!insideMap) return

        val localX = (dropPos.x - mapRect.left).toFloat()
        val localY = (dropPos.y - mapRect.top).toFloat()
        val newLatLng = mapObj.projection.fromScreenLocation(PointF(localX, localY))

        incidentViewModel.updateSceneLocationFromDrag(context, newLatLng)
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    val panelActive = rightMode != RightPanelMode.NONE

    Row(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(
            modifier = Modifier
                .weight(if (panelActive) 2f else 1f)
                .fillMaxHeight()
                .background(Color.Black)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coords -> mapRectInWindow = coords.boundsInWindow() }
                    .pointerInput(mapLoaded, incidentViewModel.placedVehicles, incident?.latitude, incident?.longitude, rightMode, isSectorMode) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startX = down.position.x
                            val isLeftEdge = startX <= edgePx
                            val isRightEdge = startX >= (size.width - edgePx)

                            if (isLeftEdge || isRightEdge) {
                                var accX = 0f
                                var accY = 0f
                                var last = down.position

                                while (true) {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Main)
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (change.changedToUp()) break
                                    change.consumeAllChanges()
                                    val dx = change.position.x - last.x
                                    val dy = abs(change.position.y - last.y)
                                    accX += dx
                                    accY += dy
                                    last = change.position
                                    val mostlyHorizontal = abs(accX) > accY * 1.3f
                                    if (isLeftEdge && mostlyHorizontal && accX >= triggerPx) {
                                        onEdit()
                                        break
                                    }
                                    if (isRightEdge && mostlyHorizontal && accX <= -triggerPx) {
                                        if (rightMode == RightPanelMode.NONE) {
                                            rightMode = RightPanelMode.HUB
                                        }
                                        break
                                    }
                                }
                                return@awaitEachGesture
                            }

                            val longPressChange = awaitLongPressOrCancellation(down.id)

                            if (longPressChange == null) {
                                if (isSectorMode) {
                                    val payload = findNearestPlacedPayload(down.position)
                                    sectorTargetVehicleId = payload?.id
                                    if (payload != null) strongVibrate(context)
                                }
                                return@awaitEachGesture
                            }

                            val isScene = isNearSceneMarker(longPressChange.position)
                            if (isScene) {
                                val mapRect = mapRectInWindow ?: return@awaitEachGesture
                                hapticArm()
                                sceneDragActive = true
                                sceneDragWindowPos = Offset(
                                    mapRect.left + longPressChange.position.x,
                                    mapRect.top + longPressChange.position.y
                                )
                                while (true) {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Main)
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (change.changedToUp()) {
                                        dropSceneIfPossible()
                                        sceneDragActive = false
                                        break
                                    }
                                    change.consumeAllChanges()
                                    val mapRect2 = mapRectInWindow ?: continue
                                    sceneDragWindowPos = Offset(
                                        mapRect2.left + change.position.x,
                                        mapRect2.top + change.position.y
                                    )
                                }
                                return@awaitEachGesture
                            }

                            val payload = findNearestPlacedPayload(longPressChange.position) ?: return@awaitEachGesture
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
                val currentZoom = cameraPositionState.position.zoom
                val zoomFactor = 2.0.pow(currentZoom - 18.0).toFloat()

                NaverMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(mapType = if (isSatellite) MapType.Satellite else MapType.Basic),
                    uiSettings = MapUiSettings(
                        isZoomControlEnabled = false,
                        isCompassEnabled = false,
                        isLocationButtonEnabled = false,
                        isRotateGesturesEnabled = false,
                        isTiltGesturesEnabled = false
                    ),
                    onMapLoaded = { mapLoaded = true }
                ) {
                    MapEffect(Unit) { map -> naverMapObj = map }

                    if (lat != null && lng != null) {
                        val fireType = FireType.from(incident?.meta?.fireType)
                        val markerRes = MarkerIconMapper.markerResFor(fireType)

                        LaunchedEffect(sceneDragActive, sceneDragWindowPos) {
                            if (!sceneDragActive) return@LaunchedEffect
                            val mapRect = mapRectInWindow ?: return@LaunchedEffect
                            val mapObj = naverMapObj ?: return@LaunchedEffect
                            val localX = (sceneDragWindowPos.x - mapRect.left).toFloat()
                            val localY = (sceneDragWindowPos.y - mapRect.top).toFloat()
                            markerState.position = mapObj.projection.fromScreenLocation(PointF(localX, localY))
                        }
                        LaunchedEffect(lat, lng, sceneDragActive) {
                            if (!sceneDragActive) {
                                markerState.position = LatLng(lat, lng)
                            }
                        }

                        val dynamicSceneSize = (sceneIconBaseSize.value * zoomFactor).coerceAtLeast(1f).dp

                        Marker(
                            state = markerState,
                            icon = OverlayImage.fromResource(markerRes),
                            width = dynamicSceneSize,
                            height = dynamicSceneSize,
                            isIconPerspectiveEnabled = false,
                            captionText = "현장",
                            captionColor = Color.White,
                            captionHaloColor = Color.Black
                        )
                    }

                    // ✅ 차량 렌더링
                    incidentViewModel.placedVehicles.forEach { pv ->
                        key(pv.id) {
                            val st = rememberMarkerState(position = pv.position)

                            LaunchedEffect(pv.position) {
                                st.position = pv.position
                            }

                            val equipRaw = pv.equipment
                            val iconRes = VehicleIconMapper.iconResForEquip(equipRaw)
                            val label = VehicleIconMapper.deptLabel(pv.department)
                            val scale = vehicleScaleFor(equipRaw)
                            val baseSize = 26.dp

                            val markerHeight = (baseSize.value * scale * zoomFactor).coerceAtLeast(1f).dp
                            val markerWidth = if (equipRaw.contains("탱크") || equipRaw.contains("급수")) {
                                (markerHeight.value * 1.6f).dp
                            } else {
                                markerHeight
                            }

                            Marker(
                                state = st,
                                icon = OverlayImage.fromResource(iconRes),
                                width = markerWidth,
                                height = markerHeight,
                                isIconPerspectiveEnabled = false,
                                captionText = label,
                                captionColor = Color.White,
                                captionHaloColor = Color.Black,
                                captionOffset = 0.dp,
                                // 💡 [해결 1] 궤도 이탈 & 숨김 방지를 위한 차량 정중앙 고정!
                                anchor = Offset(0.5f, 0.5f)
                            )
                        }
                    }

                    fun rotateMapTo(targetBearing: Double, targetLatLng: LatLng) {
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                update = CameraUpdate.toCameraPosition(
                                    com.naver.maps.map.CameraPosition(
                                        targetLatLng,
                                        cameraPositionState.position.zoom,
                                        0.0,
                                        targetBearing % 360.0
                                    )
                                ),
                                animation = CameraAnimation.Easing,
                                durationMs = 800
                            )
                        }
                    }

                    // ✅ 마법의 isFlat & 크기 비율 연동 간격 로직
                    if (isSectorMode && sectorTargetVehicleId != null) {
                        val targetVehicle = incidentViewModel.placedVehicles.find { it.id == sectorTargetVehicleId }

                        targetVehicle?.let { vehicle ->
                            val centerLat = vehicle.position.latitude
                            val centerLng = vehicle.position.longitude
                            val scale = vehicleScaleFor(vehicle.equipment)

                            // 💡 [해결 2] 차량 크기에 비례하여 완벽하게 여백 확보! (겹침 방지)
                            val dist = 0.000055 * scale
                            val cosLat = kotlin.math.cos(centerLat * Math.PI / 180.0)
                            val latOffset = dist
                            val lngOffset = dist / cosLat

                            val arrowSize = (40f * zoomFactor).coerceAtLeast(1f).dp

                            // 북쪽 화살표 (클릭 시 북쪽(180도)이 화면 아래로)
                            Marker(
                                state = MarkerState(position = LatLng(centerLat + latOffset, centerLng)),
                                icon = OverlayImage.fromResource(R.drawable.ic_arrow_up),
                                width = arrowSize, height = arrowSize,
                                isFlat = true, // 💡 [해결 3] 지도를 돌리면 이미지도 알아서 예쁘게 착착 돌아감!
                                angle = 0f,
                                anchor = Offset(0.5f, 0.5f),
                                zIndex = 100, // 💡 차량보다 항상 위에 표시!
                                onClick = { rotateMapTo(180.0, vehicle.position); true }
                            )
                            // 남쪽 화살표
                            Marker(
                                state = MarkerState(position = LatLng(centerLat - latOffset, centerLng)),
                                icon = OverlayImage.fromResource(R.drawable.ic_arrow_down),
                                width = arrowSize, height = arrowSize,
                                isFlat = true,
                                angle = 0f,
                                anchor = Offset(0.5f, 0.5f),
                                zIndex = 100,
                                onClick = { rotateMapTo(0.0, vehicle.position); true }
                            )
                            // 동쪽 화살표
                            Marker(
                                state = MarkerState(position = LatLng(centerLat, centerLng + lngOffset)),
                                icon = OverlayImage.fromResource(R.drawable.ic_arrow_right),
                                width = arrowSize, height = arrowSize,
                                isFlat = true,
                                angle = 0f,
                                anchor = Offset(0.5f, 0.5f),
                                zIndex = 100,
                                onClick = { rotateMapTo(270.0, vehicle.position); true }
                            )
                            // 서쪽 화살표
                            Marker(
                                state = MarkerState(position = LatLng(centerLat, centerLng - lngOffset)),
                                icon = OverlayImage.fromResource(R.drawable.ic_arrow_left),
                                width = arrowSize, height = arrowSize,
                                isFlat = true,
                                angle = 0f,
                                anchor = Offset(0.5f, 0.5f),
                                zIndex = 100,
                                onClick = { rotateMapTo(90.0, vehicle.position); true }
                            )
                        }
                    }
                }
            }

            if (mapLoaded) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 16.dp)
                        .background(
                            color = if (isSectorMode) MarsOrange else Color(0xFF1C1C1C).copy(alpha = 0.8f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, if (isSectorMode) Color.White else BorderGray, RoundedCornerShape(8.dp))
                        .clickable {
                            isSectorMode = !isSectorMode
                            if (!isSectorMode) sectorTargetVehicleId = null
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (isSectorMode) "방면지휘 ON" else "방면지휘",
                        color = if (isSectorMode) Color.White else MarsOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomEnd)
                    .padding(bottom = if (showTray) 90.dp else 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { isSatellite = !isSatellite },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1C1C1C).copy(alpha = 0.8f),
                        contentColor = Color.White
                    )
                ) { Text(if (isSatellite) "SAT" else "BASIC") }

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = {
                        incidentViewModel.clearIncident()
                        incidentViewModel.clearPlacedVehicles()
                        persistNow()
                        onExit()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1C1C1C).copy(alpha = 0.8f),
                        contentColor = MarsOrange
                    )
                ) { Text("EXIT") }
            }

            if (!panelActive && showTray) {
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

        if (panelActive) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(BgBlack)
                    .border(1.dp, BorderGray)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(18.dp)
                        .align(Alignment.CenterStart)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { start ->
                                    dragAccumX = 0f
                                    dragAccumY = 0f
                                    lastPos = start
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consumeAllChanges()
                                    dragAccumX += dragAmount
                                    val dy = abs(change.position.y - lastPos.y)
                                    dragAccumY += dy
                                    lastPos = change.position
                                    val mostlyH = dragAccumX > dragAccumY * 1.3f
                                    if (mostlyH && dragAccumX >= triggerPx) {
                                        rightMode = RightPanelMode.NONE
                                        dragAccumX = 0f
                                        dragAccumY = 0f
                                    }
                                },
                                onDragEnd = { dragAccumX = 0f; dragAccumY = 0f },
                                onDragCancel = { dragAccumX = 0f; dragAccumY = 0f }
                            )
                        }
                )

                when (rightMode) {
                    RightPanelMode.HUB -> HubPanel(
                        onBriefing = { rightMode = RightPanelMode.BRIEFING },
                        onForceStatus = { rightMode = RightPanelMode.FORCE_STATUS },
                        onClose = { rightMode = RightPanelMode.NONE }
                    )
                    RightPanelMode.BRIEFING -> BriefingPanel(
                        incidentViewModel = incidentViewModel,
                        onBackToHub = { rightMode = RightPanelMode.HUB },
                        onClose = { rightMode = RightPanelMode.NONE }
                    )
                    RightPanelMode.FORCE_STATUS -> ForceStatusPanel(
                        incidentViewModel = incidentViewModel,
                        onBackToHub = { rightMode = RightPanelMode.HUB },
                        onClose = { rightMode = RightPanelMode.NONE }
                    )
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun HubPanel(
    onBriefing: () -> Unit,
    onForceStatus: () -> Unit,
    onClose: () -> Unit
) {
    val vScroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(vScroll)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "허브", color = MarsOrange, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(
                text = "닫기",
                color = MarsOrange,
                modifier = Modifier
                    .border(1.dp, BorderGray)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .noRippleClick { onClose() }
            )
        }
        PanelButton(title = "브리핑 모드", desc = "현장 정보 + 편성/배치 요약") { onBriefing() }
        PanelButton(title = "소방력현황", desc = "현재(배치) + 참고(편성) 집계") { onForceStatus() }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "※ 허브는 우→좌 슬라이딩으로 열고\n※ 패널은 좌→우 슬라이딩으로 닫습니다.",
            color = TextPrimary.copy(alpha = 0.75f),
            fontSize = 12.sp
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PanelButton(title: String, desc: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray)
            .background(Color(0xFF111111))
            .padding(14.dp)
            .noRippleClick(onClick)
    ) {
        Text(text = title, color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(text = desc, color = TextPrimary.copy(alpha = 0.75f))
    }
}

@Composable
private fun BriefingPanel(
    incidentViewModel: IncidentViewModel,
    onBackToHub: () -> Unit,
    onClose: () -> Unit
) {
    val vScroll = rememberScrollState()
    val incident by incidentViewModel.incident.collectAsState()
    val meta = incident?.meta
    val placed = incidentViewModel.getPlacedCount()

    fun show(v: String?): String = v?.trim()?.takeIf { it.isNotBlank() } ?: "-"
    fun formatCelsius(raw: String): String {
        val v = raw.trim()
        if (v.isBlank() || v == "-") return "-"
        if (v.contains("℃") || v.contains("°")) return v
        return "${v}℃"
    }

    val DamageRed = Color(0xFFFF1744)

    @Composable
    fun DamageRowCard(rows: List<Pair<String, String>>) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderGray)
                .background(Color(0xFF111111))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rows.forEach { (k, v) ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(text = k, color = MarsOrange, fontSize = 12.sp, modifier = Modifier.width(90.dp))
                    Text(text = v, color = DamageRed, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(vScroll)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "브리핑", color = MarsOrange, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(
                text = "허브",
                color = MarsOrange,
                modifier = Modifier
                    .border(1.dp, BorderGray)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .noRippleClick { onBackToHub() }
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "닫기",
                color = MarsOrange,
                modifier = Modifier
                    .border(1.dp, BorderGray)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .noRippleClick { onClose() }
            )
        }

        SectionTitle("현장 핵심")
        InfoCard("위치", show(incident?.address))
        InfoCard("처종", show(meta?.fireType))
        InfoCard("신고접수", show(meta?.신고접수))
        InfoCard("화재원인", show(meta?.화재원인))
        if (meta?.memo?.isNotBlank() == true) {
            InfoCard("메모", show(meta?.memo))
        }

        SectionTitle("시간")
        InfoRowCard(
            rows = listOf(
                "초진시간" to show(meta?.초진시간),
                "완진시간" to show(meta?.완진시간),
                "선착대도착" to show(meta?.선착대도착시간)
            )
        )

        SectionTitle("피해")
        DamageRowCard(
            rows = listOf(
                "인명피해" to show(meta?.인명피해현황),
                "재산피해" to show(meta?.재산피해현황),
                "대원피해" to show(meta?.대원피해현황)
            )
        )

        SectionTitle("소방력")
        InfoCard("차량", "${placed}대")

        SectionTitle("기상")
        InfoRowCard(
            rows = listOf(
                "날씨" to show(meta?.기상_날씨),
                "기온" to formatCelsius(show(meta?.기상_기온)),
                "풍향/풍속" to show(meta?.기상_풍향풍속)
            )
        )

        SectionTitle("유관기관")
        InfoRowCard(
            rows = listOf(
                "경찰" to show(meta?.유관기관_경찰),
                "시청" to show(meta?.유관기관_시청),
                "한전" to show(meta?.유관기관_한전),
                "도시가스" to show(meta?.유관기관_도시가스),
                "산불진화대(화성시)" to show(meta?.유관기관_산불진화대_화성시)
            )
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text = "※ 지도:패널 = 2:1\n※ 편성 변경/귀소/교대 반영은 즉시 반영됩니다.",
            color = TextPrimary.copy(alpha = 0.75f),
            fontSize = 12.sp
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ForceStatusPanel(
    incidentViewModel: IncidentViewModel,
    onBackToHub: () -> Unit,
    onClose: () -> Unit
) {
    val vScroll = rememberScrollState()
    val placed = incidentViewModel.placedVehicles

    val actualEquipCounts: List<Pair<String, Int>> = remember(placed) {
        placed.groupingBy { it.equipment }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
    }

    val actualDeptCounts: List<Pair<String, Int>> = remember(placed) {
        placed.groupingBy { it.department }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(vScroll)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "소방력현황", color = MarsOrange, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(
                text = "허브",
                color = MarsOrange,
                modifier = Modifier
                    .border(1.dp, BorderGray)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .noRippleClick { onBackToHub() }
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "닫기",
                color = MarsOrange,
                modifier = Modifier
                    .border(1.dp, BorderGray)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .noRippleClick { onClose() }
            )
        }

        SectionTitle("현재(실제 배치) - 차종별")
        if (actualEquipCounts.isEmpty()) {
            Text(text = "현재 지도에 배치된 차량이 없습니다.", color = TextPrimary.copy(alpha = 0.75f))
        } else {
            actualEquipCounts.forEach { (equip, cnt) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderGray)
                        .background(Color(0xFF111111))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val iconRes = VehicleIconMapper.iconResForEquip(equip)
                    if (iconRes != 0) {
                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(text = equip, color = TextPrimary, modifier = Modifier.weight(1f))
                    Text(text = "${cnt}대", color = MarsOrange, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        SectionTitle("현재(실제 배치) - 부서별")
        if (actualDeptCounts.isEmpty()) {
            Text(text = "현재 지도에 배치된 차량이 없습니다.", color = TextPrimary.copy(alpha = 0.75f))
        } else {
            actualDeptCounts.forEach { (dept, cnt) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderGray)
                        .background(Color(0xFF111111))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = VehicleIconMapper.deptLabel(dept),
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = "${cnt}대", color = MarsOrange, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "※ 집계 기준: 현재(실제 배치=지도 마커)",
            color = TextPrimary.copy(alpha = 0.75f),
            fontSize = 12.sp
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, color = MarsOrange, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun InfoCard(title: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray)
            .background(Color(0xFF111111))
            .padding(14.dp)
    ) {
        Text(text = title, color = MarsOrange, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Text(text = value, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun InfoRowCard(rows: List<Pair<String, String>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderGray)
            .background(Color(0xFF111111))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { (k, v) ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = k, color = MarsOrange, fontSize = 12.sp, modifier = Modifier.width(90.dp))
                Text(text = v, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun Modifier.noRippleClick(onClick: () -> Unit): Modifier =
    this.clickable(interactionSource = MutableInteractionSource(), indication = null) { onClick() }

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
                    onDragStart = { localStart -> onLift(chipTopLeftInWindow + localStart) },
                    onDrag = { change, _ -> onMove(chipTopLeftInWindow + change.position) },
                    onDragEnd = { onDrop() },
                    onDragCancel = { onDrop() }
                )
            }
    ) {
        TrayChip(iconRes = iconRes, text = deptLabel, wobble = false)
    }
}

@Composable
private fun TrayChip(iconRes: Int, text: String, wobble: Boolean) {
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
        Text(text = text, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}