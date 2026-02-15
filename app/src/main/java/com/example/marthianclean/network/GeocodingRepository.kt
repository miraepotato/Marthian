// GeocodingRepository.kt
package com.example.marthianclean.network

class GeocodingRepository(
    private val service: NaverGeocodingService
) {
    data class Success(
        val resolvedAddress: String,
        val latitude: Double,
        val longitude: Double
    )

    sealed class Outcome {
        data class Ok(val data: Success) : Outcome()
        data class Fail(val reason: String) : Outcome()
    }

    suspend fun geocode(query: String): Outcome {
        val raw = query.trim()
        if (raw.isEmpty()) return Outcome.Fail("검색어가 비어있음")

        // ✅ 1차: 원문 그대로 시도 (POI/주소 모두 대응)
        val firstTry = request(raw)
        if (firstTry is Outcome.Ok) return firstTry

        // ✅ 2차: 주소 힌트 붙여서 재시도 (POI 입력 시 정확도 보완)
        val secondQuery = refineAsAddressHint(raw)
        // 1차가 이미 "주소"가 포함된 것과 유사하면 중복 시도 방지
        if (secondQuery == raw) return firstTry

        return request(secondQuery)
    }

    private fun refineAsAddressHint(raw: String): String {
        val looksLikeAddress =
            raw.contains("로") || raw.contains("길") || raw.contains("동") || raw.contains("읍") ||
                    raw.contains("면") || raw.contains("리") || raw.contains("시") || raw.contains("구") || raw.contains("번지")

        return if (looksLikeAddress) raw else "$raw 주소"
    }

    private suspend fun request(query: String): Outcome {
        val resp = try {
            service.geocode(query = query)
        } catch (e: Exception) {
            return Outcome.Fail("네트워크 예외: ${e.message ?: "unknown"}")
        }

        // ✅ HTTP 에러면 코드/바디 노출 (원인 확정용)
        if (!resp.isSuccessful) {
            val errBody = try { resp.errorBody()?.string() } catch (_: Exception) { null }
            android.util.Log.e("GEOCODE_HTTP", "code=${resp.code()} msg=${resp.message()} err=${errBody ?: "null"}")
            // UI용 reason은 너무 길면 지저분하니 요약
            val brief = "HTTP ${resp.code()} ${resp.message()}".trim()
            return Outcome.Fail(if (errBody.isNullOrBlank()) brief else "$brief | ${errBody.take(300)}")
        }

        val body = resp.body()
            ?: return Outcome.Fail("HTTP 200인데 body가 null")

        val first = body.addresses.firstOrNull()
            ?: return Outcome.Fail("검색 결과 0건 (query=$query)")

        val lat = first.y?.toDoubleOrNull()
            ?: return Outcome.Fail("위도 파싱 실패(y=${first.y})")

        val lng = first.x?.toDoubleOrNull()
            ?: return Outcome.Fail("경도 파싱 실패(x=${first.x})")

        val resolved = first.roadAddress ?: first.jibunAddress ?: query

        return Outcome.Ok(
            Success(
                resolvedAddress = resolved,
                latitude = lat,
                longitude = lng
            )
        )
    }
}
