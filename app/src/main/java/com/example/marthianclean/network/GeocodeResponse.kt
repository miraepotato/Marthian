// GeocodeResponse.kt  (파일명은 형님 프로젝트 스타일에 맞게 유지하셔도 됩니다)
package com.example.marthianclean.network

data class GeocodeResponse(
    val status: String? = null,
    val addresses: List<GeocodeAddress> = emptyList()
)

data class GeocodeAddress(
    val roadAddress: String? = null,
    val jibunAddress: String? = null,
    val englishAddress: String? = null,
    val x: String? = null, // 경도(longitude)
    val y: String? = null  // 위도(latitude)
)
