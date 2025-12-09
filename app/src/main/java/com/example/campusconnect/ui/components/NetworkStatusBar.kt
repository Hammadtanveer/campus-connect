package com.example.campusconnect.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.campusconnect.network.ConnectivityManager

/**
 * Network status bar that appears when offline.
 */
@Composable
fun NetworkStatusBar(
    connectivityManager: ConnectivityManager,
    modifier: Modifier = Modifier
) {
    val isConnected by connectivityManager.isConnected.collectAsState()
    val connectionType by connectivityManager.connectionType.collectAsState()

    AnimatedVisibility(
        visible = !isConnected,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.error,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "No connection",
                    tint = MaterialTheme.colorScheme.onError
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "You're offline. Changes will sync when connected.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onError
                )
            }
        }
    }

    // Optional: Show connection type indicator when online
    AnimatedVisibility(
        visible = isConnected && connectionType == ConnectivityManager.ConnectionType.CELLULAR,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Using cellular data",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

/**
 * Inline network status indicator.
 */
@Composable
fun NetworkStatusIndicator(
    connectivityManager: ConnectivityManager,
    modifier: Modifier = Modifier
) {
    val isConnected by connectivityManager.isConnected.collectAsState()

    if (!isConnected) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = "Offline",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Offline",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

