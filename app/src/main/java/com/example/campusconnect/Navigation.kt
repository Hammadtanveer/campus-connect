package com.example.campusconnect

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.campusconnect.ui.theme.AccountView
import com.example.campusconnect.ui.theme.DownloadView
import com.example.campusconnect.ui.theme.PlacementCareerScreen
import com.example.campusconnect.ui.theme.Seniors
import com.example.campusconnect.ui.theme.EventsListScreen
import com.example.campusconnect.ui.theme.EventDetailScreen
import com.example.campusconnect.ui.theme.CreateEventScreen
import com.example.campusconnect.ui.theme.MentorsListScreen
import com.example.campusconnect.ui.theme.MentorProfileScreen
import com.example.campusconnect.ui.theme.MyMentorshipScreen
import com.example.campusconnect.ui.theme.RequestDetailScreen

@Composable
fun Navigation(navController: NavController, viewModel: MainViewModel, pd: PaddingValues) {
    NavHost(
        navController = navController as NavHostController,
        startDestination = Screen.DrawerScreen.Profile.route,
        modifier = Modifier.padding(pd)
    ) {
        composable(Screen.DrawerScreen.Notes.route) { Notes() }
        composable(Screen.DrawerScreen.Seniors.route) { Seniors() }
        composable(Screen.DrawerScreen.Societies.route) { Societies() }
        composable(Screen.DrawerScreen.Profile.route) { AccountView(viewModel) }
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
    }
}