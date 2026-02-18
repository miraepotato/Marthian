package com.example.marthianclean.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Incident(
    val id: String = UUID.randomUUID().toString(),
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,

    // ✅ 편성/배치/현장정보 (기본값 필수)
    val dispatchPlan: DispatchPlan = DispatchPlan(),
    val placements: List<VehiclePlacement> = emptyList(),
    val meta: IncidentMeta = IncidentMeta(),

    val createdAtMillis: Long = System.currentTimeMillis()
)
