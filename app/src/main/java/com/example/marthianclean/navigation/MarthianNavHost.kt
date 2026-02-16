package com.example.marthianclean.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
import com.example.marthianclean.viewmodel.IncidentViewModel

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

    NavHost(
        navController = navController,
        startDestination = Routes.Banner
    ) {
        composable(Routes.Banner) {
            BannerScreen(onFinished = { navController.navigate(Routes.FieldSelect) })
        }

        composable(Routes.FieldSelect) {
            FieldSelectScreen(
                onNewIncident = { navController.navigate(Routes.AddressSearch) },
                onPastIncidents = { navController.navigate(Routes.PastIncidents) },
                onBack = { navController.popBackStack() }
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
                    // ✅ 주소검색 직후 바로 저장(지난현장에도 남김)
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
                onBack = { navController.popBackStack() },
                onDone = {
                    val popped = navController.popBackStack(Routes.SituationBoard, inclusive = false)
                    if (!popped) navController.navigate(Routes.SituationBoard)
                }
            )
        }

        composable(Routes.IncidentInfoEdit) {
            val inc = incidentViewModel.incident.value
            val initialMeta = inc?.meta ?: IncidentMeta()

            IncidentInfoEditScreen(
                initialMeta = initialMeta,
                onBack = { navController.popBackStack() },
                onSave = { newMeta ->
                    incidentViewModel.updateIncidentMeta(newMeta)
                    incidentViewModel.saveCurrentIncident(context)
                    navController.popBackStack()
                }
            )
        }
    }
}
