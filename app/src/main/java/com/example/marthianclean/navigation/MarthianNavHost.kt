package com.example.marthianclean.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.marthianclean.ui.banner.BannerScreen
import com.example.marthianclean.ui.field.AddressSearchScreen
import com.example.marthianclean.ui.field.FieldSelectScreen
import com.example.marthianclean.ui.situation.SatelliteLoadingScreen
import com.example.marthianclean.ui.situation.SituationBoardScreen
import com.example.marthianclean.viewmodel.IncidentViewModel

object Routes {
    const val Banner = "banner"
    const val FieldSelect = "field_select"
    const val AddressSearch = "address_search"
    const val SatelliteLoading = "satellite_loading"
    const val SituationBoard = "situation_board"
}

@Composable
fun MarthianNavHost() {
    val navController = rememberNavController()

    // ✅ Incident 공유 ViewModel (NavHost 범위에서 공유)
    val incidentViewModel: IncidentViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Routes.Banner
    ) {

        composable(Routes.Banner) {
            BannerScreen(
                onFinished = { navController.navigate(Routes.FieldSelect) }
            )
        }

        composable(Routes.FieldSelect) {
            FieldSelectScreen(
                onNewIncident = { navController.navigate(Routes.AddressSearch) },
                onPastIncidents = { navController.navigate(Routes.SituationBoard) },
                onBack = { navController.popBackStack() }
            )
        }

        // ✅ 주소 검색 -> onDone(incident)으로 받는 구조 유지
        composable(Routes.AddressSearch) {
            AddressSearchScreen(
                onDone = { incident ->
                    incidentViewModel.setIncident(incident)
                    navController.navigate(Routes.SatelliteLoading)
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ✅ 위성 로딩 -> 최소 1.2초 보장 후 상황판
        composable(Routes.SatelliteLoading) {
            SatelliteLoadingScreen(
                onFinished = {
                    navController.navigate(Routes.SituationBoard) {
                        popUpTo(Routes.Banner) { inclusive = false }
                    }
                }
            )
        }

        composable(Routes.SituationBoard) {
            SituationBoardScreen(
                incidentViewModel = incidentViewModel,
                onExit = {
                    navController.navigate(Routes.Banner) {
                        popUpTo(Routes.Banner) { inclusive = true }
                    }
                }
            )
        }
    }
}
