package com.example.campusconnect.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.campusconnect.DrawerItem
import com.example.campusconnect.MainViewModel
import com.example.campusconnect.MoreBottomSheet
import com.example.campusconnect.Navigation
import com.example.campusconnect.Screen
import com.example.campusconnect.screenInDrawer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainView(viewModel: MainViewModel) {
    val scaffoldState: ScaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()
    val controller: NavController = rememberNavController()
    val navBackStackEntry by controller.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Update ViewModel's current screen based on navigation route changes
    LaunchedEffect(currentRoute) {
        currentRoute?.let { viewModel.setCurrentScreenByRoute(it) }
    }

    val currentScreenFromViewModel = viewModel.currentScreen.value
    // Determine title from ViewModel, default if not a DrawerScreen or title is null
    val title = (currentScreenFromViewModel as? Screen.DrawerScreen)?.dTitle ?: "CampusConnect"

    val isSheetFullScreen by remember { mutableStateOf(false) }
    val modifier = if (isSheetFullScreen) Modifier.fillMaxWidth() else Modifier

    val modalSheetState = androidx.compose.material.rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmValueChange = { it != ModalBottomSheetValue.HalfExpanded } // Prevent half-expanded state
    )
    val roundedCornerRadius = if (isSheetFullScreen) 0.dp else 12.dp

    ModalBottomSheetLayout(
        sheetState = modalSheetState,
        sheetShape = RoundedCornerShape(topStart = roundedCornerRadius, topEnd = roundedCornerRadius),
        sheetContent = { MoreBottomSheet(modifier = modifier) }
    ) {
        Scaffold(
            scaffoldState = scaffoldState,
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    actions = {
                        IconButton(onClick = {
                            scope.launch { // Toggle bottom sheet visibility
                                if (modalSheetState.isVisible) modalSheetState.hide()
                                else modalSheetState.show()
                            }
                        }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More options")
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { scaffoldState.drawerState.open() } // Open drawer
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Open navigation drawer"
                            )
                        }
                    }
                )
            },
            drawerContent = {

                LazyColumn(
                    modifier = Modifier.padding(top = 24.dp) // Top padding for drawer items
                ) {
                    items(screenInDrawer) { item ->
                        DrawerItem(
                            selected = currentRoute == item.dRoute,
                            item = item,
                            onDrawerItemClicked = {
                                scope.launch { scaffoldState.drawerState.close() }
                                if (currentRoute != item.dRoute) { // Avoid re-navigating to the same screen
                                    controller.navigate(item.dRoute) {
                                        // Standard navigation pattern for drawer items
                                        popUpTo(controller.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        ) { paddingValues ->
            // Main navigation host for app screens
            Navigation(navController = controller, viewModel = viewModel, pd = paddingValues)
        }
    }
}
