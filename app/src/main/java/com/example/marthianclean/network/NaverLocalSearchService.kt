package com.example.marthianclean.network

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface NaverLocalSearchService {

    @GET("v1/search/local.json")
    suspend fun searchLocal(
        @Header("X-Naver-Client-Id") clientId: String,
        @Header("X-Naver-Client-Secret") clientSecret: String,
        @Query("query") query: String,
        @Query("display") display: Int = 5,
        @Query("sort") sort: String = "random"
    ): NaverLocalSearchResponse
}
