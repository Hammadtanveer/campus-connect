package com.example.campusconnect.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch

/**
 * A lightweight, mobile-friendly side drawer component.
 *
 * Features
 * - Smooth slide-in/out animation
 * - Blurred background content (glassmorphism effect)
 * - Configurable blur radius, scrim alpha, animation duration and drawer width
 *
 * Usage:
 * SideDrawer(
 *   drawerWidth = 300.dp,
 *   blurRadius = 20.dp,
 *   scrimAlpha = 0.65f,
 * ) { openDrawer ->
 *   // Main app content, call openDrawer() to open the drawer
 * } drawerContent: {
 *   // Drawer content goes here
 * }
 */

@Composable
fun SideDrawer(
    drawerWidth: Dp = 300.dp,
    blurRadius: Dp = 20.dp,
    scrimAlpha: Float = 0.65f,
    animationMs: Int = 260,
    content: @Composable (openDrawer: () -> Unit) -> Unit,
    drawerContent: @Composable () -> Unit
) {
    val openState = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // animate drawer's horizontal offset: 0 when open, -drawerWidth when closed
    val targetOffset = if (openState.value) 0.dp else -drawerWidth
    val drawerOffsetDp by animateDpAsState(targetValue = targetOffset, animationSpec = tween(durationMillis = animationMs))

    Box(modifier = Modifier.fillMaxSize()) {
        // Main content: apply blur when drawer is open. We keep it interactable only when drawer is closed.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (openState.value) Modifier.blur(blurRadius) else Modifier)
        ) {
            content {
                scope.launch { openState.value = true }
            }
        }

        // Scrim overlay to dim and capture clicks; closes drawer when tapped
        if (openState.value) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable {
                        scope.launch { openState.value = false }
                    }
            )
        }

        // Drawer panel
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(drawerWidth)
                .zIndex(2f)
                .then(Modifier)
                .offset(x = drawerOffsetDp),
            contentAlignment = Alignment.TopStart
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    drawerContent()
                }
            }
        }
    }
}
