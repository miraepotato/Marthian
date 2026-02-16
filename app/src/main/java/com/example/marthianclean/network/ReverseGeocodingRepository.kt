package com.example.marthianclean.network

class ReverseGeocodingRepository(
    private val service: NaverReverseGeocodingService
) {
    sealed class Outcome {
        data class Ok(val address: String) : Outcome()
        data class Fail(val reason: String) : Outcome()
    }

    suspend fun reverse(lat: Double, lng: Double): Outcome {
        val coords = "$lng,$lat"

        val resp = try {
            service.reverseGeocode(coords = coords)
        } catch (e: Exception) {
            return Outcome.Fail("네트워크 예외: ${e.message ?: "unknown"}")
        }

        if (!resp.isSuccessful) {
            val errBody = try { resp.errorBody()?.string() } catch (_: Exception) { null }
            val brief = "HTTP ${resp.code()} ${resp.message()}".trim()
            return Outcome.Fail(if (errBody.isNullOrBlank()) brief else "$brief | ${errBody.take(200)}")
        }

        val body = resp.body() ?: return Outcome.Fail("HTTP 200인데 body가 null")

        val first = body.results?.firstOrNull()
            ?: return Outcome.Fail("reverse 결과 0건")

        val region = first.region
        val land = first.land

        val area = listOfNotNull(
            region?.area1?.name,
            region?.area2?.name,
            region?.area3?.name,
            region?.area4?.name
        ).joinToString(" ").trim()

        val landName = land?.name?.trim().orEmpty()
        val n1 = land?.number1?.trim().orEmpty()
        val n2 = land?.number2?.trim().orEmpty()
        val bunji = when {
            n1.isNotBlank() && n2.isNotBlank() -> "$n1-$n2"
            n1.isNotBlank() -> n1
            else -> ""
        }

        val roadAddr = listOf(area, landName, bunji)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .trim()

        val best = roadAddr.ifBlank {
            // addition0.value가 있으면 그걸로
            land?.addition0?.value?.trim().orEmpty()
        }.ifBlank {
            // 최후 fallback
            coords
        }

        return Outcome.Ok(best)
    }
}
