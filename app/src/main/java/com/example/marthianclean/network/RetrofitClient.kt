package com.example.marthianclean.network

import com.example.marthianclean.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // =========================
    // 1) NCP Geocoding / ReverseGeocoding 공용 OkHttp
    // =========================
    private val ncpOkHttp: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
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

    // =========================
    // 2) Forward Geocoding (형님 기존)
    // =========================
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

    // =========================
    // ✅ 3) Reverse Geocoding
    // =========================
    private val reverseRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            // Reverse는 보통 이 도메인으로 제공됩니다.
            .baseUrl("https://naveropenapi.apigw.ntruss.com/")
            .client(ncpOkHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val reverseGeocodingService: NaverReverseGeocodingService by lazy {
        reverseRetrofit.create(NaverReverseGeocodingService::class.java)
    }

    // =========================
    // 4) Naver OpenAPI Local Search (openapi.naver.com)
    // =========================
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
