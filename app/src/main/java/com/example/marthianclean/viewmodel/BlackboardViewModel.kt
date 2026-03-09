package com.example.marthianclean.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.marthianclean.data.FireResourceRepository
import com.example.marthianclean.model.FireStation
import com.example.marthianclean.util.MatrixCalculator // ✅ 매트릭스 계산기 임포트
import com.naver.maps.geometry.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BlackboardViewModel(private val repository: FireResourceRepository) : ViewModel() {

    private val _selectedStation = MutableStateFlow<FireStation?>(null)
    val selectedStation: StateFlow<FireStation?> = _selectedStation.asStateFlow()

    private val _dynamicMatrix = MutableStateFlow<List<List<Int>>>(emptyList())
    val dynamicMatrix: StateFlow<List<List<Int>>> = _dynamicMatrix.asStateFlow()

    private val _matrixDepartments = MutableStateFlow<List<String>>(emptyList())
    val matrixDepartments: StateFlow<List<String>> = _matrixDepartments.asStateFlow()

    private val _matrixEquipments = MutableStateFlow<List<String>>(emptyList())
    val matrixEquipments: StateFlow<List<String>> = _matrixEquipments.asStateFlow()

    // ✅ 소방서 선택 시 매트릭스 계산 로직까지 한 번에 실행
    fun selectStation(stationName: String, location: LatLng? = null) {
        viewModelScope.launch {
            val station = repository.getStationByName(stationName)
            _selectedStation.value = station

            station?.let {
                // 1. 해당 소방서의 센터 및 차량 데이터를 기반으로 매트릭스 계산
                val result = MatrixCalculator.calculateMatrix(it)

                // 2. 계산된 결과를 상태에 업데이트 (이때 UI가 자동으로 그려짐)
                _dynamicMatrix.value = result.matrix
                _matrixDepartments.value = result.departments
                _matrixEquipments.value = result.equipments
            }
        }
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