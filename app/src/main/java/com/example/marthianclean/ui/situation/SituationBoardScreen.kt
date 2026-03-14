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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.marthianclean.R
import com.example.marthianclean.model.FireType
import com.example.marthianclean.model.MarkerIconMapper
import com.example.marthianclean.ui.sticker.VehicleIconMapper
import com.example.marthianclean.model.IncidentMeta
import com.example.marthianclean.viewmodel.DisasterMode
import com.example.marthianclean.viewmodel.IncidentViewModel
import com.example.marthianclean.viewmodel.SearchStatus
import com.example.marthianclean.util.AreaCalculator
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
import com.example.marthianclean.util.HydrantManager
import com.example.marthianclean.model.Hydrant

private val MarsOrange = Color(0xFFFF8C00)
private val CommandYellowGreen = Color(0xFFD4FF00)
private val TextPrimary = Color(0xFFF0F0F0)
private val BgBlack = Color(0xFF0E0E0E)
private val BorderGray = Color(0xFF2E2E2E)
private val NeonRed = Color(0xFFFF1744)
private val NeonOrange = Color(0xFFFF9100)
private val WaterCyan = Color(0xFF00E5FF)

private enum class RightPanelMode { NONE, HUB, BRIEFING, FORCE_STATUS }

private data class DragPayload(val id: String, val department: String, val equipment: String)

private data class DragState(
    val active: Boolean = false,
    val payload: DragPayload? = null,
    val windowPos: Offset = Offset.Zero,
    val wobble: Boolean = false
)

private fun vehicleScaleFor(equipment: String): Float {
    val e = equipment.trim().lowercase()
    return when {
        e.contains("배연") -> 6.0f
        e.contains("내폭") -> 5.0f
        e.contains("조명") -> 7.0f
        e.contains("무인") || e.contains("방수") || e.contains("파괴") -> 6.24f
        e.contains("고가") || e.contains("사다리") || e.contains("ladder") -> 4.8f
        e.contains("굴절") -> 4.8f
        e.contains("회복") || e.contains("버스") -> 4.8f
        e.contains("화학") -> 4.0f
        e.contains("포크") || e.contains("굴삭") -> 3.2f
        e.contains("구조") || e.contains("장비운반") || e.contains("생활") || e.contains("안전지원") -> 3.0f
        e.contains("탱크") || e.contains("급수") -> 2.66f
        e.contains("펌프") -> 2.5f
        e.contains("지휘") -> 2.1f
        e.contains("구급") -> 2.0f
        else -> 1.5f
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SituationBoardScreen(
    incidentViewModel: IncidentViewModel,
    onEdit: () -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current

    // ✅ 화성시 소화전 데이터 최초 1회 로드
    val hydrantList = remember { HydrantManager.loadHwaseongHydrants(context) }

    val incident by incidentViewModel.incident.collectAsState()
    val weatherData by incidentViewModel.weatherData.collectAsState()

    val currentMode by incidentViewModel.currentMode.collectAsState()
    val isBriefingLocked = incidentViewModel.isBriefingLocked
    val apartmentData by incidentViewModel.apartmentData.collectAsState()
    val waterData by incidentViewModel.waterData.collectAsState()

    var rightMode by remember { mutableStateOf(RightPanelMode.NONE) }
    var isSectorMode by remember { mutableStateOf(false) }
    var sectorTargetVehicleId by remember { mutableStateOf<String?>(null) }
    var isMarkerLocked by remember { mutableStateOf(false) }
    var isSceneMovable by remember { mutableStateOf(false) }

    var isMeasuringDistance by remember { mutableStateOf(false) }
    val measurePoints = remember { mutableStateListOf<LatLng>() }

    var isMeasuringArea by remember { mutableStateOf(false) }
    var areaPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var isAreaCompleted by remember { mutableStateOf(false) }
    val areaResultText = if (isAreaCompleted) AreaCalculator.getFormattedArea(areaPoints) else "측정 대기 중"

    var waterTargetLatLng by remember { mutableStateOf<LatLng?>(null) }
    var isDrawingSearchZone by remember { mutableStateOf(false) }
    var currentSearchDay by remember { mutableIntStateOf(1) }
    var drawingCenter by remember { mutableStateOf<LatLng?>(null) }
    var drawingRadius by remember { mutableDoubleStateOf(0.0) }

    var searchZoneClickCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(searchZoneClickCount) {
        if (searchZoneClickCount > 0) {
            delay(350)
            if (searchZoneClickCount == 1) {
                isDrawingSearchZone = !isDrawingSearchZone
            }
            searchZoneClickCount = 0
        }
    }

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

    LaunchedEffect(Unit) { incidentViewModel.fetchRealtimeWeather() }

    LaunchedEffect(lat, lng, mapLoaded) {
        if (!didInitialCam && mapLoaded && lat != null && lng != null) {
            didInitialCam = true
            val pos = LatLng(lat, lng)
            markerState.position = pos
            cameraPositionState.animate(update = CameraUpdate.scrollTo(pos), durationMs = 700)
            cameraPositionState.animate(update = CameraUpdate.zoomTo(17.5), durationMs = 320)
        }
    }

    fun hapticArm() { strongVibrate(context); view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }

    fun dropPayloadIfPossible() {
        if (isBriefingLocked) return
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
            if (dist < bestDist) { bestDist = dist; best = DragPayload(pv.id, pv.department, pv.equipment) }
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
        if (isBriefingLocked) return
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

            val currentZoom = cameraPositionState.position.zoom
            val zoomFactor = 2.0.pow(currentZoom - 17.5).toFloat()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coords -> mapRectInWindow = coords.boundsInWindow() }
                    .pointerInput(mapLoaded, incidentViewModel.placedVehicles, rightMode, isSectorMode, isMarkerLocked, isSceneMovable, isMeasuringDistance, isMeasuringArea, isBriefingLocked, isDrawingSearchZone) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val mapObj = naverMapObj
                            val mapRect = mapRectInWindow

                            if (isDrawingSearchZone && !isBriefingLocked && mapObj != null && mapRect != null) {
                                hapticArm()
                                val startLocalX = (down.position.x).toFloat()
                                val startLocalY = (down.position.y).toFloat()
                                val startLatLng = mapObj.projection.fromScreenLocation(PointF(startLocalX, startLocalY))
                                drawingCenter = startLatLng
                                drawingRadius = 0.0

                                while (true) {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Main)
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                    if (change.changedToUp()) {
                                        if (drawingRadius > 5.0) {
                                            incidentViewModel.addWaterSearchZone(currentSearchDay, drawingCenter!!, drawingRadius)
                                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        }
                                        drawingCenter = null
                                        drawingRadius = 0.0
                                        break
                                    }
                                    change.consumeAllChanges()
                                    val currentLocalX = (change.position.x).toFloat()
                                    val currentLocalY = (change.position.y).toFloat()
                                    val currentLatLng = mapObj.projection.fromScreenLocation(PointF(currentLocalX, currentLocalY))
                                    drawingRadius = startLatLng.distanceTo(currentLatLng)
                                }
                                return@awaitEachGesture
                            }

                            val longPressChange = awaitLongPressOrCancellation(down.id)

                            if (longPressChange == null) {
                                if (isSectorMode && !isBriefingLocked) {
                                    val payload = findNearestPlacedPayload(down.position)
                                    if (payload != null) { sectorTargetVehicleId = payload.id; hapticArm() }
                                }
                                return@awaitEachGesture
                            }

                            if (isMeasuringDistance || isMeasuringArea || isBriefingLocked) return@awaitEachGesture

                            val nearScene = isNearSceneMarker(longPressChange.position)
                            val canDragMarkers = !isMarkerLocked && rightMode != RightPanelMode.BRIEFING

                            if (nearScene) {
                                if (isSceneMovable) {
                                    val mapRect2 = mapRectInWindow ?: return@awaitEachGesture
                                    hapticArm()
                                    sceneDragActive = true
                                    sceneDragWindowPos = Offset(mapRect2.left + longPressChange.position.x, mapRect2.top + longPressChange.position.y)
                                    while (true) {
                                        val event = awaitPointerEvent(pass = PointerEventPass.Main)
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (change.changedToUp()) { dropSceneIfPossible(); sceneDragActive = false; break }
                                        change.consumeAllChanges()
                                        val mR = mapRectInWindow ?: continue
                                        sceneDragWindowPos = Offset(mR.left + change.position.x, mR.top + change.position.y)
                                    }
                                }
                                return@awaitEachGesture
                            }

                            if (canDragMarkers) {
                                val payload = findNearestPlacedPayload(longPressChange.position) ?: return@awaitEachGesture
                                val mapRect2 = mapRectInWindow ?: return@awaitEachGesture
                                hapticArm()
                                dragState = DragState(active = true, payload = payload, windowPos = Offset(mapRect2.left + longPressChange.position.x, mapRect2.top + longPressChange.position.y), wobble = true)
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
                NaverMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(mapType = if (isSatellite) MapType.Satellite else MapType.Basic),
                    uiSettings = MapUiSettings(isZoomControlEnabled = false, isCompassEnabled = false, isLocationButtonEnabled = false),
                    onMapLoaded = { mapLoaded = true },
                    onMapClick = { _, clickedLatLng ->
                        if (!isBriefingLocked && !isDrawingSearchZone) {
                            if (isMeasuringDistance) { measurePoints.add(clickedLatLng); strongVibrate(context) }
                            if (isMeasuringArea && !isAreaCompleted) { areaPoints = areaPoints.toList() + clickedLatLng; strongVibrate(context) }
                            if (currentMode == DisasterMode.WATER) { waterTargetLatLng = clickedLatLng; strongVibrate(context) }
                        }
                    }
                ) {
                    MapEffect(Unit) { map -> naverMapObj = map }

// ✅ [수정된 부분] 소화전 마커 실시간 필터링 렌더링
                    val currentBounds = cameraPositionState.contentBounds // 현재 화면의 사각 영역

// 줌 레벨이 14.0 이상(약 2km 반경 시야)이고 화면 영역 정보가 있을 때만 계산
                    if (currentZoom >= 14.0 && currentBounds != null) {
                        hydrantList
                            .filter { hydrant ->
                                // 1. 현재 화면 영역 안에 좌표가 포함되는 데이터만 추출
                                currentBounds.contains(LatLng(hydrant.lat, hydrant.lng))
                            }
                            .forEach { hydrant ->
                                Marker(
                                    state = MarkerState(position = LatLng(hydrant.lat, hydrant.lng)),
                                    icon = OverlayImage.fromResource(R.drawable.ic_hydrant),
                                    iconTintColor = if (hydrant.type == "1") NeonRed else WaterCyan,
                                    width = 36.dp,
                                    height = 36.dp,
                                    zIndex = 5 // 차량 마커(zIndex 10)보다 아래에 배치
                                )
                            }
                    }
                    if (currentMode == DisasterMode.WATER) {
                        waterData.commandPost?.let { cp ->
                            Marker(
                                state = MarkerState(position = cp),
                                icon = OverlayImage.fromResource(R.drawable.ic_water_command_post),
                                width = 40.dp, height = 40.dp,
                                captionText = "지휘소(CP)", captionColor = Color.White, captionHaloColor = Color.Black, captionTextSize = 16.sp, zIndex = 150
                            )
                        }

                        val maxDay = waterData.searchZones.maxOfOrNull { it.day } ?: 1

                        waterData.searchZones.forEach { zone ->
                            val circleColor = when {
                                zone.day == maxDay -> MarsOrange
                                zone.day == maxDay - 1 -> CommandYellowGreen
                                else -> WaterCyan
                            }

                            CircleOverlay(
                                center = zone.center,
                                radius = zone.radiusMeter,
                                color = circleColor.copy(alpha = 0.15f),
                                outlineWidth = 2.dp,
                                outlineColor = circleColor
                            )
                            Marker(
                                state = MarkerState(position = zone.center),
                                icon = MarkerIcons.BLACK,
                                iconTintColor = circleColor,
                                width = 14.dp, height = 14.dp,
                                anchor = Offset(0.5f, 0.5f),
                                captionText = "${zone.day}일차 수색반경\n(${zone.radiusMeter.toInt()}m)",
                                captionColor = circleColor,
                                captionHaloColor = Color.Black
                            )
                        }

                        if (drawingCenter != null && drawingRadius > 0) {
                            CircleOverlay(center = drawingCenter!!, radius = drawingRadius, color = NeonRed.copy(alpha = 0.2f), outlineWidth = 2.dp, outlineColor = NeonRed)
                            Marker(state = MarkerState(position = drawingCenter!!), icon = MarkerIcons.BLACK, iconTintColor = NeonRed, width = 14.dp, height = 14.dp, anchor = Offset(0.5f, 0.5f), captionText = "${currentSearchDay}일차\n${drawingRadius.toInt()}m", captionColor = NeonRed, captionHaloColor = Color.Black)
                        }

                        waterTargetLatLng?.let { target ->
                            if(!isDrawingSearchZone) {
                                Marker(state = MarkerState(position = target), icon = MarkerIcons.BLACK, iconTintColor = Color.Red, width = 20.dp, height = 20.dp, captionText = "목표 위치", captionColor = Color.Red, captionHaloColor = Color.Black)
                            }
                        }
                    }

                    if (measurePoints.isNotEmpty()) {
                        if (measurePoints.size >= 2) {
                            PathOverlay(coords = measurePoints.toList(), width = 4.dp, color = MarsOrange, outlineWidth = 1.dp, outlineColor = Color.White)
                        }
                        var totalDistance = 0.0
                        measurePoints.forEachIndexed { index, pt ->
                            if (index > 0) totalDistance += measurePoints[index - 1].distanceTo(pt)
                            val distText = if (index == 0) "시작" else {
                                val d = if (totalDistance < 1000) "${totalDistance.roundToInt()}m" else String.format("%.2fkm", totalDistance / 1000.0)
                                val hoseCount = kotlin.math.ceil(totalDistance / 15.0).toInt()
                                "$d\n${hoseCount}벌"
                            }
                            Marker(state = MarkerState(position = pt), icon = MarkerIcons.BLACK, iconTintColor = MarsOrange, width = 22.dp, height = 30.dp, captionText = distText, captionColor = Color.White, captionHaloColor = MarsOrange, captionTextSize = 16.sp, zIndex = 200)
                        }
                    }

                    if (areaPoints.isNotEmpty()) {
                        if (areaPoints.size >= 2 && !isAreaCompleted) {
                            PathOverlay(coords = areaPoints.toList(), width = 3.dp, color = Color.Yellow, outlineWidth = 1.dp, outlineColor = Color.Black)
                        }
                        if (isAreaCompleted && areaPoints.size >= 3) {
                            PolygonOverlay(coords = areaPoints.toList(), color = Color(0x66FF1744), outlineWidth = 2.dp, outlineColor = Color.Red)
                        }
                        areaPoints.forEach { pt ->
                            Marker(state = MarkerState(position = pt), icon = MarkerIcons.BLACK, iconTintColor = if (isAreaCompleted) Color.Red else Color.Yellow, width = 12.dp, height = 12.dp, anchor = Offset(0.5f, 0.5f), zIndex = 210)
                        }
                    }

                    if (lat != null && lng != null) {
                        val fireType = FireType.from(incident?.fireType)
                        val markerRes = MarkerIconMapper.markerResFor(fireType)
                        val dynamicSceneSize = (sceneIconBaseSize.value * zoomFactor).coerceAtLeast(1f).dp
                        if (!sceneDragActive) Marker(state = markerState, icon = OverlayImage.fromResource(markerRes), width = dynamicSceneSize, height = dynamicSceneSize, captionText = "현장", captionColor = Color.White, captionHaloColor = Color.Black)
                    }

                    incidentViewModel.placedVehicles.forEach { pv ->
                        if (dragState.active && dragState.payload?.id == pv.id) return@forEach
                        key(pv.id) {
                            val isCommand = pv.equipment.contains("지휘")
                            val st = rememberMarkerState(position = pv.position)
                            val iconRes = VehicleIconMapper.iconResForEquip(pv.equipment)

                            val hqName = incidentViewModel.selectedStationName.ifBlank { "관할" }
                            val label = VehicleIconMapper.customVehicleLabel(callSign = "", stationName = hqName, department = pv.department, equipment = pv.equipment)

                            val scale = if (isCommand) 5.0f else vehicleScaleFor(pv.equipment)
                            val baseSize = 26.dp
                            val markerHeight = (baseSize.value * scale * zoomFactor).coerceAtLeast(1f).dp
                            val markerWidth = if (pv.equipment.contains("탱크") || pv.equipment.contains("급수")) (markerHeight.value * 1.6f).dp else markerHeight

                            Marker(
                                state = st, icon = OverlayImage.fromResource(iconRes),
                                width = markerWidth, height = markerHeight,
                                captionText = label, captionColor = if (isCommand) CommandYellowGreen else Color.White, captionHaloColor = Color.Black,
                                anchor = Offset(0.5f, 0.5f), zIndex = if (isCommand) 10 else 1
                            )
                        }
                    }

                    if (isSectorMode && sectorTargetVehicleId != null) {
                        if (dragState.active && dragState.payload?.id == sectorTargetVehicleId) return@NaverMap
                        val targetVehicle = incidentViewModel.placedVehicles.find { it.id == sectorTargetVehicleId }
                        targetVehicle?.let { vehicle ->
                            val centerLat = vehicle.position.latitude; val centerLng = vehicle.position.longitude
                            val bearing = cameraPositionState.position.bearing
                            val gapMeters = 32.0; val latPerMeter = 1.0 / 111320.0; val lngPerMeter = 1.0 / (111320.0 * kotlin.math.cos(centerLat * Math.PI / 180.0))
                            val dLat10 = gapMeters * kotlin.math.cos(Math.toRadians(bearing - 45.0)) * latPerMeter; val dLng10 = gapMeters * kotlin.math.sin(Math.toRadians(bearing - 45.0)) * lngPerMeter
                            val dLat4 = gapMeters * kotlin.math.cos(Math.toRadians(bearing + 135.0)) * latPerMeter; val dLng4 = gapMeters * kotlin.math.sin(Math.toRadians(bearing + 135.0)) * lngPerMeter
                            val arrowSize = (70f * zoomFactor).coerceAtLeast(30f).dp

                            fun rotateMapBy(degrees: Double) {
                                coroutineScope.launch { cameraPositionState.animate(update = CameraUpdate.toCameraPosition(com.naver.maps.map.CameraPosition(vehicle.position, cameraPositionState.position.zoom, 0.0, (cameraPositionState.position.bearing + degrees + 360.0) % 360.0)), animation = CameraAnimation.Easing, durationMs = 500) }
                            }
                            Marker(state = MarkerState(position = LatLng(centerLat + dLat10, centerLng + dLng10)), icon = OverlayImage.fromResource(R.drawable.ic_turn_left), width = arrowSize, height = arrowSize, isFlat = false, anchor = Offset(0.5f, 0.5f), zIndex = 100, onClick = { rotateMapBy(90.0); true })
                            Marker(state = MarkerState(position = LatLng(centerLat + dLat4, centerLng + dLng4)), icon = OverlayImage.fromResource(R.drawable.ic_turn_right), width = arrowSize, height = arrowSize, isFlat = false, anchor = Offset(0.5f, 0.5f), zIndex = 100, onClick = { rotateMapBy(-90.0); true })
                        }
                    }
                }
            }

            if (currentMode == DisasterMode.APARTMENT) {
                Box(modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp).width(300.dp).fillMaxHeight(0.7f).background(Color(0xEE1C1C1C), RoundedCornerShape(12.dp)).border(2.dp, BorderGray, RoundedCornerShape(12.dp)).padding(16.dp)) {
                    if (apartmentData.totalFloors == 0) {
                        ApartmentSetupPanel(onSetup = { f, l -> incidentViewModel.setupApartmentGrid(f, l) })
                    } else {
                        ApartmentGrid(apartmentData, incidentViewModel)
                    }
                }
            }

            if (currentMode == DisasterMode.WATER && !isBriefingLocked) {
                Box(modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)) {
                    WaterRescueOverviewPanel(context = context)
                }
            }

            if (mapLoaded) {
                val bearing = cameraPositionState.position.bearing.toFloat()
                Box(
                    modifier = Modifier.align(Alignment.TopStart).padding(top = 20.dp, start = 20.dp).size(64.dp)
                        .background(Color(0xFF1C1C1C).copy(alpha = 0.85f), CircleShape).border(1.5.dp, BorderGray, CircleShape)
                        .clickable { coroutineScope.launch { cameraPositionState.animate(CameraUpdate.toCameraPosition(com.naver.maps.map.CameraPosition(cameraPositionState.position.target, cameraPositionState.position.zoom, 0.0, 0.0))) } },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                        modifier = Modifier.graphicsLayer { rotationZ = -bearing }.size(54.dp).padding(2.dp)
                    ) {
                        Text("N", color = Color(0xFFFF1744), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        Box(Modifier.width(3.dp).height(12.dp).background(Color(0xFFFF1744)))
                        Box(Modifier.width(3.dp).height(12.dp).background(Color.White))
                        Text("S", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }

                if (areaPoints.isNotEmpty()) {
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(top = 100.dp, start = 20.dp).background(Color(0xCC000000), RoundedCornerShape(8.dp)).border(1.dp, MarsOrange, RoundedCornerShape(8.dp)).padding(12.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("면적 산출(추정)", color = MarsOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(text = areaResultText, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.background(if (areaPoints.size >= 3 && !isAreaCompleted) MarsOrange else Color.DarkGray, RoundedCornerShape(4.dp)).clickable(enabled = areaPoints.size >= 3 && !isAreaCompleted) { isAreaCompleted = true; view.performHapticFeedback(HapticFeedbackConstants.CONFIRM) }.padding(horizontal = 12.dp, vertical = 6.dp)) { Text("측정", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                Box(modifier = Modifier.background(Color(0xFF333333), RoundedCornerShape(4.dp)).clickable { areaPoints = emptyList(); isAreaCompleted = false }.padding(horizontal = 12.dp, vertical = 6.dp)) { Text("초기화", color = Color.LightGray, fontSize = 12.sp) }
                            }
                        }
                    }
                }

                if (rightMode != RightPanelMode.BRIEFING) {
                    Column(modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 16.dp), horizontalAlignment = Alignment.End) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TopBarButton(text = "공동주택", isActive = currentMode == DisasterMode.APARTMENT, onClick = {
                                incidentViewModel.setDisasterMode(if (currentMode == DisasterMode.APARTMENT) DisasterMode.NORMAL else DisasterMode.APARTMENT)
                            })
                            TopBarButton(text = "수난구조", isActive = currentMode == DisasterMode.WATER, onClick = {
                                incidentViewModel.setDisasterMode(if (currentMode == DisasterMode.WATER) DisasterMode.NORMAL else DisasterMode.WATER)
                            })
                            TopBarButton(text = "차량편성/수정", onClick = onEdit)
                            TopBarButton(text = if (isSectorMode) "방면지휘 ON" else "방면지휘", isActive = isSectorMode, onClick = { isSectorMode = !isSectorMode; if (!isSectorMode) sectorTargetVehicleId = null })
                            TopBarButton(text = "브리핑모드", onClick = { rightMode = RightPanelMode.HUB })
                        }

                        Spacer(Modifier.height(10.dp))
                        TopBarButton(text = if (isMarkerLocked) "📌 마커 잠금됨" else "🔓 마커 이동 가능", isActive = isMarkerLocked, onClick = { isMarkerLocked = !isMarkerLocked })
                        Spacer(Modifier.height(10.dp))
                        TopBarButton(text = if (isSceneMovable) "📍 현장 변경 ON" else "📍 현장 변경", isActive = isSceneMovable, onClick = { isSceneMovable = !isSceneMovable })
                        Spacer(Modifier.height(10.dp))
                        TopBarButton(text = if (isMeasuringDistance) "📏 측정 종료" else "📏 거리측정(65mm)", isActive = isMeasuringDistance, onClick = { isMeasuringDistance = !isMeasuringDistance; if (!isMeasuringDistance) measurePoints.clear(); if (isMeasuringDistance) { isMeasuringArea = false; areaPoints = emptyList(); isAreaCompleted = false } })
                        Spacer(Modifier.height(10.dp))
                        TopBarButton(text = if (isMeasuringArea) "📐 면적 종료" else "📐 면적측정", isActive = isMeasuringArea, onClick = { isMeasuringArea = !isMeasuringArea; if (!isMeasuringArea) { areaPoints = emptyList(); isAreaCompleted = false }; if (isMeasuringArea) { isMeasuringDistance = false; measurePoints.clear() } })

                        if (currentMode == DisasterMode.WATER && !isBriefingLocked) {
                            Spacer(Modifier.height(16.dp))
                            Text("⚓ 수난구조 통제", color = WaterCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                            TopBarButton(text = "📍 지휘소(CP) 지정", isActive = true, onClick = { waterTargetLatLng?.let { incidentViewModel.setWaterCommandPost(it) } })
                            Spacer(Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .background(if (isDrawingSearchZone) NeonRed else Color(0xFF1C1C1C).copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                    .border(1.dp, if (isDrawingSearchZone) Color.White else BorderGray, RoundedCornerShape(8.dp))
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = {
                                                strongVibrate(context)
                                                currentSearchDay = if (currentSearchDay < 5) currentSearchDay + 1 else 1
                                                searchZoneClickCount = 0
                                            },
                                            onTap = {
                                                searchZoneClickCount++
                                                if (searchZoneClickCount >= 3) {
                                                    strongVibrate(context)
                                                    currentSearchDay = if (currentSearchDay > 1) currentSearchDay - 1 else 5
                                                    searchZoneClickCount = 0
                                                }
                                            }
                                        )
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                val modeText = if (isDrawingSearchZone) "⭕ 그리기 켜짐 ($currentSearchDay 일차)" else "⭕ 수색반경 ($currentSearchDay 일차)"
                                Text(text = modeText, color = if (isDrawingSearchZone) Color.White else WaterCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            if (waterData.searchZones.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                TopBarButton(text = "↩ 최근 반경 취소", isActive = false, onClick = { incidentViewModel.removeLastWaterSearchZone() })
                                Spacer(Modifier.height(10.dp))
                                TopBarButton(text = "초기화", isActive = false, onClick = { incidentViewModel.clearWaterSearchZones() })
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = if (showTray && !panelActive) 110.dp else 16.dp, start = 16.dp)
                        .background(Color(0xFF1C1C1C).copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
                        .clickable {
                            strongVibrate(context)
                            incidentViewModel.fetchRealtimeWeather()
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    val sky = if (weatherData.sky != "-") weatherData.sky else incident?.meta?.기상_날씨?.takeIf { it.isNotBlank() && it != "-" } ?: "맑음"
                    val temp = if (weatherData.temp != "-") "${weatherData.temp}℃" else incident?.meta?.기상_기온?.takeIf { it.isNotBlank() && it != "-" } ?: "기온-"
                    val windDir = if (weatherData.windDirStr != "-") weatherData.windDirStr else incident?.meta?.기상_풍향?.takeIf { it.isNotBlank() && it != "-" } ?: "풍향-"
                    val windSpeed = if (weatherData.windSpeed != "-") "${weatherData.windSpeed}m/s" else incident?.meta?.기상_풍속?.takeIf { it.isNotBlank() && it != "-" } ?: "풍속-"

                    Text(text = "$sky / $temp / $windDir / $windSpeed ↻", color = MarsOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                                val hqName = incidentViewModel.selectedStationName.ifBlank { "관할" }
                                TrayChipDraggableAfterLongPress(
                                    item = item,
                                    deptLabel = VehicleIconMapper.customVehicleLabel("", hqName, item.department, item.equipment),
                                    iconRes = VehicleIconMapper.iconResForEquip(item.equipment),
                                    onLift = { windowPos -> if(!isBriefingLocked) { hapticArm(); dragState = DragState(active = true, payload = DragPayload(item.id, item.department, item.equipment), windowPos = windowPos, wobble = true) } },
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
                val hqName = incidentViewModel.selectedStationName.ifBlank { "관할" }
                val scale = vehicleScaleFor(payload.equipment)
                val mHeight = (26.dp.value * scale * zoomFactor * 1.5f).coerceAtLeast(40f).dp
                val offsetHalf = (mHeight.value / 2).toInt()

                Box(modifier = Modifier.offset { IntOffset(dragState.windowPos.x.roundToInt() - offsetHalf, dragState.windowPos.y.roundToInt() - offsetHalf) }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(painterResource(VehicleIconMapper.iconResForEquip(payload.equipment)), null, Modifier.size(mHeight).alpha(0.75f))
                        Text(
                            text = VehicleIconMapper.customVehicleLabel("", hqName, payload.department, payload.equipment),
                            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (sceneDragActive) {
                val fireType = FireType.from(incident?.fireType)
                val markerRes = MarkerIconMapper.markerResFor(fireType)
                val sizeDp = 100.dp
                val halfPx = with(density) { (sizeDp.toPx() / 2).roundToInt() }
                Box(modifier = Modifier.offset { IntOffset(sceneDragWindowPos.x.roundToInt() - halfPx, sceneDragWindowPos.y.roundToInt() - halfPx) }) {
                    Image(painterResource(markerRes), null, Modifier.size(sizeDp).alpha(0.75f))
                    Text("현장 이동 중", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.align(Alignment.BottomCenter).offset(y = 20.dp).background(Color.Red.copy(0.8f), RoundedCornerShape(4.dp)).padding(4.dp))
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
fun CustomWheelDateTimePicker(
    initialDateTime: String,
    onDateTimeSelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val parts = initialDateTime.split(" ", "-", ":")
    val initialYear = parts.getOrNull(0)?.toIntOrNull() ?: 2026
    val initialMonth = parts.getOrNull(1)?.toIntOrNull() ?: 1
    val initialDay = parts.getOrNull(2)?.toIntOrNull() ?: 1
    val initialHour = parts.getOrNull(3)?.toIntOrNull() ?: 12
    val initialMinute = parts.getOrNull(4)?.toIntOrNull() ?: 0

    val years = (2020..2030).toList()
    val months = (1..12).toList()
    val days = (1..31).toList()
    val hours = (0..23).toList()
    val minutes = (0..59).toList()

    val yearState = rememberLazyListState(initialFirstVisibleItemIndex = years.indexOf(initialYear).coerceAtLeast(0))
    val monthState = rememberLazyListState(initialFirstVisibleItemIndex = months.indexOf(initialMonth).coerceAtLeast(0))
    val dayState = rememberLazyListState(initialFirstVisibleItemIndex = days.indexOf(initialDay).coerceAtLeast(0))
    val hourState = rememberLazyListState(initialFirstVisibleItemIndex = hours.indexOf(initialHour).coerceAtLeast(0))
    val minuteState = rememberLazyListState(initialFirstVisibleItemIndex = minutes.indexOf(initialMinute).coerceAtLeast(0))

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1C1C1C),
            modifier = Modifier.padding(8.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("날짜 및 시간 설정", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.height(150.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    @Composable
                    fun WheelColumn(state: androidx.compose.foundation.lazy.LazyListState, items: List<Int>, format: String, width: Dp) {
                        LazyColumn(
                            state = state, modifier = Modifier.width(width), horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            items(items.size) { index ->
                                val isSelected = index == remember { derivedStateOf { state.firstVisibleItemIndex } }.value
                                Text(
                                    text = String.format(format, items[index]),
                                    fontSize = if (isSelected) 18.sp else 14.sp,
                                    color = if (isSelected) MarsOrange else Color.Gray,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    WheelColumn(yearState, years, "%04d", 50.dp)
                    Text("-", color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 2.dp))
                    WheelColumn(monthState, months, "%02d", 35.dp)
                    Text("-", color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 2.dp))
                    WheelColumn(dayState, days, "%02d", 35.dp)

                    Spacer(Modifier.width(10.dp))

                    WheelColumn(hourState, hours, "%02d", 35.dp)
                    Text(":", color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 2.dp))
                    WheelColumn(minuteState, minutes, "%02d", 35.dp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onDismissRequest, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)) {
                        Text("취소", color = Color.Gray)
                    }
                    Button(
                        onClick = {
                            val y = years[yearState.firstVisibleItemIndex]
                            val M = months[monthState.firstVisibleItemIndex]
                            val d = days[dayState.firstVisibleItemIndex]
                            val h = hours[hourState.firstVisibleItemIndex]
                            val m = minutes[minuteState.firstVisibleItemIndex]
                            onDateTimeSelected(String.format("%04d-%02d-%02d %02d:%02d", y, M, d, h, m))
                            onDismissRequest()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MarsOrange)
                    ) { Text("확인", color = Color.White) }
                }
            }
        }
    }
}

@Composable
private fun WaterRescueOverviewPanel(context: Context) {
    val currentDateTime = remember { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.KOREA).format(java.util.Date()) }

    var reportTime by remember { mutableStateOf(currentDateTime) }
    var arrivalTime by remember { mutableStateOf(currentDateTime) }
    var entryTime by remember { mutableStateOf(currentDateTime) }

    var victimInfo by remember { mutableStateOf("성별 / 나이") }
    var showVictimDialog by remember { mutableStateOf(false) }
    var victimInput by remember { mutableStateOf(victimInfo) }

    var equipCount by remember { mutableStateOf(0) }
    var personCount by remember { mutableStateOf(0) }
    var policeCount by remember { mutableStateOf(0) }
    var cityCount by remember { mutableStateOf(0) }
    var civilCount by remember { mutableStateOf(0) }

    var showTimePickerFor by remember { mutableStateOf<String?>(null) }

    if (showTimePickerFor != null) {
        val initialTime = when (showTimePickerFor) {
            "report" -> reportTime
            "arrival" -> arrivalTime
            "entry" -> entryTime
            else -> currentDateTime
        }
        CustomWheelDateTimePicker(
            initialDateTime = initialTime,
            onDateTimeSelected = { timeStr ->
                when (showTimePickerFor) {
                    "report" -> reportTime = timeStr
                    "arrival" -> arrivalTime = timeStr
                    "entry" -> entryTime = timeStr
                }
            },
            onDismissRequest = { showTimePickerFor = null }
        )
    }

    if (showVictimDialog) {
        AlertDialog(
            onDismissRequest = { showVictimDialog = false },
            containerColor = Color(0xFF1C1C1C),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("구조대상자 정보", fontWeight = FontWeight.Bold) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = victimInput,
                    onValueChange = { victimInput = it },
                    placeholder = { Text("예: 남 / 40대", color = Color.Gray) },
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MarsOrange,
                        unfocusedBorderColor = BorderGray,
                        cursorColor = MarsOrange
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = { victimInfo = victimInput; showVictimDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MarsOrange)
                ) { Text("확인", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                Button(
                    onClick = { showVictimDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
                ) { Text("취소", color = Color.LightGray) }
            }
        )
    }

    Column(
        modifier = Modifier
            .width(340.dp)
            .background(Color(0xEE1C1C1C), RoundedCornerShape(12.dp))
            .border(2.dp, WaterCyan, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("⚓ 수난구조 개요도", color = WaterCyan, fontWeight = FontWeight.Bold, fontSize = 18.sp)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("최초신고", color = Color.Gray, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Text(reportTime.substring(2), color = Color.White, fontSize = 14.sp, modifier = Modifier.clickable { showTimePickerFor = "report" })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("현장도착", color = Color.Gray, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Text(arrivalTime.substring(2), color = Color.White, fontSize = 14.sp, modifier = Modifier.clickable { showTimePickerFor = "arrival" })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("입수 추정", color = Color.Gray, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Text(entryTime.substring(2), color = Color.White, fontSize = 14.sp, modifier = Modifier.clickable { showTimePickerFor = "entry" })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("구조대상자", color = Color.Gray, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Text(victimInfo, color = MarsOrange, fontSize = 15.sp, modifier = Modifier.clickable { victimInput = victimInfo; showVictimDialog = true })
        }

        Divider(color = BorderGray)

        Text("동원 소방력", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("장비", color = Color.White, modifier = Modifier.width(40.dp))
            NumberPicker(equipCount) { equipCount = it }
            Spacer(Modifier.width(20.dp))
            Text("인원", color = Color.White, modifier = Modifier.width(40.dp))
            NumberPicker(personCount) { personCount = it }
        }

        Divider(color = BorderGray)

        Text("유관기관 지원", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("경찰", color = Color.White, modifier = Modifier.width(40.dp))
            NumberPicker(policeCount) { policeCount = it }
            Spacer(Modifier.width(20.dp))
            Text("시청", color = Color.White, modifier = Modifier.width(40.dp))
            NumberPicker(cityCount) { cityCount = it }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("민간", color = Color.White, modifier = Modifier.width(40.dp))
            NumberPicker(civilCount) { civilCount = it }
        }
    }
}

@Composable
private fun NumberPicker(value: Int, onValueChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(Color(0xFF333333), RoundedCornerShape(6.dp))) {
        Text("▼", color = MarsOrange, modifier = Modifier.clickable { if(value > 0) onValueChange(value - 1) }.padding(horizontal = 8.dp, vertical = 6.dp))
        Text(value.toString(), color = Color.White, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        Text("▲", color = MarsOrange, modifier = Modifier.clickable { onValueChange(value + 1) }.padding(horizontal = 8.dp, vertical = 6.dp))
    }
}

@Composable
private fun ApartmentSetupPanel(onSetup: (Int, Int) -> Unit) {
    var floors by remember { mutableStateOf(15) }
    var lines by remember { mutableStateOf(4) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🏢 인명수색 현황판 생성", color = MarsOrange, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("층수", color = Color.White, modifier = Modifier.width(50.dp))
            Button(onClick = { if (floors > 1) floors-- }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))) { Text("-") }
            Text("$floors 층", color = Color.White, modifier = Modifier.padding(horizontal = 16.dp), fontWeight = FontWeight.Bold)
            Button(onClick = { floors++ }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))) { Text("+") }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("라인", color = Color.White, modifier = Modifier.width(50.dp))
            Button(onClick = { if (lines > 1) lines-- }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))) { Text("-") }
            Text("$lines 라인", color = Color.White, modifier = Modifier.padding(horizontal = 16.dp), fontWeight = FontWeight.Bold)
            Button(onClick = { lines++ }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))) { Text("+") }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { onSetup(floors, lines) }, colors = ButtonDefaults.buttonColors(containerColor = MarsOrange, contentColor = Color.White), modifier = Modifier.fillMaxWidth()) {
            Text("현황판 생성하기", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun ApartmentGrid(apartmentData: com.example.marthianclean.viewmodel.ApartmentData, viewModel: IncidentViewModel) {
    val context = LocalContext.current
    val view = LocalView.current

    Column(modifier = Modifier.fillMaxSize()) {
        Text("🏢 동 인명수색 현황", color = MarsOrange, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            LegendItem(SearchStatus.WAITING)
            LegendItem(SearchStatus.SEARCHING)
            LegendItem(SearchStatus.COMPLETED)
            LegendItem(SearchStatus.DANGER)
        }
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(apartmentData.totalFloors) { floorIndex ->
                val floorNum = apartmentData.totalFloors - floorIndex
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    Text("${floorNum}F", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
                    for (line in 1..apartmentData.lines) {
                        val cellId = "${floorNum}_${line}"
                        val status = apartmentData.gridStatus[cellId] ?: SearchStatus.WAITING
                        val color = when (status) {
                            SearchStatus.WAITING -> Color(0xFF333333)
                            SearchStatus.SEARCHING -> CommandYellowGreen
                            SearchStatus.COMPLETED -> Color(0xFF00C853)
                            SearchStatus.DANGER -> NeonRed
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .padding(horizontal = 4.dp)
                                .background(color, RoundedCornerShape(4.dp))
                                .border(1.dp, if (status == SearchStatus.WAITING) Color.DarkGray else color, RoundedCornerShape(4.dp))
                                .clickable {
                                    if (!viewModel.isBriefingLocked) {
                                        viewModel.toggleApartmentSearchStatus(cellId)
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${floorNum}0${line}", color = if (status == SearchStatus.WAITING) Color.Gray else Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(status: SearchStatus) {
    val color = when (status) {
        SearchStatus.WAITING -> Color(0xFF333333)
        SearchStatus.SEARCHING -> CommandYellowGreen
        SearchStatus.COMPLETED -> Color(0xFF00C853)
        SearchStatus.DANGER -> NeonRed
    }
    val text = when (status) {
        SearchStatus.WAITING -> "대기"
        SearchStatus.SEARCHING -> "수색중"
        SearchStatus.COMPLETED -> "완료"
        SearchStatus.DANGER -> "위험"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, color = Color.LightGray, fontSize = 11.sp)
    }
}

@Composable
private fun BriefingTile(title: String, value: String, modifier: Modifier = Modifier, valueColor: Color = TextPrimary) {
    Column(modifier = modifier.fillMaxWidth().background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp)).border(1.dp, BorderGray, RoundedCornerShape(8.dp)).padding(8.dp)) {
        Text(text = title, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value.ifBlank { "-" }, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
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

    Column(modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("현장 브리핑", color = MarsOrange, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.weight(1f))
            TopBarButton(text = "허브", onClick = onBackToHub)
            Spacer(Modifier.width(6.dp))
            TopBarButton(text = "닫기", onClick = onClose)
        }
        Spacer(Modifier.height(12.dp))

        val sky = if (weatherData.sky != "-") weatherData.sky else meta.기상_날씨.takeIf { it.isNotBlank() && it != "-" } ?: "맑음"
        val temp = if (weatherData.temp != "-") "${weatherData.temp}℃" else meta.기상_기온.takeIf { it.isNotBlank() && it != "-" }?.let { "${it}℃" } ?: "-"
        val windDir = if (weatherData.windDirStr != "-") weatherData.windDirStr else meta.기상_풍향.takeIf { it.isNotBlank() && it != "-" } ?: "-"
        val windSpeed = if (weatherData.windSpeed != "-") "${weatherData.windSpeed}m/s" else meta.기상_풍속.takeIf { it.isNotBlank() && it != "-" }?.let { "${it}m/s" } ?: "-"

        Text("관측 기상 현황", color = MarsOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            BriefingTile("날씨", sky, Modifier.weight(1f))
            BriefingTile("기온", temp, Modifier.weight(1f))
            BriefingTile("풍향/풍속", "$windDir $windSpeed", Modifier.weight(1.5f))
        }
        Spacer(Modifier.height(12.dp))

        Text("재난 발생 개요", color = MarsOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        BriefingTile("발생 위치", incident?.address ?: "-", Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val fireTypeStr = incident?.fireType ?: "-"
            val displayFireType = if (fireTypeStr == "-") "-" else FireType.from(fireTypeStr).label

            BriefingTile("처종", displayFireType, Modifier.weight(1f))
            BriefingTile("대응 단계", incident?.대응단계 ?: "-", Modifier.weight(1f), valueColor = if(incident?.대응단계?.contains("단계") == true) NeonRed else TextPrimary)
        }
        Spacer(Modifier.height(4.dp))
        BriefingTile("화재 원인 추정", incident?.화재원인 ?: "-", Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))

        Text("주요 시간대별 현황", color = MarsOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            BriefingTile("신고 접수", incident?.신고접수일시 ?: "-", Modifier.weight(1f))
            BriefingTile("선착대 도착", incident?.선착대도착시간 ?: "-", Modifier.weight(1f))
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            BriefingTile("초진 시간", incident?.초진시간 ?: "-", Modifier.weight(1f))
            BriefingTile("완진 시간", incident?.완진시간 ?: "-", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))

        Text("주요 피해 현황", color = MarsOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            BriefingTile("인명 피해", incident?.인명피해현황 ?: "-", Modifier.weight(1f), valueColor = NeonRed)
            BriefingTile("재산 피해", incident?.재산피해현황 ?: "-", Modifier.weight(1f), valueColor = NeonRed)
            BriefingTile("대원 피해", incident?.대원피해현황 ?: "-", Modifier.weight(1f), valueColor = NeonOrange)
        }
        Spacer(Modifier.height(12.dp))

        Text("동원 소방력 현황", color = MarsOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            BriefingTile("소방력 인원", if (incident?.소방력_인원?.isNotBlank() == true) "${incident?.소방력_인원}명" else "-", Modifier.weight(1f))
            BriefingTile("차량(실제배치)", "${placed}대", Modifier.weight(1f))
        }
        Spacer(Modifier.height(4.dp))

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
        Spacer(Modifier.height(16.dp))
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