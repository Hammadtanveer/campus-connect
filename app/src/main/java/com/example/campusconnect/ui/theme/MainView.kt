package com.example.campusconnect.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.campusconnect.DrawerItem
import com.example.campusconnect.MainViewModel
import com.example.campusconnect.Navigation
import com.example.campusconnect.Screen
import com.example.campusconnect.screenInDrawer
import kotlinx.coroutines.launch
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.draw.blur
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.MaterialTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainView(viewModel: MainViewModel) {
    val scope = rememberCoroutineScope()
    val controller: NavController = rememberNavController()
    val navBackStackEntry by controller.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val context = LocalContext.current

    // Start/stop pending requests listener when authentication/profile changes
    LaunchedEffect(viewModel.userProfile?.id) {
        if (viewModel.userProfile?.id != null) {
            // start listener with app context
            viewModel.startPendingRequestsListener(context)
        } else {
            viewModel.stopPendingRequestsListener()
        }
    }

    // Update ViewModel's current screen based on navigation route changes
    LaunchedEffect(currentRoute) {
        currentRoute?.let { viewModel.setCurrentScreenByRoute(it) }
    }

    val currentScreenFromViewModel = viewModel.currentScreen
    val title = (currentScreenFromViewModel as? Screen.DrawerScreen)?.dTitle ?: "CampusConnect"

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Track whether drawer is open (drives blur/scrim)
    val isDrawerOpen by derivedStateOf { drawerState.currentValue == DrawerValue.Open }
    val blurDp by animateDpAsState(targetValue = if (isDrawerOpen) 12.dp else 0.dp)
    val scrimAlpha = if (isDrawerOpen) 0.35f else 0f

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            LazyColumn(
                modifier = Modifier.padding(top = 24.dp)
            ) {
                items(screenInDrawer) { item ->
                    DrawerItem(
                        selected = currentRoute == item.dRoute,
                        item = item,
                        badgeCount = when (item) {
                            Screen.DrawerScreen.Events -> viewModel.unreadEventNotifications
                            Screen.DrawerScreen.Mentorship -> viewModel.pendingMentorshipRequests
                            else -> 0
                        },
                        onDrawerItemClicked = {
                            scope.launch { drawerState.close() }
                            if (currentRoute != item.dRoute) {
                                controller.navigate(item.dRoute) {
                                    popUpTo(controller.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) {
        // Wrap scaffold + navigation inside a Box so we can overlay a blurred scrim when drawer is open
        Box(modifier = Modifier.fillMaxSize()) {
            // Simple Scaffold for main content
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(title) },
                        actions = {
                            IconButton(onClick = {
                                // placeholder for future actions
                            }) {
                                Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More options")
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                scope.launch { drawerState.open() }
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = "Open navigation drawer"
                                )
                            }
                        }
                    )
                }
            ) { paddingValues ->
                // Apply blur to navigation content itself so underlying UI is blurred when drawer opens
                Navigation(navController = controller, viewModel = viewModel, pd = paddingValues)
            }

            // Blurred scrim overlay when drawer is open
            if (isDrawerOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = scrimAlpha))
                        .clickable { scope.launch { drawerState.close() } }
                        .blur(blurDp)
                ) {
                    // Clicking the scrim should close the drawer
                }
            }
        }
    }
}