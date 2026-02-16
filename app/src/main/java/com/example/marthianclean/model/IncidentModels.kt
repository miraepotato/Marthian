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
    val reportedAtMillis: Long? = null,     // 신고 시각
    val dispatchAtMillis: Long? = null,     // 출동 시각
    val firstArriveAtMillis: Long? = null,  // 선착대 도착 시각
    val fireType: String = "",              // 처종(아이콘/마킹 등에 사용)
    val memo: String = ""                   // 메모
)

/**
 * ✅ 배치 저장용
 * - LatLng 자체는 직렬화가 안 되니까 lat/lng로 저장
 */
@Serializable
data class VehiclePlacement(
    val id: String,          // rX_cY
    val department: String,
    val equipment: String,
    val lat: Double,
    val lng: Double
) {
    fun toLatLng(): LatLng = LatLng(lat, lng)
}
