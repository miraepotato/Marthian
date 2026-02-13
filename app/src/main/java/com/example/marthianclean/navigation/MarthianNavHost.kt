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

        // 1) 배너 -> 현장 선택
        composable(Routes.Banner) {
            BannerScreen(
                onFinished = { navController.navigate(Routes.FieldSelect) }
            )
        }

        // 2) 현장 선택 -> (새로운 현장: 주소검색) / (지난 현장: 상황판)
        composable(Routes.FieldSelect) {
            FieldSelectScreen(
                onNewIncident = { navController.navigate(Routes.AddressSearch) },
                onPastIncidents = { navController.navigate(Routes.SituationBoard) },
                onBack = { navController.popBackStack() }
            )
        }

        // 3) 주소 검색 -> 완료 시 Incident 생성 후 위성로딩으로
        composable(Routes.AddressSearch) {
            AddressSearchScreen(
                onDone = { incident ->
                    incidentViewModel.setIncident(incident)
                    navController.navigate(Routes.SatelliteLoading)
                },
                onBack = { navController.popBackStack() }
            )
        }

        // 4) 위성 로딩(문구) -> 상황판
        composable(Routes.SatelliteLoading) {
            SatelliteLoadingScreen(
                onFinished = {
                    navController.navigate(Routes.SituationBoard) {
                        popUpTo(Routes.Banner) { inclusive = false }
                    }
                }
            )
        }

        // 5) 상황판 -> 나가기 시 배너로
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
