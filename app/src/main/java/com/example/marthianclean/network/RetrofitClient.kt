// RetrofitClient.kt
package com.example.marthianclean.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // ✅ 네이버 공식 API 서버 주소 (끝에 / 필수)
    private const val BASE_URL = "https://maps.apigw.ntruss.com/"

    /**
     * ⚠️ 여기에 형님 Key를 "직접" 넣고 싶지 않으시면
     * Repository/ViewModel에서 주입하는 방식으로 바꿔도 됩니다.
     *
     * 지금은 401 원인 파악을 빠르게 하기 위해 "헤더 자동 주입" 형태로 제공합니다.
     */
    var NCP_KEY_ID: String = ""   // ex) "abcd1234..."
    var NCP_KEY: String = ""      // ex) "wxyz5678..."

    /**
     * 키 마스킹(로그용)
     */
    private fun maskKey(value: String, visible: Int = 4): String {
        if (value.isBlank()) return "(blank)"
        if (value.length <= visible) return "*".repeat(value.length)
        val head = value.take(visible)
        return head + "*".repeat(value.length - visible)
    }

    /**
     * ✅ 헤더 자동 주입 + 요청 기본 정보 로그(키는 마스킹)
     */
    private val authHeaderInterceptor = Interceptor { chain ->
        val original: Request = chain.request()

        val keyIdTrimmed = NCP_KEY_ID.trim()
        val keyTrimmed = NCP_KEY.trim()

        val newRequest = original.newBuilder()
            .header("X-NCP-APIGW-API-KEY-ID", keyIdTrimmed)
            .header("X-NCP-APIGW-API-KEY", keyTrimmed)
            .build()

        // 키는 절대 원문 로그로 남기지 않기 (마스킹)
        android.util.Log.e(
            "NaverGeo",
            "REQ ${newRequest.method} ${newRequest.url} | KEY_ID=${maskKey(keyIdTrimmed)} KEY=${maskKey(keyTrimmed)}"
        )

        chain.proceed(newRequest)
    }

    /**
     * ✅ HTTP 상세 로그 (Body는 너무 길 수 있어 BASIC 권장)
     * - 401/404 등의 코드와 URL 확인 목적
     */
    private val httpLoggingInterceptor = HttpLoggingInterceptor { message ->
        // 필요 시 너무 길면 필터링해도 됩니다.
        android.util.Log.d("NaverGeoHttp", message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(authHeaderInterceptor)
            .addInterceptor(httpLoggingInterceptor)
            .build()
    }

    val geocodingService: NaverGeocodingService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NaverGeocodingService::class.java)
    }
}
