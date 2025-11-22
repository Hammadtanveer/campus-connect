package com.example.campusconnect

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.campusconnect.ui.components.Notes
import com.example.campusconnect.ui.screens.AccountView
import com.example.campusconnect.ui.screens.DownloadView
import com.example.campusconnect.ui.screens.PlacementCareerScreen
import com.example.campusconnect.ui.screens.Seniors
import com.example.campusconnect.ui.screens.Societies
import com.example.campusconnect.ui.screens.EventsListScreen
import com.example.campusconnect.ui.screens.EventDetailScreen
import com.example.campusconnect.ui.screens.CreateEventScreen
import com.example.campusconnect.ui.screens.MentorsListScreen
import com.example.campusconnect.ui.screens.MentorProfileScreen
import com.example.campusconnect.ui.screens.MyMentorshipScreen
import com.example.campusconnect.ui.screens.RequestDetailScreen
import com.example.campusconnect.ui.screens.AdminPanelScreen

@Composable
fun Navigation(navController: NavController, viewModel: MainViewModel, pd: PaddingValues) {
    NavHost(
        navController = navController as NavHostController,
        startDestination = Screen.DrawerScreen.Profile.route,
        modifier = Modifier.padding(pd)
    ) {
        composable(Screen.DrawerScreen.Notes.route) { Notes(viewModel) }
        composable(Screen.DrawerScreen.Seniors.route) { Seniors(viewModel) }
        composable(Screen.DrawerScreen.Societies.route) { Societies() }
        composable(Screen.DrawerScreen.Profile.route) { AccountView(viewModel, navController) }
        composable(Screen.DrawerScreen.Download.route) { DownloadView(viewModel) }
        composable(Screen.DrawerScreen.PlacementCareer.dRoute) {
            PlacementCareerScreen(viewModel = viewModel)
        }

        // Events screens
        composable(Screen.DrawerScreen.Events.route) {
            EventsListScreen(viewModel = viewModel, navController = navController)
        }
        composable("event/{eventId}") { backStack ->
            val eventId = backStack.arguments?.getString("eventId")
            EventDetailScreen(eventId = eventId, viewModel = viewModel, navController = navController)
        }
        composable("events/create") {
            CreateEventScreen(viewModel = viewModel, navController = navController)
        }

        // Mentors screens
        composable(Screen.DrawerScreen.Mentors.route) {
            MentorsListScreen(viewModel = viewModel, navController = navController)
        }
        composable("mentor/{mentorId}") { backStack ->
            val mentorId = backStack.arguments?.getString("mentorId")
            MentorProfileScreen(mentorId = mentorId, viewModel = viewModel, navController = navController)
        }

        // Mentorship management
        composable(Screen.DrawerScreen.Mentorship.dRoute) {
            MyMentorshipScreen(viewModel = viewModel, navController = navController)
        }
        composable("mentorship/request/{requestId}") { backStack ->
            val requestId = backStack.arguments?.getString("requestId")
            RequestDetailScreen(requestId = requestId, viewModel = viewModel, navController = navController)
        }

        // Admin Panel
        composable("admin/panel") {
            AdminPanelScreen(viewModel = viewModel, navController = navController)
        }
    }
}