package com.example.marthianclean.model

import androidx.annotation.DrawableRes
import com.example.marthianclean.R

object MarkerIconMapper {
    @DrawableRes
    fun markerResFor(type: FireType): Int = when (type) {
        FireType.FACTORY -> R.drawable.ic_marker_factory
        FireType.WAREHOUSE -> R.drawable.ic_marker_warehouse
        FireType.SINGLE_HOUSE -> R.drawable.ic_marker_single_house
        FireType.APARTMENT -> R.drawable.ic_marker_apartment
        FireType.COMMERCIAL -> R.drawable.ic_marker_commercial
        FireType.HAZMAT_PLANT -> R.drawable.ic_marker_hazmat_plant
        FireType.FOREST -> R.drawable.ic_marker_forest
        FireType.PIG_FARM -> R.drawable.ic_marker_pig_farm
        FireType.COW_FARM -> R.drawable.ic_marker_cow_farm
        FireType.CHICKEN_FARM -> R.drawable.ic_marker_chicken_farm
        FireType.GREENHOUSE -> R.drawable.ic_marker_greenhouse
        FireType.OTHER -> R.drawable.ic_marker_other
    }
}
