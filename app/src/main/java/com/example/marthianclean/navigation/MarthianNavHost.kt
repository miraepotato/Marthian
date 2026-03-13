package com.example.marthianclean.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.marthianclean.data.FireResourceRepository
import com.example.marthianclean.model.IncidentMeta
import com.example.marthianclean.ui.banner.BannerScreen
import com.example.marthianclean.ui.dispatch.DispatchMatrixScreen
import com.example.marthianclean.ui.field.AddressSearchScreen
import com.example.marthianclean.ui.field.FieldSelectScreen
import com.example.marthianclean.ui.field.IncidentEditHubScreen
import com.example.marthianclean.ui.field.IncidentInfoEditScreen
import com.example.marthianclean.ui.field.PastIncidentsScreen
import com.example.marthianclean.ui.situation.SatelliteLoadingScreen
import com.example.marthianclean.ui.situation.SituationBoardScreen
import com.example.marthianclean.viewmodel.BlackboardViewModel
import com.example.marthianclean.viewmodel.BlackboardViewModelFactory
import com.example.marthianclean.viewmodel.IncidentViewModel
import com.example.marthianclean.model.Incident

object Routes {
    const val Banner = "banner"
    const val FieldSelect = "field_select"
    const val AddressSearch = "address_search"
    const val SatelliteLoading = "satellite_loading"
    const val SituationBoard = "situation_board"

    const val PastIncidents = "past_incidents"
    const val IncidentEditHub = "incident_edit_hub"
    const val DispatchMatrix = "dispatch_matrix"
    const val IncidentInfoEdit = "incident_info_edit"
}

@Composable
fun MarthianNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current

    val incidentViewModel: IncidentViewModel = viewModel()

    // ✅ 해결 1: Repository에 context를 전달합니다. (No value passed 에러 해결)
    val repository = remember { FireResourceRepository(context) }
    val blackboardViewModel: BlackboardViewModel = viewModel(
        factory = BlackboardViewModelFactory(repository)
    )

    NavHost(
        navController = navController,
        startDestination = Routes.Banner
    ) {
        composable(Routes.Banner) {
            BannerScreen(onFinished = { navController.navigate(Routes.FieldSelect) })
        }

        composable(Routes.FieldSelect) {
            FieldSelectScreen(
                onPastIncidents = { navController.navigate(Routes.PastIncidents) },
                onNewIncident = { stationName ->
                    // 👇 이 줄을 추가하여 소방서 선택 시 무조건 이전 잔상을 완전히 폭파시킵니다.
                    incidentViewModel.clearIncident()

                    incidentViewModel.selectedStationName = stationName
                    blackboardViewModel.selectStation(stationName)
                    navController.navigate(Routes.AddressSearch)
                }
            )
        }

        composable(Routes.PastIncidents) {
            PastIncidentsScreen(
                onBack = { navController.popBackStack() },
                onOpenIncident = { inc ->
                    incidentViewModel.setIncidentAndRestoreAll(inc)
                    navController.navigate(Routes.SatelliteLoading)
                }
            )
        }

        composable(Routes.AddressSearch) {
            AddressSearchScreen(
                onDone = { incident ->
                    incidentViewModel.setIncident(incident)
                    incidentViewModel.saveCurrentIncident(context)
                    navController.navigate(Routes.SatelliteLoading)
                },
                onBack = { navController.popBackStack() }
            )
        }

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
                onEdit = { navController.navigate(Routes.IncidentEditHub) },
                onExit = {
                    navController.navigate(Routes.Banner) {
                        popUpTo(Routes.Banner) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.IncidentEditHub) {
            IncidentEditHubScreen(
                onEditMatrix = { navController.navigate(Routes.DispatchMatrix) },
                onEditInfo = { navController.navigate(Routes.IncidentInfoEdit) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.DispatchMatrix) {
            DispatchMatrixScreen(
                incidentViewModel = incidentViewModel,
                blackboardViewModel = blackboardViewModel,
                onBack = { navController.popBackStack() },
                onDone = {
                    val popped = navController.popBackStack(Routes.SituationBoard, inclusive = false)
                    if (!popped) navController.navigate(Routes.SituationBoard)
                }
            )
        }

        composable(Routes.IncidentInfoEdit) {
            val inc by incidentViewModel.incident.collectAsState()
            val currentIncident = inc ?: Incident() // 현재 전체 데이터 불러오기

            IncidentInfoEditScreen(
                initialIncident = currentIncident, // 전체를 넘김
                onBack = { navController.popBackStack() },
                onSave = { updatedIncident ->
                    // ✅ 수정된 전체 데이터를 뷰모델에 덮어쓰고 저장!
                    incidentViewModel.updateFullIncident(updatedIncident)
                    incidentViewModel.saveCurrentIncident(context)
                    navController.popBackStack()
                }
            )
        }
    }
}