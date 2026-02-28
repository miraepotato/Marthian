package com.example.marthianclean.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NaverReverseGeocodingService {

    // NCP Reverse Geocoding (coordsToaddr)
    @GET("map-reversegeocode/v2/gc")
    suspend fun reverseGeocode(
        @Query("request") request: String = "coordsToaddr",
        @Query("coords") coords: String, // "lng,lat"
        @Query("sourcecrs") sourceCrs: String = "epsg:4326",
        @Query("output") output: String = "json",
        // ✅ orders에 addr,admcode,roadaddr를 추가하여 상세 지번과 도로명을 모두 요청합니다.
        @Query("orders") orders: String = "addr,roadaddr"
    ): Response<ReverseGeocodeResponse>
}

// ===== DTO (상세 주소 파싱을 위해 보강됨) =====

data class ReverseGeocodeResponse(
    val status: ReverseStatus?,
    val results: List<ReverseResult>?
)

data class ReverseStatus(
    val code: Int?,
    val name: String?,
    val message: String?
)

data class ReverseResult(
    val name: String?, // "addr" 또는 "roadaddr"
    val region: ReverseRegion?,
    val land: ReverseLand?
)

data class ReverseRegion(
    val area1: ReverseArea?, // 시/도
    val area2: ReverseArea?, // 시/군/구
    val area3: ReverseArea?, // 읍/면/동
    val area4: ReverseArea?  // 리
)

data class ReverseArea(
    val name: String?
)

data class ReverseLand(
    val name: String?,    // 건물명
    val number1: String?, // 본번 (예: 436)
    val number2: String?, // 부번 (예: 6)
    val addition0: ReverseAddition? // 상세 건물명 등
)

data class ReverseAddition(
    val type: String?,
    val value: String?
)