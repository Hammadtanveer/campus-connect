package com.example.campusconnect

import android.widget.Toast
import com.example.campusconnect.ui.screens.AccountView
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.campusconnect.ui.screens.DownloadView
import com.example.campusconnect.ui.screens.PlacementCareerScreen
import com.example.campusconnect.ui.screens.Seniors
import com.example.campusconnect.ui.screens.Societies
import com.example.campusconnect.ui.screens.EventsListScreen
import com.example.campusconnect.ui.screens.EventDetailScreen
import com.example.campusconnect.ui.screens.CreateEventScreen
import com.example.campusconnect.ui.screens.AdminPanelScreen
import com.example.campusconnect.ui.screens.UploadNoteScreen
import com.example.campusconnect.ui.screens.NotesScreen
import com.example.campusconnect.ui.senior.SeniorDetailScreen
import com.example.campusconnect.ui.senior.SeniorEditScreen
import com.example.campusconnect.ui.senior.SeniorAddScreen
import com.example.campusconnect.ui.placement.AddPlacementScreen
import com.example.campusconnect.ui.placement.PlacementDetailScreen

@Composable
fun Navigation(navController: NavController, viewModel: MainViewModel, pd: PaddingValues) {
    NavHost(
        navController = navController as NavHostController,
        startDestination = Screen.DrawerScreen.Profile.route,
        modifier = Modifier.padding(pd)
    ) {
        composable(Screen.DrawerScreen.Notes.route) {
            NotesScreen(navController = navController, mainViewModel = viewModel)
        }
        composable("upload_note") {
            UploadNoteScreen(
                onNavigateBack = { navController.popBackStack() },
                onUploadSuccess = {
                    navController.navigate(Screen.DrawerScreen.Notes.route) {
                        popUpTo("upload_note") { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.DrawerScreen.Seniors.route) { Seniors(viewModel, navController) }
        composable(Screen.DrawerScreen.Societies.route) { Societies() }
        composable(Screen.DrawerScreen.Profile.route) { AccountView(viewModel) }
        composable(Screen.DrawerScreen.Download.route) { DownloadView(viewModel) }
        composable(Screen.DrawerScreen.PlacementCareer.dRoute) {
            PlacementCareerScreen(navController = navController, mainViewModel = viewModel)
        }
        composable("placement/add") {
            AddPlacementScreen(navController = navController)
        }
        composable("placement/{placementId}") { backStack ->
            val id = backStack.arguments?.getString("placementId")
            if (id != null) {
                PlacementDetailScreen(placementId = id, navController = navController)
            }
        }

        // Events screens
        composable(Screen.DrawerScreen.Events.route) {
            EventsListScreen(navController = navController)
        }
        composable("event/{eventId}") { backStack ->
            val eventId = backStack.arguments?.getString("eventId")
            EventDetailScreen(eventId = eventId, mainViewModel = viewModel, navController = navController)
        }
        composable("events/create") {
            CreateEventScreen(navController = navController)
        }


        // Admin Panel
        composable("admin/panel") {
            AdminPanelScreen(viewModel = viewModel, navController = navController)
        }

        // Senior screens
        composable("senior_detail/{seniorId}") { backStackEntry ->
            val seniorId = backStackEntry.arguments?.getString("seniorId")
            val senior = viewModel.getSenior(seniorId ?: "")
            if (senior != null) {
                SeniorDetailScreen(
                    senior = senior,
                    onBackClick = { navController.popBackStack() },
                    onEditClick = { navController.navigate("senior_edit/${senior.id}") },
                    onDeleteClick = {
                        val context = navController.context
                        viewModel.deleteSenior(senior.id) { success, error ->
                            if (success) {
                                navController.popBackStack()
                            } else {
                                Toast.makeText(context, "Delete failed: $error", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
        composable("senior_edit/{seniorId}") { backStackEntry ->
            val seniorId = backStackEntry.arguments?.getString("seniorId")
            val senior = viewModel.getSenior(seniorId ?: "")
            if (senior != null) {
                val context = LocalContext.current
                SeniorEditScreen(
                    viewModel = viewModel,
                    senior = senior,
                    onBackClick = { navController.popBackStack() },
                    onSaveClick = { updatedSenior ->
                        viewModel.updateSenior(updatedSenior) { success, error ->
                            if (success) {
                                navController.popBackStack()
                            } else {
                                Toast.makeText(context, "Update failed: $error", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
        composable("senior_add") {
            val context = LocalContext.current
            SeniorAddScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onAddClick = { newSenior ->
                    viewModel.addSenior(newSenior) { success, error ->
                        if (success) {
                            navController.popBackStack()
                        } else {
                            Toast.makeText(context, "Failed to add senior: $error", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }
    }
}