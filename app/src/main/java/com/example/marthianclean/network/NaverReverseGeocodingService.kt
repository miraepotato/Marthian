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
        @Query("output") output: String = "json"
    ): Response<ReverseGeocodeResponse>
}

// ===== DTO (필요 최소만) =====

data class ReverseGeocodeResponse(
    val results: List<ReverseResult>?
)

data class ReverseResult(
    val region: ReverseRegion?,
    val land: ReverseLand?
)

data class ReverseRegion(
    val area1: ReverseArea?,
    val area2: ReverseArea?,
    val area3: ReverseArea?,
    val area4: ReverseArea?
)

data class ReverseArea(
    val name: String?
)

data class ReverseLand(
    val name: String?,
    val number1: String?,
    val number2: String?,
    val addition0: ReverseAddition?
)

data class ReverseAddition(
    val value: String?
)
