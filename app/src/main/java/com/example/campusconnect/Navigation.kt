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
import com.example.campusconnect.ui.theme.Seniors

@Composable
fun Navigation(navController: NavController, viewModel: MainViewModel, pd: PaddingValues) {
    NavHost(
        navController = navController as NavHostController,
        startDestination = Screen.DrawerScreen.Profile.route,
        modifier = Modifier.padding(pd)
    ) {
        composable(Screen.BottomScreen.Notes.bRoute) { Notes() }
        composable(Screen.BottomScreen.Seniors.bRoute) { Seniors() }
        composable(Screen.BottomScreen.Societies.bRoute) { Societies() }
        composable(Screen.DrawerScreen.Profile.route) { AccountView(viewModel) }
        composable(Screen.DrawerScreen.Download.route) { DownloadView(viewModel) }
    }
}