package com.example.marthianclean.network

import com.example.marthianclean.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // =========================
    // 1) NCP Geocoding (maps.apigw.ntruss.com)
    // =========================
    private val ncpOkHttp: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    // ✅ NCP Geocoding 인증 헤더
                    .addHeader("x-ncp-apigw-api-key-id", BuildConfig.NCP_MAPS_CLIENT_ID)
                    .addHeader("x-ncp-apigw-api-key", BuildConfig.NCP_MAPS_CLIENT_SECRET)
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(logging)
            .build()
    }

    private val ncpRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://maps.apigw.ntruss.com/")
            .client(ncpOkHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // ✅ 형님 기존 코드가 이 이름을 사용 중
    val geocodingService: NaverGeocodingService by lazy {
        ncpRetrofit.create(NaverGeocodingService::class.java)
    }

    // =========================
    // 2) Naver OpenAPI Local Search (openapi.naver.com)
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
