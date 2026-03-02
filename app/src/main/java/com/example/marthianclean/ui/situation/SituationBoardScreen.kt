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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import com.naver.maps.map.overlay.OverlayImage

private val MarsOrange = Color(0xFFFF8C00)
private val CommandYellowGreen = Color(0xFFD4FF00)
private val TextPrimary = Color(0xFFF0F0F0)
private val BgBlack = Color(0xFF0E0E0E)
private val BorderGray = Color(0xFF2E2E2E)

private enum class RightPanelMode { NONE, HUB, BRIEFING, FORCE_STATUS }

private data class DragPayload(val id: String, val department: String, val equipment: String)

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
    var isMarkerLocked by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val reverseRepo = remember {
        com.example.marthianclean.network.ReverseGeocodingRepository(com.example.marthianclean.network.RetrofitClient.reverseGeocodingService)
    }

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

    val stickerQueue = listOf(IncidentViewModel.StickerItem("CMD_AUTO_01", "화성소방서 지휘단", "지휘차"))
    val placedIds = incidentViewModel.placedVehicles.map { it.id }.toSet()
    val notPlaced = stickerQueue.filterNot { placedIds.contains(it.id) }
    val showTray = notPlaced.isNotEmpty()

    val sceneIconBaseSize: Dp = 90.dp
    var didInitialCam by remember { mutableStateOf(false) }

    LaunchedEffect(lat, lng, mapLoaded) {
        if (!didInitialCam && mapLoaded && lat != null && lng != null) {
            didInitialCam = true
            val pos = LatLng(lat, lng)
            markerState.position = pos
            cameraPositionState.animate(update = CameraUpdate.scrollTo(pos), durationMs = 700)
            cameraPositionState.animate(update = CameraUpdate.zoomTo(17.5), durationMs = 320)
        }
    }

    fun hapticArm() {
        strongVibrate(context)
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    fun dropPayloadIfPossible() {
        val mapRect = mapRectInWindow ?: return
        val mapObj = naverMapObj ?: return
        val payload = dragState.payload ?: return
        val dropPos = dragState.windowPos
        if (dropPos.x !in mapRect.left..mapRect.right || dropPos.y !in mapRect.top..mapRect.bottom) return

        val localX = (dropPos.x - mapRect.left).toFloat()
        val localY = (dropPos.y - mapRect.top).toFloat()
        val latLng = mapObj.projection.fromScreenLocation(PointF(localX, localY))

        incidentViewModel.placeVehicle(payload.id, payload.department, payload.equipment, latLng)
        incidentViewModel.saveCurrentIncident(context)
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
        val pt = mapObj.projection.toScreenLocation(LatLng(ilat, ilng))
        val dx = pt.x - localPosInMap.x
        val dy = pt.y - localPosInMap.y
        return sqrt(dx * dx + dy * dy) <= with(density) { 60.dp.toPx() }
    }

    fun dropSceneIfPossible() {
        val mapRect = mapRectInWindow ?: return
        val mapObj = naverMapObj ?: return
        val dropPos = sceneDragWindowPos
        if (dropPos.x !in mapRect.left..mapRect.right || dropPos.y !in mapRect.top..mapRect.bottom) return

        val localX = (dropPos.x - mapRect.left).toFloat()
        val localY = (dropPos.y - mapRect.top).toFloat()
        val latLng = mapObj.projection.fromScreenLocation(PointF(localX, localY))
        incidentViewModel.updateSceneLocationFromDrag(context, latLng)
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    val panelActive = rightMode != RightPanelMode.NONE

    Row(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(modifier = Modifier.weight(if (panelActive) 2f else 1f).fillMaxHeight().background(Color.Black)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coords -> mapRectInWindow = coords.boundsInWindow() }
                    .pointerInput(mapLoaded, incidentViewModel.placedVehicles, rightMode, isSectorMode, isMarkerLocked) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val longPressChange = awaitLongPressOrCancellation(down.id)

                            if (longPressChange == null) {
                                if (isSectorMode) {
                                    val payload = findNearestPlacedPayload(down.position)
                                    if (payload != null) {
                                        sectorTargetVehicleId = payload.id
                                        strongVibrate(context)
                                    }
                                }
                                return@awaitEachGesture
                            }

                            val canDragMarkers = !isMarkerLocked && rightMode != RightPanelMode.BRIEFING
                            if (canDragMarkers && isNearSceneMarker(longPressChange.position)) {
                                val mapRect = mapRectInWindow ?: return@awaitEachGesture
                                hapticArm()
                                sceneDragActive = true
                                sceneDragWindowPos = Offset(mapRect.left + longPressChange.position.x, mapRect.top + longPressChange.position.y)
                                while (true) {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Main)
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (change.changedToUp()) { dropSceneIfPossible(); sceneDragActive = false; break }
                                    change.consumeAllChanges()
                                    val mR = mapRectInWindow ?: continue
                                    sceneDragWindowPos = Offset(mR.left + change.position.x, mR.top + change.position.y)
                                }
                                return@awaitEachGesture
                            }
                            if (canDragMarkers) {
                                val payload = findNearestPlacedPayload(longPressChange.position) ?: return@awaitEachGesture
                                val mapRect = mapRectInWindow ?: return@awaitEachGesture
                                hapticArm()
                                dragState = DragState(active = true, payload = payload, windowPos = Offset(mapRect.left + longPressChange.position.x, mapRect.top + longPressChange.position.y), wobble = true)
                                while (true) {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Main)
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (change.changedToUp()) { dropPayloadIfPossible(); dragState = DragState(active = false); break }
                                    change.consumeAllChanges()
                                    val mR = mapRectInWindow ?: continue
                                    dragState = dragState.copy(windowPos = Offset(mR.left + change.position.x, mR.top + change.position.y), wobble = true)
                                }
                            }
                        }
                    }
            ) {
                val currentZoom = cameraPositionState.position.zoom
                val zoomFactor = 2.0.pow(currentZoom - 17.5).toFloat()

                NaverMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(mapType = if (isSatellite) MapType.Satellite else MapType.Basic),
                    uiSettings = MapUiSettings(isZoomControlEnabled = false, isCompassEnabled = false, isLocationButtonEnabled = false),
                    onMapLoaded = { mapLoaded = true }
                ) {
                    MapEffect(Unit) { map -> naverMapObj = map }

                    if (lat != null && lng != null) {
                        val fireType = FireType.from(incident?.meta?.fireType)
                        val markerRes = MarkerIconMapper.markerResFor(fireType)
                        val dynamicSceneSize = (sceneIconBaseSize.value * zoomFactor).coerceAtLeast(1f).dp
                        Marker(state = markerState, icon = OverlayImage.fromResource(markerRes), width = dynamicSceneSize, height = dynamicSceneSize, captionText = "현장", captionColor = Color.White, captionHaloColor = Color.Black)
                    }

                    incidentViewModel.placedVehicles.forEach { pv ->
                        key(pv.id) {
                            val isCommand = pv.equipment.contains("지휘")
                            val st = rememberMarkerState(position = pv.position)
                            val iconRes = VehicleIconMapper.iconResForEquip(pv.equipment)
                            val label = VehicleIconMapper.deptLabel(pv.department)

                            val scale = if (isCommand) 5.0f else vehicleScaleFor(pv.equipment)
                            val baseSize = 26.dp
                            val markerHeight = (baseSize.value * scale * zoomFactor).coerceAtLeast(1f).dp
                            val markerWidth = if (pv.equipment.contains("탱크")) (markerHeight.value * 1.6f).dp else markerHeight

                            Marker(
                                state = st,
                                icon = OverlayImage.fromResource(iconRes),
                                width = markerWidth,
                                height = markerHeight,
                                captionText = label,
                                captionColor = if (isCommand) CommandYellowGreen else Color.White,
                                captionHaloColor = Color.Black,
                                anchor = Offset(0.5f, 0.5f),
                                zIndex = if (isCommand) 10 else 1
                            )
                        }
                    }

                    if (isSectorMode && sectorTargetVehicleId != null) {
                        val targetVehicle = incidentViewModel.placedVehicles.find { it.id == sectorTargetVehicleId }
                        targetVehicle?.let { vehicle ->
                            val centerLat = vehicle.position.latitude
                            val centerLng = vehicle.position.longitude

                            val bearing = cameraPositionState.position.bearing
                            val rightAzimuthRad = Math.toRadians(bearing + 90.0)
                            val leftAzimuthRad = Math.toRadians(bearing - 90.0)

                            // ✅ [황금비율 간격] 형님 스크린샷에 맞춰 65.0m -> 32.0m 로 최적화했습니다.
                            val gapMeters = 32.0

                            val latPerMeter = 1.0 / 111320.0
                            val cosLat = kotlin.math.cos(centerLat * Math.PI / 180.0)
                            val lngPerMeter = 1.0 / (111320.0 * cosLat)

                            val dLatRight = gapMeters * kotlin.math.cos(rightAzimuthRad) * latPerMeter
                            val dLngRight = gapMeters * kotlin.math.sin(rightAzimuthRad) * lngPerMeter

                            val dLatLeft = gapMeters * kotlin.math.cos(leftAzimuthRad) * latPerMeter
                            val dLngLeft = gapMeters * kotlin.math.sin(leftAzimuthRad) * lngPerMeter

                            val arrowSize = (70f * zoomFactor).coerceAtLeast(30f).dp

                            fun rotateMapBy(degrees: Double) {
                                coroutineScope.launch {
                                    val currentB = cameraPositionState.position.bearing
                                    val newBearing = (currentB + degrees + 360.0) % 360.0
                                    cameraPositionState.animate(
                                        update = CameraUpdate.toCameraPosition(
                                            com.naver.maps.map.CameraPosition(vehicle.position, cameraPositionState.position.zoom, 0.0, newBearing)
                                        ),
                                        animation = CameraAnimation.Easing,
                                        durationMs = 500
                                    )
                                }
                            }

                            // ⬅️ 화면 기준 좌측 화살표 (시계 방향 90도 회전)
                            Marker(
                                state = MarkerState(position = LatLng(centerLat + dLatLeft, centerLng + dLngLeft)),
                                icon = OverlayImage.fromResource(R.drawable.ic_turn_left),
                                width = arrowSize, height = arrowSize,
                                isFlat = false,
                                anchor = Offset(0.5f, 0.5f),
                                zIndex = 100,
                                onClick = { rotateMapBy(90.0); true }
                            )

                            // ➡️ 화면 기준 우측 화살표 (반시계 방향 90도 회전)
                            Marker(
                                state = MarkerState(position = LatLng(centerLat + dLatRight, centerLng + dLngRight)),
                                icon = OverlayImage.fromResource(R.drawable.ic_turn_right),
                                width = arrowSize, height = arrowSize,
                                isFlat = false,
                                anchor = Offset(0.5f, 0.5f),
                                zIndex = 100,
                                onClick = { rotateMapBy(-90.0); true }
                            )
                        }
                    }
                }
            }

            if (mapLoaded) {
                val bearing = cameraPositionState.position.bearing.toFloat()
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 20.dp, start = 20.dp)
                        .size(64.dp)
                        .background(Color(0xFF1C1C1C).copy(alpha = 0.85f), CircleShape)
                        .border(1.5.dp, BorderGray, CircleShape)
                        .clickable {
                            coroutineScope.launch { cameraPositionState.animate(CameraUpdate.toCameraPosition(com.naver.maps.map.CameraPosition(cameraPositionState.position.target, cameraPositionState.position.zoom, 0.0, 0.0))) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.graphicsLayer { rotationZ = -bearing }.size(54.dp).padding(2.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("N", color = Color(0xFFFF1744), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        Box(Modifier.width(3.dp).height(12.dp).background(Color(0xFFFF1744)))
                        Box(Modifier.width(3.dp).height(12.dp).background(Color.White))
                        Text("S", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }

                Column(modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 16.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TopBarButton(text = "차량편성/입력", onClick = onEdit)
                        TopBarButton(text = if (isSectorMode) "방면지휘 ON" else "방면지휘", isActive = isSectorMode, onClick = { isSectorMode = !isSectorMode; if (!isSectorMode) sectorTargetVehicleId = null })
                        TopBarButton(text = "브리핑모드", onClick = { rightMode = RightPanelMode.HUB })
                    }
                    TopBarButton(text = if (isMarkerLocked) "🔒 마커 잠금됨" else "🔓 마커 이동 가능", isActive = isMarkerLocked, onClick = { isMarkerLocked = !isMarkerLocked })
                }
            }

            Row(modifier = Modifier.fillMaxWidth().align(Alignment.BottomEnd).padding(bottom = if (showTray) 90.dp else 16.dp, end = 16.dp), horizontalArrangement = Arrangement.End) {
                Button(onClick = { isSatellite = !isSatellite }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1C).copy(alpha = 0.8f))) { Text(if (isSatellite) "SAT" else "BASIC") }
                Spacer(Modifier.width(10.dp))
                Button(onClick = { incidentViewModel.clearIncident(); onExit() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1C).copy(alpha = 0.8f), contentColor = MarsOrange)) { Text("EXIT") }
            }

            if (!panelActive && showTray) {
                Column(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).background(Color.Black.copy(alpha = 0.55f)).padding(12.dp).navigationBarsPadding()) {
                    Text(text = "지휘차 배치 대기", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        notPlaced.forEach { item ->
                            TrayChipDraggableAfterLongPress(
                                item = item, deptLabel = VehicleIconMapper.deptLabel(item.department), iconRes = VehicleIconMapper.iconResForEquip(item.equipment),
                                onLift = { windowPos -> hapticArm(); dragState = DragState(active = true, payload = DragPayload(item.id, item.department, item.equipment), windowPos = windowPos, wobble = true) },
                                onMove = { windowPos -> if (dragState.active && dragState.payload?.id == item.id) { dragState = dragState.copy(windowPos = windowPos, wobble = true) } },
                                onDrop = { dropPayloadIfPossible(); dragState = DragState(active = false) },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                }
            }

            if (dragState.active && dragState.payload != null) {
                Box(modifier = Modifier.offset { IntOffset(dragState.windowPos.x.roundToInt() - 70, dragState.windowPos.y.roundToInt() - 28) }) {
                    TrayChip(iconRes = VehicleIconMapper.iconResForEquip(dragState.payload!!.equipment), text = VehicleIconMapper.deptLabel(dragState.payload!!.department), wobble = true)
                }
            }
        }

        if (panelActive) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(BgBlack).border(1.dp, BorderGray)) {
                when (rightMode) {
                    RightPanelMode.HUB -> HubPanel(onBriefing = { rightMode = RightPanelMode.BRIEFING }, onForceStatus = { rightMode = RightPanelMode.FORCE_STATUS }, onClose = { rightMode = RightPanelMode.NONE })
                    RightPanelMode.BRIEFING -> BriefingPanel(incidentViewModel = incidentViewModel, onBackToHub = { rightMode = RightPanelMode.HUB }, onClose = { rightMode = RightPanelMode.NONE })
                    RightPanelMode.FORCE_STATUS -> ForceStatusPanel(incidentViewModel = incidentViewModel, onBackToHub = { rightMode = RightPanelMode.HUB }, onClose = { rightMode = RightPanelMode.NONE })
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun TopBarButton(text: String, isActive: Boolean = false, onClick: () -> Unit) {
    Box(modifier = Modifier.background(if (isActive) MarsOrange else Color(0xFF1C1C1C).copy(alpha = 0.8f), RoundedCornerShape(8.dp)).border(1.dp, if (isActive) Color.White else BorderGray, RoundedCornerShape(8.dp)).clickable { onClick() }.padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(text = text, color = if (isActive) Color.White else MarsOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun HubPanel(onBriefing: () -> Unit, onForceStatus: () -> Unit, onClose: () -> Unit) {
    val vScroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(vScroll).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("허브", color = MarsOrange, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("닫기", color = MarsOrange, modifier = Modifier.clickable { onClose() })
        }
        Box(modifier = Modifier.fillMaxWidth().border(1.dp, BorderGray).clickable { onBriefing() }.padding(14.dp)) {
            Column { Text("브리핑", color = TextPrimary, fontWeight = FontWeight.Bold); Text("현장 정보 요약", color = TextPrimary.copy(0.7f)) }
        }
        Box(modifier = Modifier.fillMaxWidth().border(1.dp, BorderGray).clickable { onForceStatus() }.padding(14.dp)) {
            Column { Text("소방력 현황", color = TextPrimary, fontWeight = FontWeight.Bold); Text("차량 및 부서 집계", color = TextPrimary.copy(0.7f)) }
        }
    }
}

@Composable
private fun BriefingPanel(incidentViewModel: IncidentViewModel, onBackToHub: () -> Unit, onClose: () -> Unit) {
    val incident by incidentViewModel.incident.collectAsState()
    val meta = incident?.meta
    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {
        Row { Text("브리핑", color = MarsOrange, fontWeight = FontWeight.Bold, fontSize = 20.sp); Spacer(Modifier.weight(1f)); Text("허브", color = MarsOrange, modifier = Modifier.clickable { onBackToHub() }); Spacer(Modifier.width(10.dp)); Text("닫기", color = MarsOrange, modifier = Modifier.clickable { onClose() }) }
        Spacer(Modifier.height(16.dp))
        Text("위치: ${incident?.address ?: "-"}", color = TextPrimary, fontSize = 18.sp)
        Text("처종: ${meta?.fireType ?: "-"}", color = TextPrimary, fontSize = 18.sp)
    }
}

@Composable
private fun ForceStatusPanel(incidentViewModel: IncidentViewModel, onBackToHub: () -> Unit, onClose: () -> Unit) {
    val placed = incidentViewModel.placedVehicles
    Column(modifier = Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState())) {
        Row { Text("소방력 현황", color = MarsOrange, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text("허브", color = MarsOrange, modifier = Modifier.clickable { onBackToHub() }) }
        placed.groupBy { it.equipment }.forEach { (equip, list) ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(equip, color = TextPrimary, modifier = Modifier.weight(1f))
                Text("${list.size}대", color = MarsOrange)
            }
        }
    }
}

@Composable
private fun TrayChipDraggableAfterLongPress(item: IncidentViewModel.StickerItem, deptLabel: String, iconRes: Int, onLift: (Offset) -> Unit, onMove: (Offset) -> Unit, onDrop: () -> Unit, modifier: Modifier = Modifier) {
    var chipPos by remember { mutableStateOf(Offset.Zero) }
    Box(modifier = modifier.onGloballyPositioned { chipPos = it.boundsInWindow().topLeft }.pointerInput(Unit) {
        detectDragGesturesAfterLongPress(onDragStart = { onLift(chipPos + it) }, onDrag = { change, _ -> onMove(chipPos + change.position) }, onDragEnd = { onDrop() }, onDragCancel = { onDrop() })
    }) { TrayChip(iconRes, deptLabel, false) }
}

@Composable
private fun TrayChip(iconRes: Int, text: String, wobble: Boolean) {
    var sign by remember { mutableStateOf(1f) }
    LaunchedEffect(wobble) { if (wobble) while (true) { sign *= -1f; delay(90) } }
    Row(modifier = Modifier.graphicsLayer { rotationZ = if (wobble) 4f * sign else 0f }.background(BgBlack, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (iconRes != 0) { Image(painterResource(iconRes), null, Modifier.size(24.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}