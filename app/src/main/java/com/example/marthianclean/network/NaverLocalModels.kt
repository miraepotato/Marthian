package com.example.marthianclean.network

import com.google.gson.annotations.SerializedName

data class NaverLocalSearchResponse(
    @SerializedName("items") val items: List<NaverLocalItem> = emptyList()
)

data class NaverLocalItem(
    @SerializedName("title") val title: String? = null,
    @SerializedName("roadAddress") val roadAddress: String? = null,
    @SerializedName("address") val address: String? = null
)
