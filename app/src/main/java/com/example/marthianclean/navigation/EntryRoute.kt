package com.example.marthianclean.navigation

sealed class EntryRoute(val route: String) {

    object Banner : EntryRoute("banner")
    object FieldSelect : EntryRoute("field_select")
    object AddressSearch : EntryRoute("address_search")

    object MapPreview : EntryRoute("map_preview?address={address}") {
        fun createRoute(address: String): String =
            "map_preview?address=$address"
    }

    object SatelliteLoading : EntryRoute("satellite_loading")

    object SituationBoard : EntryRoute("situation_board?editable={editable}") {
        fun createRoute(editable: Boolean): String =
            "situation_board?editable=$editable"
    }
}
