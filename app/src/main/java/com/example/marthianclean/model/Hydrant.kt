package com.example.marthianclean.model

/**
 * 소화전 데이터 모델
 */
data class Hydrant(
    val id: String,           // 시설번호 (FACLT_NO)
    val name: String,         // 시설상세명 (FACLT_DEVICE_DETAIL_DIV_NM)
    val lat: Double,          // 위도 (REFINE_WGS84_LAT)
    val lng: Double,          // 경도 (REFINE_WGS84_LOGT)
    val type: String          // 시설유형 (FACLT_TYPE_CD)
)