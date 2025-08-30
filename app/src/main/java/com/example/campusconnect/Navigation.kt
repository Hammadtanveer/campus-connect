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
import com.example.campusconnect.ui.theme.OnlineMeetingsEventsScreen
import com.example.campusconnect.ui.theme.PlacementCareerScreen
import com.example.campusconnect.ui.theme.Seniors

@Composable
fun Navigation(navController: NavController, viewModel: MainViewModel, pd: PaddingValues) {
    NavHost(
        navController = navController as NavHostController,
        startDestination = Screen.DrawerScreen.Profile.route, // Default starting screen
        modifier = Modifier.padding(pd) // Apply padding from Scaffold
    ) {
        composable(Screen.DrawerScreen.Notes.route) { Notes() }
        composable(Screen.DrawerScreen.Seniors.route) { Seniors() }
        composable(Screen.DrawerScreen.Societies.route) { Societies() }
        composable(Screen.DrawerScreen.Profile.route) { AccountView(viewModel) }
        composable(Screen.DrawerScreen.Download.route) { DownloadView(viewModel) }

        // Routes for the new modules
        composable(Screen.DrawerScreen.PlacementCareer.dRoute) {
            PlacementCareerScreen(viewModel = viewModel)
        }
        composable(Screen.DrawerScreen.OnlineMeetingsEvents.dRoute) {
            OnlineMeetingsEventsScreen(viewModel = viewModel)
        }
    }
}
