package com.example.marthianclean.model

import com.naver.maps.geometry.LatLng

// [통합 마스터 모델]
data class Incident(
    val id: String = "",
    val latitude: Double = 37.1994,
    val longitude: Double = 126.9161,
    val address: String = "",
    val startTime: String = "-",
    val fireType: String = "-",
    val 대응단계: String = "-",
    val 화재원인: String = "-",
    val 신고접수일시: String = "-",
    val 선착대도착시간: String = "-",
    val 초진시간: String = "-",
    val 완진시간: String = "-",
    val 인명피해현황: String = "-",
    val 재산피해현황: String = "-",
    val 대원피해현황: String = "-",
    val 소방력_인원: String = "-",
    val 유관기관_경찰: String = "-",
    val 유관기관_시청: String = "-",
    val 유관기관_한전: String = "-",
    val 유관기관_도시가스: String = "-",
    val 유관기관_산불진화대_화성시: String = "-",
    val meta: IncidentMeta = IncidentMeta(),
    val dispatchPlan: DispatchPlan = DispatchPlan(),
    val placements: List<VehiclePlacement> = emptyList()
) {
    val location: LatLng get() = LatLng(latitude, longitude)
}

data class IncidentMeta(
    val 재난발생위치: String = "",
    val 신고접수일시: String = "",
    val 기상_기온: String = "-",
    val 기상_풍속: String = "-",
    val 기상_풍향: String = "-",
    val 기상_날씨: String = "-",
    val 현장상태: String = "접수"
)

data class DispatchPlan(
    val matrix: List<List<Int>> = emptyList(),
    val departments: List<String> = emptyList(),
    val equipments: List<String> = emptyList()
)

data class VehiclePlacement(
    val id: String,
    val department: String,
    val equipment: String,
    val lat: Double,
    val lng: Double
)

data class FireStation(
    val name: String,
    val location: LatLng,
    val centers: List<SafetyCenter>
)

data class SafetyCenter(
    val name: String,
    val location: LatLng,
    val vehicles: List<FireVehicle>
)

data class FireVehicle(
    val type: String,
    val callSign: String,
    val plateNumber: String,
    val status: String = "대기"
)