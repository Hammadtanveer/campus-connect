package com.example.campusconnect.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.campusconnect.ui.components.DrawerItem
import com.example.campusconnect.MainViewModel
import com.example.campusconnect.Navigation
import com.example.campusconnect.Screen
import com.example.campusconnect.screenInDrawer
import kotlinx.coroutines.launch

// Configurable constants for drawer blur/scrim behavior
private val DRAWER_BLUR_OPEN: Dp = 20.dp    // stronger blur when drawer opens
private val DRAWER_BLUR_CLOSED: Dp = 0.dp
private const val DRAWER_SCRIM_ALPHA_OPEN = 0.65f // scrim darkness when drawer open
private const val DRAWER_SCRIM_ALPHA_CLOSED = 0f
private const val DRAWER_BLUR_ANIM_MS = 260 // animation duration in ms

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainView(viewModel: MainViewModel) {
    val scope = rememberCoroutineScope()
    val controller: NavController = rememberNavController()
    val navBackStackEntry by controller.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Identify standalone screens that should not show the main drawer/toolbar structure
    val isStandaloneScreen = currentRoute in listOf(
        "senior_add",
        "senior_detail/{seniorId}",
        "senior_edit/{seniorId}"
    )

    val context = LocalContext.current

    // Mentorship listener logic removed

    // Update ViewModel's current screen based on navigation route changes
    LaunchedEffect(currentRoute) {
        currentRoute?.let { viewModel.setCurrentScreenByRoute(it) }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Track whether drawer is open (drives blur/scrim)
    val isDrawerOpen by remember { derivedStateOf { drawerState.currentValue == DrawerValue.Open } }

    val blurDp by animateDpAsState(
        targetValue = if (isDrawerOpen) DRAWER_BLUR_OPEN else DRAWER_BLUR_CLOSED,
        animationSpec = tween(durationMillis = DRAWER_BLUR_ANIM_MS)
    )
    val scrimAlpha = if (isDrawerOpen) DRAWER_SCRIM_ALPHA_OPEN else DRAWER_SCRIM_ALPHA_CLOSED

    val currentScreenFromViewModel = viewModel.currentScreen
    val title = (currentScreenFromViewModel as? Screen.DrawerScreen)?.dTitle ?: "CampusConnect"

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isStandaloneScreen,
        // Use transparent scrim; we render our own to apply custom blur/scrim combo
        scrimColor = Color.Transparent,
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
        // Wrap scaffold inside a Box so we can overlay our own scrim
        Box(modifier = Modifier.fillMaxSize()) {
            // Blur the whole main content when drawer is open
            Box(modifier = Modifier
                .fillMaxSize()
                .blur(blurDp)
            ) {
                Scaffold(
                    topBar = {
                        if (!isStandaloneScreen) {
                            TopAppBar(
                                title = { Text(title) },
                                actions = {
                                    IconButton(onClick = { /* placeholder for future actions */ }) {
                                        Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More options")
                                    }
                                },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(
                                            imageVector = Icons.Filled.Menu,
                                            contentDescription = "Open navigation drawer"
                                        )
                                    }
                                }
                            )
                        }
                    }
                ) { paddingValues ->
                    Navigation(navController = controller, viewModel = viewModel, pd = paddingValues)
                }
            }

            // Non-blurred scrim overlay when drawer is open
            if (isDrawerOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = scrimAlpha))
                        .clickable { scope.launch { drawerState.close() } }
                ) { /* tap to close */ }
            }
        }
    }
}
