package com.example.marthianclean.network

import com.example.marthianclean.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// ✅ 기상청 지상관측자료(ASOS/AWS) API 서비스 인터페이스
interface KmaApiService {
    @GET("api/typ01/url/kma_sfctm2.php")
    suspend fun getStationWeather(
        @Query("tm") time: String,      // 예: 202603031400 (yyyymmddHHMI)
        @Query("stn") stnId: Int = 0,   // 0: 전체 지점, 특정 지역 번호 입력 가능
        @Query("help") help: Int = 0,   // 0: 주석 제거(데이터만), 1: 헤더 포함
        @Query("authKey") key: String = "XtWlgdPMRl6VpYHTzIZejA" // 형님 인증키
    ): ResponseBody // 응답이 JSON이 아닌 일반 텍스트이므로 ResponseBody로 직접 받아서 파싱
}

object RetrofitClient {

    // ==========================================
    // 1. 네이버 클라우드 플랫폼 (NCP) Maps 통신망
    // ==========================================
    private val ncpOkHttp: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
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


    // ==========================================
    // 2. 네이버 OpenAPI (지역 검색) 통신망
    // ==========================================
    private val openApiRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://openapi.naver.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val localSearchService: NaverLocalSearchService by lazy {
        openApiRetrofit.create(NaverLocalSearchService::class.java)
    }


    // ==========================================
    // 3. 기상청 API 허브 통신망 (신규 추가)
    // ==========================================
    private val kmaOkHttp: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        // 기상청은 NCP 헤더가 필요 없으므로 순수한 OkHttpClient를 사용합니다.
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    private val kmaRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://apihub.kma.go.kr/")
            .client(kmaOkHttp)
            // ResponseBody로 직접 텍스트를 받기 때문에 ConverterFactory 추가 불필요
            .build()
    }

    val kmaService: KmaApiService by lazy {
        kmaRetrofit.create(KmaApiService::class.java)
    }
}