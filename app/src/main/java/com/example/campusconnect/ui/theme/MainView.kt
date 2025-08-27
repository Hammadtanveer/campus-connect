package com.example.campusconnect.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.campusconnect.MainViewModel
import com.example.campusconnect.MoreBottomSheet
import com.example.campusconnect.Navigation
import com.example.campusconnect.R
import com.example.campusconnect.Screen
import com.example.campusconnect.screenInBottom
import com.example.campusconnect.screenInDrawer
import com.example.campusconnect.DrawerItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainView(viewModel: MainViewModel) {
    val scaffoldState: ScaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()
    val controller: NavController = rememberNavController()
    val navBackStackEntry by controller.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    // Keep currentScreen in sync with navigation route
    LaunchedEffect(currentRoute) {
        currentRoute?.let { viewModel.setCurrentScreenByRoute(it) }
    }
    val isSheetFullScreen by remember { mutableStateOf(false) }
    val modifier = if (isSheetFullScreen) Modifier else Modifier
    val currentScreen = viewModel.currentScreen.value
    val titleState = remember { mutableStateOf(currentScreen.title) }

    val currentIconRes = remember(currentRoute) {
        screenInBottom.find { it.bRoute == currentRoute }?.icon
            ?: screenInDrawer.find { it.dRoute == currentRoute }?.icon
            ?: R.drawable.outline_account_circle_24
    }
    val currentTitleForA11y = remember(currentRoute) {
        screenInBottom.find { it.bRoute == currentRoute }?.bTitle
            ?: screenInDrawer.find { it.dRoute == currentRoute }?.dtitle
            ?: titleState.value
    }

    val modalSheetState = androidx.compose.material.rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmValueChange = { it != ModalBottomSheetValue.HalfExpanded }
    )
    val roundedCornerRadius = if (isSheetFullScreen) 0.dp else 12.dp

    val bottomBar: @Composable () -> Unit = {
        NavigationBar(Modifier.wrapContentSize()) {
            screenInBottom.forEach { item ->
                val isSelected = currentRoute == item.bRoute
                val tint = if (isSelected) Color.Black else Color.Black
                NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            controller.navigate(item.bRoute)
                            titleState.value = item.bTitle
                            viewModel.setCurrentScreenByRoute(item.bRoute)
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = item.icon),
                            contentDescription = item.bTitle,
                            tint = tint
                        )
                    },
                    label = { Text(item.bTitle, color = tint) }
                )
            }
        }
    }

    ModalBottomSheetLayout(
        sheetState = modalSheetState,
        sheetShape = RoundedCornerShape(topStart = roundedCornerRadius, topEnd = roundedCornerRadius),
        sheetContent = { MoreBottomSheet(modifier = modifier) }
    ) {
        Scaffold(
            scaffoldState = scaffoldState,
            bottomBar = bottomBar,
            topBar = {
                TopAppBar(
                    title = { Text(titleState.value) },
                    actions = {
                        IconButton(onClick = {
                            scope.launch {
                                if (modalSheetState.isVisible) modalSheetState.hide()
                                else modalSheetState.show()
                            }
                        }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { scaffoldState.drawerState.open() }
                        }) {
                            Icon(
                                painter = painterResource(id = currentIconRes),
                                contentDescription = currentTitleForA11y
                            )
                        }
                    }
                )
            },
            drawerContent = {
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = 56.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    )
                ) {
                    items(screenInDrawer) { item ->
                        DrawerItem(selected = currentRoute == item.dRoute, item = item) {
                            scope.launch { scaffoldState.drawerState.close() }
                            controller.navigate(item.dRoute)
                            titleState.value = item.dtitle
                            viewModel.setCurrentScreenByRoute(item.dRoute)
                        }
                    }
                }
            }
        ) { paddingValues ->
            Navigation(navController = controller, viewModel = viewModel, pd = paddingValues)
        }
    }
}