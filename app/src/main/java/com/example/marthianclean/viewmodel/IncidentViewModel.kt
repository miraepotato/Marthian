package com.example.marthianclean.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.marthianclean.BuildConfig
import com.example.marthianclean.model.Incident
import com.example.marthianclean.network.GeocodingRepository
import com.example.marthianclean.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IncidentViewModel : ViewModel() {

    private val _incident = MutableStateFlow<Incident?>(null)
    val incident: StateFlow<Incident?> = _incident.asStateFlow()

    private val geocodingRepo = GeocodingRepository(RetrofitClient.geocodingService)

    init {
        // ✅ local.properties -> BuildConfig 로 주입된 값을 1회 세팅
        RetrofitClient.NCP_KEY_ID = BuildConfig.NCP_MAPS_CLIENT_ID
        RetrofitClient.NCP_KEY = BuildConfig.NCP_MAPS_CLIENT_SECRET

        // (선택) 혹시 키가 비어있으면 로그로만 알려주기 (크래시 방지)
        if (BuildConfig.NCP_MAPS_CLIENT_ID.isBlank() || BuildConfig.NCP_MAPS_CLIENT_SECRET.isBlank()) {
            android.util.Log.e("NCP_KEY", "NCP keys are blank. Check local.properties & Gradle sync.")
        }
    }

    fun setIncident(value: Incident) {
        _incident.value = value
    }

    fun clearIncident() {
        _incident.value = null
    }

    fun geocodeAndApply(
        query: String,
        onSuccess: () -> Unit,
        onFail: (String) -> Unit
    ) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            onFail("검색어가 비어 있습니다.")
            return
        }

        viewModelScope.launch {
            when (val out = geocodingRepo.geocode(trimmed)) {
                is GeocodingRepository.Outcome.Fail -> onFail(out.reason)
                is GeocodingRepository.Outcome.Ok -> {
                    val data = out.data
                    android.util.Log.e(
                        "MAP_DEBUG",
                        "GEOCODE OK addr=${data.resolvedAddress}, lat=${data.latitude}, lng=${data.longitude}"
                    )

                    val current = _incident.value
                    val updated = current?.copy(
                        address = data.resolvedAddress,
                        latitude = data.latitude,
                        longitude = data.longitude
                    ) ?: Incident(
                        address = data.resolvedAddress,
                        latitude = data.latitude,
                        longitude = data.longitude
                    )

                    _incident.value = updated
                    android.util.Log.e("MAP_DEBUG", "INCIDENT UPDATED=${_incident.value}")

                    onSuccess()
                }
            }
        }
    }
}
