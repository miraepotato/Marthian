package com.example.marthianclean.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NaverGeocodingService {

    // display=10 추가
    @GET("map-geocode/v2/geocode")
    suspend fun geocode(
        @Query("query") query: String,
        @Query("display") display: Int = 10   // 🔥 최대 10개 요청
    ): Response<GeocodeResponse>
}
