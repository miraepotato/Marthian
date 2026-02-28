package com.example.marthianclean.network

import com.example.marthianclean.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private val ncpOkHttp: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            // 상세 로그 확인을 위해 BODY 유지
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .addHeader("x-ncp-apigw-api-key-id", BuildConfig.NCP_MAPS_CLIENT_ID)
                    .addHeader("x-ncp-apigw-api-key", BuildConfig.NCP_MAPS_CLIENT_SECRET)
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(logging)
            .build()
    }

    // ✅ 모든 NCP Map API는 이제 이 베이스 주소를 공용으로 사용합니다.
    private val ncpRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://maps.apigw.ntruss.com/")
            .client(ncpOkHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val geocodingService: NaverGeocodingService by lazy {
        ncpRetrofit.create(NaverGeocodingService::class.java)
    }

    val reverseGeocodingService: NaverReverseGeocodingService by lazy {
        ncpRetrofit.create(NaverReverseGeocodingService::class.java)
    }

    // ✅ 네이버 검색 API (별도 도메인 유지)
    private val openApiRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://openapi.naver.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val localSearchService: NaverLocalSearchService by lazy {
        openApiRetrofit.create(NaverLocalSearchService::class.java)
    }
}