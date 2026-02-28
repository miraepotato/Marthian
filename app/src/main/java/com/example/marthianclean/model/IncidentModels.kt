package com.example.marthianclean.model

import com.naver.maps.geometry.LatLng
import kotlinx.serialization.Serializable

@Serializable
data class DispatchPlan(
    val matrix: List<List<Int>> = emptyList(),
    val departments: List<String> = emptyList(),
    val equipments: List<String> = emptyList()
)

@Serializable
data class IncidentMeta(
    val reportedAtMillis: Long? = null,
    val dispatchAtMillis: Long? = null,
    val firstArriveAtMillis: Long? = null,
    val fireType: String = "",
    val memo: String = "",

    val 신고접수일시: String = "",
    val 재난발생위치: String = "",

    val 대응단계: String = "해당 사항 없음",
    val 화재원인: String = "",

    val 초진시간: String = "",
    val 완진시간: String = "",

    val 기상_날씨: String = "맑음",
    val 기상_기온: String = "0°C",
    val 기상_풍향: String = "북",
    val 기상_풍속: String = "0m/s",

    val 선착대도착시간: String = "",

    val 인명피해현황: String = "",
    val 재산피해현황: String = "",
    val 대원피해현황: String = "",

    // ✅ 소방력 인원 필드 추가
    val 소방력_인원: String = "",

    val 유관기관_경찰: String = "",
    val 유관기관_시청: String = "",
    val 유관기관_한전: String = "",
    val 유관기관_도시가스: String = "",
    val 유관기관_산불진화대_화성시: String = ""
)

@Serializable
data class VehiclePlacement(
    val id: String,
    val department: String,
    val equipment: String,
    val lat: Double,
    val lng: Double
) {
    fun toLatLng(): LatLng = LatLng(lat, lng)
}