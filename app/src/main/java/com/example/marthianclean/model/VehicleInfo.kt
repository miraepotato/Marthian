package com.example.marthianclean.model

/**
 * 경기도 소방차량 목록 CSV의 한 행을 담는 데이터 모델
 */
data class VehicleInfo(
    val station: String,  // 소방서 (예: 화성소방서)
    val center: String,   // 센터/부서 (예: 향남119안전센터)
    val type: String,      // 차량종류 (예: 펌프차)
    val callsign: String,  // 콜사인 (예: 향남1펌프)
    var isSelected: Boolean = false // 매트릭스에서 선택(체크)되었는지 여부
)