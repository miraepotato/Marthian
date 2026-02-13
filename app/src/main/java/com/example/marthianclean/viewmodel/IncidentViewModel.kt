package com.example.marthianclean.viewmodel

import androidx.lifecycle.ViewModel
import com.example.marthianclean.model.Incident
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class IncidentViewModel : ViewModel() {

    private val _incident = MutableStateFlow<Incident?>(null)
    val incident: StateFlow<Incident?> = _incident.asStateFlow()

    fun setIncident(value: Incident) {
        _incident.value = value
    }

    fun clearIncident() {
        _incident.value = null
    }
}
