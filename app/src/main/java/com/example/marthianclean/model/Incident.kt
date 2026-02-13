package com.example.marthianclean.model

import java.util.UUID

data class Incident(
    val id: String = UUID.randomUUID().toString(),
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val disasterType: String = "FIRE",
    val timestamp: Long = System.currentTimeMillis(),
    val editable: Boolean = true
)
