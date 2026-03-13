package com.example.marthianclean.model

import androidx.annotation.DrawableRes
import com.example.marthianclean.R

object MarkerIconMapper {
    @DrawableRes
    fun markerResFor(type: FireType): Int {
        // 형님의 지시대로, 이제 처종(type)에 상관없이
        // 무조건 기본 화재 마커(ic_marker_other) 하나로 통일하여 반환합니다.
        return R.drawable.ic_marker_other
    }
}