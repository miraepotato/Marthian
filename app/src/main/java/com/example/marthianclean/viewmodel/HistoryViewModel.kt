package com.example.marthianclean.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.marthianclean.data.IncidentStore
import com.example.marthianclean.model.Incident
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel : ViewModel() {

    // 1. 상태 관리: 지난 현장 목록
    private val _incidents = MutableStateFlow<List<Incident>>(emptyList())
    val incidents = _incidents.asStateFlow()

    // 2. 상태 관리: 로딩 및 에러
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    // 3. 상태 관리: 선택된 ID 리스트 (삭제/추출용)
    private val _selectedIds = mutableStateListOf<String>()
    val selectedIds: List<String> get() = _selectedIds

    // 목록 새로고침
    fun reload(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            runCatching {
                // 신고접수일시 기준으로 내림차순 정렬하여 로드
                val list = IncidentStore.loadAll(context)
                    .sortedByDescending { it.meta.신고접수일시 }
                _incidents.value = list
            }.onFailure {
                _errorMessage.value = "기록을 불러오지 못했습니다: ${it.message}"
            }
            _isLoading.value = false
        }
    }

    // 선택 토글
    fun toggleSelection(id: String) {
        if (_selectedIds.contains(id)) {
            _selectedIds.remove(id)
        } else {
            _selectedIds.add(id)
        }
    }

    // 선택 초기화
    fun clearSelection() {
        _selectedIds.clear()
    }

    // 선택된 항목 삭제
    fun deleteSelected(context: Context) {
        if (_selectedIds.isEmpty()) return

        viewModelScope.launch {
            runCatching {
                IncidentStore.deleteMany(context, _selectedIds.toList())
                _selectedIds.clear()
                reload(context)
            }.onFailure {
                _errorMessage.value = "삭제 중 오류 발생: ${it.message}"
            }
        }
    }

    // 선택된 데이터 추출용 리스트 반환
    fun getSelectedIncidents(): List<Incident> {
        return _incidents.value.filter { _selectedIds.contains(it.id) }
    }
}