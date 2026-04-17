package com.example.campusconnect

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.campusconnect.security.canAddSenior
import com.example.campusconnect.security.canUpdateSenior
import com.example.campusconnect.security.canUploadNotes
import com.example.campusconnect.ui.placement.AddPlacementScreen
import com.example.campusconnect.ui.placement.PlacementDetailScreen
import com.example.campusconnect.ui.screens.AccountView
import com.example.campusconnect.ui.screens.AdminPanelScreen
import com.example.campusconnect.ui.screens.AdminAnalyticsScreen
import com.example.campusconnect.ui.screens.AdminContentModerationScreen
import com.example.campusconnect.ui.screens.AdminSendNotificationScreen
import com.example.campusconnect.ui.screens.AdminActivityLogScreen
import com.example.campusconnect.ui.screens.AdminUsersScreen
import com.example.campusconnect.ui.screens.AdminUserPermissionDetailScreen
import com.example.campusconnect.ui.screens.CreateEventScreen
import com.example.campusconnect.ui.screens.CreateSocietyEventScreen
import com.example.campusconnect.ui.screens.DownloadView
import com.example.campusconnect.ui.screens.EventDetailScreen
import com.example.campusconnect.ui.screens.EventsListScreen
import com.example.campusconnect.ui.screens.NotesScreen
import com.example.campusconnect.ui.screens.PlacementCareerScreen
import com.example.campusconnect.ui.screens.Seniors
import com.example.campusconnect.ui.screens.Societies
import com.example.campusconnect.ui.screens.SocietyEventDetailScreen
import com.example.campusconnect.ui.screens.SocietyEventsScreen
import com.example.campusconnect.ui.screens.UploadNoteScreen
import com.example.campusconnect.ui.senior.SeniorAddScreen
import com.example.campusconnect.ui.senior.SeniorDetailScreen
import com.example.campusconnect.ui.senior.SeniorEditScreen

@Composable
fun Navigation(navController: NavController, viewModel: MainViewModel, pd: PaddingValues) {
    val profile by viewModel.sessionProfileFlow.collectAsStateWithLifecycle(null)
    NavHost(
        navController = navController as NavHostController,
        startDestination = Screen.DrawerScreen.Profile.route,
        modifier = Modifier.padding(pd)
    ) {
        composable(Screen.DrawerScreen.Notes.route) {
            NotesScreen(navController = navController, mainViewModel = viewModel)
        }
        composable("upload_note") {
            val context = LocalContext.current
            val canUpload = profile?.canUploadNotes() == true

            if (!canUpload) {
                LaunchedEffect(Unit) {
                    Toast.makeText(context, "Only admin and super admin can upload notes", Toast.LENGTH_LONG).show()
                    navController.popBackStack()
                }
            } else {
                UploadNoteScreen(
                    mainViewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onUploadSuccess = {
                        navController.navigate(Screen.DrawerScreen.Notes.route) {
                            popUpTo("upload_note") { inclusive = true }
                        }
                    },
                    onOpenAdminPanel = { navController.navigate("admin/panel") }
                )
            }
        }
        composable(Screen.DrawerScreen.Seniors.route) { Seniors(viewModel, navController) }
        composable(Screen.DrawerScreen.Societies.route) { Societies(navController) }

        composable("societyEvents/{societyId}/{societyName}") { backStackEntry ->
            val societyId = backStackEntry.arguments?.getString("societyId") ?: ""
            val encodedName = backStackEntry.arguments?.getString("societyName") ?: ""
            val societyName = Uri.decode(encodedName)
            if (societyId.isBlank() || societyName.isBlank()) {
                navController.popBackStack()
            } else {
                SocietyEventsScreen(
                    societyId = societyId,
                    societyName = societyName,
                    navController = navController
                )
            }
        }

        composable("societyEvent/create/{societyId}/{societyName}") { backStackEntry ->
            val societyId = backStackEntry.arguments?.getString("societyId") ?: ""
            val encodedName = backStackEntry.arguments?.getString("societyName") ?: ""
            val societyName = Uri.decode(encodedName)
            if (societyId.isBlank() || societyName.isBlank()) {
                navController.popBackStack()
            } else {
                CreateSocietyEventScreen(
                    societyId = societyId,
                    societyName = societyName,
                    onEventSaved = { navController.popBackStack() }
                )
            }
        }

        composable("societyEvent/edit/{societyId}/{eventId}") { backStackEntry ->
            val societyId = backStackEntry.arguments?.getString("societyId") ?: ""
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            if (societyId.isBlank() || eventId.isBlank()) {
                navController.popBackStack()
            } else {
                CreateSocietyEventScreen(
                    societyId = societyId,
                    societyName = "",
                    eventId = eventId,
                    onEventSaved = { navController.popBackStack() }
                )
            }
        }

        composable("societyEvent/{societyId}/{eventId}") { backStackEntry ->
            val societyId = backStackEntry.arguments?.getString("societyId") ?: ""
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            if (societyId.isNotBlank() && eventId.isNotBlank()) {
                SocietyEventDetailScreen(
                    societyId = societyId,
                    eventId = eventId,
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate("societyEvent/edit/$societyId/$id") }
                )
            }
        }

        composable(Screen.DrawerScreen.Profile.route) { AccountView(viewModel) }
        composable(Screen.DrawerScreen.Download.route) { DownloadView(viewModel) }
        composable(Screen.DrawerScreen.PlacementCareer.dRoute) {
            PlacementCareerScreen(navController = navController, mainViewModel = viewModel)
        }
        composable("placement/add") {
            AddPlacementScreen(navController = navController)
        }
        composable("placement/edit/{placementId}") { backStack ->
            val id = backStack.arguments?.getString("placementId")
            if (id.isNullOrBlank()) {
                navController.popBackStack()
            } else {
                AddPlacementScreen(navController = navController, placementId = id)
            }
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
        composable("events/edit/{eventId}") { backStack ->
            val eventId = backStack.arguments?.getString("eventId")
            if (eventId.isNullOrBlank()) {
                navController.popBackStack()
            } else {
                CreateEventScreen(navController = navController, eventId = eventId)
            }
        }

        // Admin Panel
        composable("admin/panel") {
            AdminPanelScreen(viewModel = viewModel, navController = navController)
        }
        composable("admin/analytics") {
            AdminAnalyticsScreen(navController = navController)
        }
        composable("admin/content-moderation") {
            AdminContentModerationScreen(navController = navController)
        }
        composable("admin/send-notification") {
            AdminSendNotificationScreen(navController = navController)
        }
        composable("admin/activity-log") {
            AdminActivityLogScreen(navController = navController)
        }
        composable("admin/users") {
            AdminUsersScreen(navController = navController)
        }
        composable("admin/users/{userId}") { backStack ->
            val encodedUserId = backStack.arguments?.getString("userId")
            val userId = if (encodedUserId.isNullOrBlank()) null else Uri.decode(encodedUserId)
            if (userId.isNullOrBlank()) {
                navController.popBackStack()
            } else {
                AdminUserPermissionDetailScreen(navController = navController, userId = userId)
            }
        }

        // Senior screens
        composable("senior_detail/{seniorId}") { backStackEntry ->
            val seniorId = backStackEntry.arguments?.getString("seniorId")
            val senior = viewModel.getSenior(seniorId ?: "")
            val canManageSenior = profile?.canUpdateSenior() == true
            if (senior != null) {
                SeniorDetailScreen(
                    senior = senior,
                    viewModel = viewModel,
                    canManageSenior = canManageSenior,
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
                    },
                    onOpenAdminPanel = { navController.navigate("admin/panel") }
                )
            }
        }
        composable("senior_edit/{seniorId}") { backStackEntry ->
            val seniorId = backStackEntry.arguments?.getString("seniorId")
            val senior = viewModel.getSenior(seniorId ?: "")
            val context = LocalContext.current
            val canManageSenior = profile?.canUpdateSenior() == true

            if (!canManageSenior) {
                LaunchedEffect(Unit) {
                    Toast.makeText(context, "You don't have permission to edit seniors", Toast.LENGTH_LONG).show()
                    navController.popBackStack()
                }
            } else if (senior != null) {
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
                    },
                    onOpenAdminPanel = { navController.navigate("admin/panel") }
                )
            }
        }
        composable("senior_add") {
            val context = LocalContext.current
            val canAddSenior = profile?.canAddSenior() == true

            if (!canAddSenior) {
                LaunchedEffect(Unit) {
                    Toast.makeText(context, "Only admin and super admin can add seniors", Toast.LENGTH_LONG).show()
                    navController.popBackStack()
                }
            } else {
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
                    },
                    onOpenAdminPanel = { navController.navigate("admin/panel") }
                )
            }
        }
    }
}
