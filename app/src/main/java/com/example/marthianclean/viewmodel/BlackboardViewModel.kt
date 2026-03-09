package com.example.marthianclean.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.marthianclean.data.FireResourceRepository
import com.example.marthianclean.model.VehicleInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BlackboardViewModel(private val repository: FireResourceRepository) : ViewModel() {

    private val _targetVehicles = MutableStateFlow<List<VehicleInfo>>(emptyList())
    val targetVehicles: StateFlow<List<VehicleInfo>> = _targetVehicles.asStateFlow()

    private val _selectedStation = MutableStateFlow("")
    val selectedStation: StateFlow<String> = _selectedStation.asStateFlow()

    fun selectStation(stationName: String) {
        _selectedStation.value = stationName
        viewModelScope.launch {
            _targetVehicles.value = repository.getVehiclesByCenter(stationName)
        }
    }

    fun getVehiclesForCenter(centerName: String): List<VehicleInfo> {
        return repository.getVehiclesByCenter(centerName)
    }

    // ✅ 타 관할 전체 부서 가져오기 연동
    fun getAllCenters(stationName: String): List<String> {
        return repository.getAllCentersSorted(stationName)
    }
}

class BlackboardViewModelFactory(private val repository: FireResourceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BlackboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BlackboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}