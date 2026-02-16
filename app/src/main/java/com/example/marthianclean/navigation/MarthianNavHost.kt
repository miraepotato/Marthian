package com.example.marthianclean.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.marthianclean.ui.banner.BannerScreen
import com.example.marthianclean.ui.dispatch.DispatchMatrixScreen
import com.example.marthianclean.ui.field.AddressSearchScreen
import com.example.marthianclean.ui.field.FieldSelectScreen
import com.example.marthianclean.ui.field.IncidentEditHubScreen
import com.example.marthianclean.ui.field.PastIncidentsScreen
import com.example.marthianclean.ui.situation.SatelliteLoadingScreen
import com.example.marthianclean.ui.situation.SituationBoardScreen
import com.example.marthianclean.viewmodel.IncidentViewModel

object Routes {
    const val Banner = "banner"
    const val FieldSelect = "field_select"
    const val AddressSearch = "address_search"
    const val SatelliteLoading = "satellite_loading"
    const val SituationBoard = "situation_board"

    // ✅ 지난 현장 목록
    const val PastIncidents = "past_incidents"

    // ✅ 좌슬라이딩 허브
    const val IncidentEditHub = "incident_edit_hub"

    // ✅ 출동대 편성(매트릭스) 화면
    const val DispatchMatrix = "dispatch_matrix"
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

        // ✅ 새 현장 시작/지난 현장 선택 화면
        composable(Routes.FieldSelect) {
            FieldSelectScreen(
                onNewIncident = { navController.navigate(Routes.AddressSearch) },
                onPastIncidents = { navController.navigate(Routes.PastIncidents) },
                onBack = { navController.popBackStack() }
            )
        }

        // ✅ 지난 현장 목록
        composable(Routes.PastIncidents) {
            PastIncidentsScreen(
                onBack = { navController.popBackStack() },
                onOpenIncident = { inc ->
                    incidentViewModel.setIncidentAndRestoreAll(inc)
                    navController.navigate(Routes.SatelliteLoading)
                }
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

        // ✅ 상황판
        composable(Routes.SituationBoard) {
            SituationBoardScreen(
                incidentViewModel = incidentViewModel,
                onEdit = { navController.navigate(Routes.IncidentEditHub) },
                onExit = {
                    navController.navigate(Routes.Banner) {
                        popUpTo(Routes.Banner) { inclusive = true }
                    }
                }
            )
        }

        // ✅ 좌슬라이딩 허브
        composable(Routes.IncidentEditHub) {
            IncidentEditHubScreen(
                onEditMatrix = {
                    navController.navigate(Routes.DispatchMatrix)
                },
                onEditInfo = {
                    navController.navigate(Routes.AddressSearch)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // ✅ 출동대 편성(매트릭스) - ViewModel 공유 주입
        composable(Routes.DispatchMatrix) {
            DispatchMatrixScreen(
                incidentViewModel = incidentViewModel,
                onBack = { navController.popBackStack() },
                onDone = {
                    // ✅ 허브 안 거치고 바로 상황판으로
                    val popped = navController.popBackStack(Routes.SituationBoard, inclusive = false)
                    if (!popped) {
                        navController.navigate(Routes.SituationBoard)
                    }
                }
            )
        }
    }
}
