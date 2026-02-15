// NaverGeocodingService.kt
package com.example.marthianclean.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NaverGeocodingService {

    // ✅ 네이버 지도 Geocoding (공식 엔드포인트)
    // 최종 URL: https://naveropenapi.apigw.ntruss.com/map-geocode/v2/geocode?query=...
    @GET("map-geocode/v2/geocode")
    suspend fun geocode(
        @Query("query") query: String
    ): Response<GeocodeResponse>
}
