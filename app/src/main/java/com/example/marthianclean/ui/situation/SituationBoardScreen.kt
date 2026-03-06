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
import androidx.compose.ui.draw.alpha
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
import com.example.marthianclean.model.IncidentMeta
import com.example.marthianclean.viewmodel.IncidentViewModel
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.util.MarkerIcons

private val MarsOrange = Color(0xFFFF8C00)
private val CommandYellowGreen = Color(0xFFD4FF00)
private val TextPrimary = Color(0xFFF0F0F0)
private val BgBlack = Color(0xFF0E0E0E)
private val BorderGray = Color(0xFF2E2E2E)
private val NeonRed = Color(0xFFFF1744)
private val NeonOrange = Color(0xFFFF9100)

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
        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(100)
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
    // ✅ 끊어졌던 실시간 날씨 데이터 연결 복구!
    val weatherData by incidentViewModel.weatherData.collectAsState()

    var rightMode by remember { mutableStateOf(RightPanelMode.NONE) }
    var isSectorMode by remember { mutableStateOf(false) }
    var sectorTargetVehicleId by remember { mutableStateOf<String?>(null) }
    var isMarkerLocked by remember { mutableStateOf(false) }
    var isSceneMovable by remember { mutableStateOf(false) }
    var isMeasuringDistance by remember { mutableStateOf(false) }
    val measurePoints = remember { mutableStateListOf<LatLng>() }

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

    val stickerQueue = incidentViewModel.buildStickerQueue()
    val placedIds = incidentViewModel.placedVehicles.map { it.id }.toSet()
    val notPlaced = stickerQueue.filterNot { placedIds.contains(it.id) }
    val showTray = notPlaced.isNotEmpty()
    val sceneIconBaseSize: Dp = 90.dp
    var didInitialCam by remember { mutableStateOf(false) }

    val panelActive = rightMode != RightPanelMode.NONE

    // ✅ 화면 띄울 때 기상청 데이터 자동으로 불러오게 호출
    LaunchedEffect(Unit) {
        incidentViewModel.fetchRealtimeWeather()
    }

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
        val threshold = with(density) { 120.dp.toPx() }
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
        return sqrt(dx * dx + dy * dy) <= with(density) { 80.dp.toPx() }
    }

    fun dropSceneIfPossible() {
        val mapRect = mapRectInWindow ?: return
        val mapObj = naverMapObj ?: return
        val dropPos = sceneDragWindowPos
        if (dropPos.x !in mapRect.left..mapRect.right || dropPos.y !in mapRect.top..mapRect.bottom) return

        val localX = (dropPos.x - mapRect.left).toFloat()
        val localY = (dropPos.y - mapRect.top).toFloat()
        val latLng = mapObj.projection.fromScreenLocation(PointF(localX, localY))

        markerState.position = latLng
        incidentViewModel.updateSceneLocationFromDrag(context, latLng)
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    Row(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(modifier = Modifier.weight(if (panelActive) 2f else 1f).fillMaxHeight().background(Color.Black)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coords -> mapRectInWindow = coords.boundsInWindow() }
                    .pointerInput(mapLoaded, incidentViewModel.placedVehicles, rightMode, isSectorMode, isMarkerLocked, isSceneMovable, isMeasuringDistance) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val longPressChange = awaitLongPressOrCancellation(down.id)

                            if (longPressChange == null) {
                                if (isSectorMode) {
                                    val payload = findNearestPlacedPayload(down.position)
                                    if (payload != null) {
                                        sectorTargetVehicleId = payload.id
                                        hapticArm()
                                    }
                                }
                                return@awaitEachGesture
                            }

                            if (isMeasuringDistance) return@awaitEachGesture

                            val nearScene = isNearSceneMarker(longPressChange.position)
                            val canDragMarkers = !isMarkerLocked && rightMode != RightPanelMode.BRIEFING

                            if (nearScene) {
                                if (isSceneMovable) {
                                    val mapRect = mapRectInWindow ?: return@awaitEachGesture
                                    hapticArm()
                                    sceneDragActive = true
                                    sceneDragWindowPos = Offset(mapRect.left + longPressChange.position.x, mapRect.top + longPressChange.position.y)
                                    while (true) {
                                        val event = awaitPointerEvent(pass = PointerEventPass.Main)
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (change.changedToUp()) {
                                            dropSceneIfPossible()
                                            sceneDragActive = false
                                            break
                                        }
                                        change.consumeAllChanges()
                                        val mR = mapRectInWindow ?: continue
                                        sceneDragWindowPos = Offset(mR.left + change.position.x, mR.top + change.position.y)
                                    }
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
                    onMapLoaded = { mapLoaded = true },
                    onMapClick = { _, clickedLatLng ->
                        if (isMeasuringDistance) {
                            measurePoints.add(clickedLatLng)
                            strongVibrate(context)
                        }
                    }
                ) {
                    MapEffect(Unit) { map -> naverMapObj = map }

                    if (measurePoints.isNotEmpty()) {
                        if (measurePoints.size >= 2) {
                            PathOverlay(
                                coords = measurePoints.toList(),
                                width = 4.dp,
                                color = MarsOrange,
                                outlineWidth = 1.dp,
                                outlineColor = Color.White
                            )
                        }

                        var totalDistance = 0.0
                        measurePoints.forEachIndexed { index, pt ->
                            if (index > 0) {
                                totalDistance += measurePoints[index - 1].distanceTo(pt)
                            }

                            val distText = if (index == 0) "시작" else if (totalDistance < 1000) "${totalDistance.roundToInt()}m" else String.format("%.2fkm", totalDistance / 1000.0)

                            Marker(
                                state = MarkerState(position = pt),
                                icon = MarkerIcons.BLACK,
                                iconTintColor = MarsOrange,
                                width = 22.dp,
                                height = 30.dp,
                                captionText = distText,
                                captionColor = Color.White,
                                captionHaloColor = MarsOrange,
                                captionTextSize = 18.sp,
                                zIndex = 200
                            )
                        }
                    }

                    if (lat != null && lng != null) {
                        val fireType = FireType.from(incident?.fireType)
                        val markerRes = MarkerIconMapper.markerResFor(fireType)
                        val dynamicSceneSize = (sceneIconBaseSize.value * zoomFactor).coerceAtLeast(1f).dp

                        if (!sceneDragActive) {
                            Marker(state = markerState, icon = OverlayImage.fromResource(markerRes), width = dynamicSceneSize, height = dynamicSceneSize, captionText = "현장", captionColor = Color.White, captionHaloColor = Color.Black)
                        }
                    }

                    incidentViewModel.placedVehicles.forEach { pv ->
                        if (dragState.active && dragState.payload?.id == pv.id) return@forEach

                        key(pv.id) {
                            val isCommand = pv.equipment.contains("지휘")
                            val st = rememberMarkerState(position = pv.position)
                            val iconRes = VehicleIconMapper.iconResForEquip(pv.equipment)
                            val label = VehicleIconMapper.deptLabel(pv.department)

                            val scale = if (isCommand) 5.0f else vehicleScaleFor(pv.equipment)
                            val baseSize = 26.dp
                            val markerHeight = (baseSize.value * scale * zoomFactor).coerceAtLeast(1f).dp
                            val markerWidth = if (pv.equipment.contains("탱크") || pv.equipment.contains("급수")) (markerHeight.value * 1.6f).dp else markerHeight

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
                        if (dragState.active && dragState.payload?.id == sectorTargetVehicleId) return@NaverMap

                        val targetVehicle = incidentViewModel.placedVehicles.find { it.id == sectorTargetVehicleId }
                        targetVehicle?.let { vehicle ->
                            val centerLat = vehicle.position.latitude
                            val centerLng = vehicle.position.longitude
                            val bearing = cameraPositionState.position.bearing
                            val pos10Rad = Math.toRadians(bearing - 45.0)
                            val pos4Rad = Math.toRadians(bearing + 135.0)
                            val gapMeters = 32.0
                            val latPerMeter = 1.0 / 111320.0
                            val cosLat = kotlin.math.cos(centerLat * Math.PI / 180.0)
                            val lngPerMeter = 1.0 / (111320.0 * cosLat)
                            val dLat10 = gapMeters * kotlin.math.cos(pos10Rad) * latPerMeter
                            val dLng10 = gapMeters * kotlin.math.sin(pos10Rad) * lngPerMeter
                            val dLat4 = gapMeters * kotlin.math.cos(pos4Rad) * latPerMeter
                            val dLng4 = gapMeters * kotlin.math.sin(pos4Rad) * lngPerMeter
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

                            Marker(
                                state = MarkerState(position = LatLng(centerLat + dLat10, centerLng + dLng10)),
                                icon = OverlayImage.fromResource(R.drawable.ic_turn_left),
                                width = arrowSize, height = arrowSize,
                                isFlat = false,
                                anchor = Offset(0.5f, 0.5f),
                                zIndex = 100,
                                onClick = { rotateMapBy(90.0); true }
                            )

                            Marker(
                                state = MarkerState(position = LatLng(centerLat + dLat4, centerLng + dLng4)),
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

                if (rightMode != RightPanelMode.BRIEFING) {
                    Column(modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 16.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TopBarButton(text = "차량편성/수정", onClick = onEdit)
                            TopBarButton(text = if (isSectorMode) "방면지휘 ON" else "방면지휘", isActive = isSectorMode, onClick = { isSectorMode = !isSectorMode; if (!isSectorMode) sectorTargetVehicleId = null })
                            TopBarButton(text = "브리핑모드", onClick = { rightMode = RightPanelMode.HUB })
                        }
                        TopBarButton(text = if (isMarkerLocked) "📌 마커 잠금됨" else "🔓 마커 이동 가능", isActive = isMarkerLocked, onClick = { isMarkerLocked = !isMarkerLocked })
                        TopBarButton(text = if (isSceneMovable) "📍 현장 변경 ON" else "📍 현장 변경", isActive = isSceneMovable, onClick = { isSceneMovable = !isSceneMovable })
                        TopBarButton(text = if (isMeasuringDistance) "📏 측정 종료" else "📏 거리측정", isActive = isMeasuringDistance, onClick = {
                            isMeasuringDistance = !isMeasuringDistance
                            if (!isMeasuringDistance) measurePoints.clear()
                        })
                    }
                }

                // ✅ 좌하단 실시간 기상 데이터 표출 (API 데이터 우선, 없으면 입력된 과거 데이터, 둘 다 없으면 기본 텍스트)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = if (showTray && !panelActive) 110.dp else 16.dp, start = 16.dp)
                        .background(Color(0xFF1C1C1C).copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    val sky = if (weatherData.sky != "-") weatherData.sky else incident?.meta?.기상_날씨?.takeIf { it.isNotBlank() && it != "-" } ?: "맑음"
                    val temp = if (weatherData.temp != "-") "${weatherData.temp}℃" else incident?.meta?.기상_기온?.takeIf { it.isNotBlank() && it != "-" } ?: "기온-"
                    val windDir = if (weatherData.windDirStr != "-") weatherData.windDirStr else incident?.meta?.기상_풍향?.takeIf { it.isNotBlank() && it != "-" } ?: "풍향-"
                    val windSpeed = if (weatherData.windSpeed != "-") "${weatherData.windSpeed}m/s" else incident?.meta?.기상_풍속?.takeIf { it.isNotBlank() && it != "-" } ?: "풍속-"

                    Text(
                        text = "$sky / $temp / $windDir / $windSpeed",
                        color = MarsOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth().align(Alignment.BottomEnd).padding(bottom = if (showTray) 90.dp else 16.dp, end = 16.dp), horizontalArrangement = Arrangement.End) {
                Button(onClick = { isSatellite = !isSatellite }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1C).copy(alpha = 0.8f))) { Text(if (isSatellite) "SAT" else "BASIC") }
                Spacer(Modifier.width(10.dp))
                Button(onClick = { incidentViewModel.clearIncident(); onExit() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1C).copy(alpha = 0.8f), contentColor = MarsOrange)) { Text("EXIT") }
            }

            if (!panelActive && showTray) {
                Column(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).background(Color.Black.copy(alpha = 0.55f)).padding(12.dp).navigationBarsPadding()) {
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        notPlaced.forEach { item ->
                            val isBeingDragged = dragState.active && dragState.payload?.id == item.id
                            key(item.id) {
                                TrayChipDraggableAfterLongPress(
                                    item = item,
                                    deptLabel = VehicleIconMapper.deptLabel(item.department),
                                    iconRes = VehicleIconMapper.iconResForEquip(item.equipment),
                                    onLift = { windowPos -> hapticArm(); dragState = DragState(active = true, payload = DragPayload(item.id, item.department, item.equipment), windowPos = windowPos, wobble = true) },
                                    onMove = { windowPos -> if (dragState.active && dragState.payload?.id == item.id) { dragState = dragState.copy(windowPos = windowPos, wobble = true) } },
                                    onDrop = { dropPayloadIfPossible(); dragState = DragState(active = false) },
                                    modifier = Modifier.padding(end = 8.dp).alpha(if (isBeingDragged) 0f else 1f)
                                )
                            }
                        }
                    }
                }
            }

            if (dragState.active && dragState.payload != null) {
                val payload = dragState.payload!!
                val isCommand = payload.equipment.contains("지휘")
                val iconRes = VehicleIconMapper.iconResForEquip(payload.equipment)
                val currentZoom = cameraPositionState.position.zoom
                val zoomFactor = 2.0.pow(currentZoom - 17.5).toFloat()
                val scale = if (isCommand) 5.0f else vehicleScaleFor(payload.equipment)
                val baseSize = 26.dp
                val markerHeight = (baseSize.value * scale * zoomFactor * 1.5f).coerceAtLeast(40f).dp
                val markerWidth = if (payload.equipment.contains("탱크") || payload.equipment.contains("급수")) (markerHeight.value * 1.6f).dp else markerHeight
                val sizePxX = with(density) { markerWidth.toPx() }
                val sizePxY = with(density) { markerHeight.toPx() }
                val halfPxX = (sizePxX / 2).roundToInt()
                val halfPxY = (sizePxY / 2).roundToInt()

                Box(modifier = Modifier.offset { IntOffset(dragState.windowPos.x.roundToInt() - halfPxX, dragState.windowPos.y.roundToInt() - halfPxY) }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(painter = painterResource(iconRes), contentDescription = null, modifier = Modifier.width(markerWidth).height(markerHeight).alpha(0.75f))
                        Text(text = VehicleIconMapper.deptLabel(payload.department), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
            }

            if (sceneDragActive) {
                val fireType = FireType.from(incident?.fireType)
                val markerRes = MarkerIconMapper.markerResFor(fireType)
                val sizeDp = 100.dp
                val sizePx = with(density) { sizeDp.toPx() }
                val halfPx = (sizePx / 2).roundToInt()
                Box(modifier = Modifier.offset { IntOffset(sceneDragWindowPos.x.roundToInt() - halfPx, sceneDragWindowPos.y.roundToInt() - halfPx) }) {
                    Image(painter = painterResource(markerRes), contentDescription = null, modifier = Modifier.size(sizeDp).alpha(0.75f))
                    Text("현장 이동 중", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.align(Alignment.BottomCenter).offset(y = 20.dp).background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(4.dp)).padding(4.dp))
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
private fun BriefingTile(title: String, value: String, modifier: Modifier = Modifier, valueColor: Color = TextPrimary) {
    Column(modifier = modifier.fillMaxWidth().background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp)).border(1.dp, BorderGray, RoundedCornerShape(8.dp)).padding(12.dp)) {
        Text(text = title, color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = value.ifBlank { "-" }, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
    val weatherData by incidentViewModel.weatherData.collectAsState()
    val meta = incident?.meta ?: IncidentMeta()
    val placed = incidentViewModel.getPlacedCount()

    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("현장 브리핑", color = MarsOrange, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(Modifier.weight(1f))
            TopBarButton(text = "허브", onClick = onBackToHub)
            Spacer(Modifier.width(8.dp))
            TopBarButton(text = "닫기", onClick = onClose)
        }
        Spacer(Modifier.height(24.dp))

        // ✅ 브리핑 패널도 실시간 기상 데이터 반영 (찌꺼기 처리)
        val sky = if (weatherData.sky != "-") weatherData.sky else meta.기상_날씨.takeIf { it.isNotBlank() && it != "-" } ?: "맑음"
        val temp = if (weatherData.temp != "-") "${weatherData.temp}℃" else meta.기상_기온.takeIf { it.isNotBlank() && it != "-" }?.let { "${it}℃" } ?: "-"
        val windDir = if (weatherData.windDirStr != "-") weatherData.windDirStr else meta.기상_풍향.takeIf { it.isNotBlank() && it != "-" } ?: "-"
        val windSpeed = if (weatherData.windSpeed != "-") "${weatherData.windSpeed}m/s" else meta.기상_풍속.takeIf { it.isNotBlank() && it != "-" }?.let { "${it}m/s" } ?: "-"

        Text("관측 기상 현황", color = MarsOrange, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BriefingTile("날씨", sky, Modifier.weight(1f))
            BriefingTile("기온", temp, Modifier.weight(1f))
            BriefingTile("풍향/풍속", "$windDir $windSpeed", Modifier.weight(1.5f))
        }
        Spacer(Modifier.height(20.dp))

        Text("재난 발생 개요", color = MarsOrange, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        BriefingTile("발생 위치", incident?.address ?: "-", Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BriefingTile("처종", incident?.fireType ?: "-", Modifier.weight(1f))
            BriefingTile("대응 단계", incident?.대응단계 ?: "-", Modifier.weight(1f), valueColor = if(incident?.대응단계?.contains("단계") == true) NeonRed else TextPrimary)
        }
        Spacer(Modifier.height(8.dp))
        BriefingTile("화재 원인 추정", incident?.화재원인 ?: "-", Modifier.fillMaxWidth())
        Spacer(Modifier.height(20.dp))

        Text("주요 시간대별 현황", color = MarsOrange, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BriefingTile("신고 접수", incident?.신고접수일시 ?: "-", Modifier.weight(1f))
            BriefingTile("선착대 도착", incident?.선착대도착시간 ?: "-", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BriefingTile("초진 시간", incident?.초진시간 ?: "-", Modifier.weight(1f))
            BriefingTile("완진 시간", incident?.완진시간 ?: "-", Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))

        Text("주요 피해 현황", color = MarsOrange, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BriefingTile("인명 피해", incident?.인명피해현황 ?: "-", Modifier.weight(1f), valueColor = NeonRed)
            BriefingTile("재산 피해", incident?.재산피해현황 ?: "-", Modifier.weight(1f), valueColor = NeonRed)
            BriefingTile("대원 피해", incident?.대원피해현황 ?: "-", Modifier.weight(1f), valueColor = NeonOrange)
        }
        Spacer(Modifier.height(20.dp))

        Text("동원 소방력 현황", color = MarsOrange, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BriefingTile("소방력 인원", if (incident?.소방력_인원?.isNotBlank() == true) "${incident?.소방력_인원}명" else "-", Modifier.weight(1f))
            BriefingTile("차량(실제배치)", "${placed}대", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))

        val relatedAgencies = listOf(
            "경찰" to incident?.유관기관_경찰,
            "시청" to incident?.유관기관_시청,
            "한전" to incident?.유관기관_한전,
            "도시가스" to incident?.유관기관_도시가스,
            "산불진화대" to incident?.유관기관_산불진화대_화성시
        ).filter { !it.second.isNullOrBlank() }

        if(relatedAgencies.isNotEmpty()) {
            val agenciesText = relatedAgencies.joinToString(" / ") { "${it.first}: ${it.second}" }
            BriefingTile("유관기관 지원", agenciesText, Modifier.fillMaxWidth())
        } else {
            BriefingTile("유관기관 지원", "해당사항 없음", Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(40.dp))
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
    Box(modifier = modifier.onGloballyPositioned { chipPos = it.boundsInWindow().topLeft }.pointerInput(item.id) {
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