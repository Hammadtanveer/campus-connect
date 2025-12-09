package com.example.campusconnect.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.campusconnect.ui.state.UiState

/**
 * Reusable error view component with retry functionality.
 *
 * Displays appropriate icon and message based on error type.
 */
@Composable
fun ErrorView(
    message: String,
    errorType: UiState.Error.ErrorType = UiState.Error.ErrorType.GENERIC,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error icon based on type
            Icon(
                imageVector = when (errorType) {
                    UiState.Error.ErrorType.NETWORK -> Icons.Default.Phone
                    UiState.Error.ErrorType.AUTH -> Icons.Default.Lock
                    UiState.Error.ErrorType.PERMISSION -> Icons.Default.Lock
                    UiState.Error.ErrorType.NOT_FOUND -> Icons.Default.Search
                    UiState.Error.ErrorType.VALIDATION -> Icons.Default.Warning
                    UiState.Error.ErrorType.SERVER_ERROR -> Icons.Default.Warning
                    UiState.Error.ErrorType.GENERIC -> Icons.Default.Info
                },
                contentDescription = "Error",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )

            // Error message
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Retry button
                if (onRetry != null) {
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry")
                    }
                }

                // Dismiss button
                if (onDismiss != null) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}

/**
 * Loading indicator component.
 */
@Composable
fun LoadingView(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator()
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Empty state component.
 */
@Composable
fun EmptyStateView(
    message: String,
    icon: ImageVector = Icons.Default.Info,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (actionLabel != null && onAction != null) {
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

/**
 * Helper composable to handle UiState automatically.
 */
@Composable
fun <T> UiStateHandler(
    state: UiState<T>,
    onRetry: (() -> Unit)? = null,
    loadingContent: @Composable () -> Unit = { LoadingView() },
    errorContent: @Composable (UiState.Error) -> Unit = { error ->
        ErrorView(
            message = error.message,
            errorType = error.errorType,
            onRetry = error.retry ?: onRetry
        )
    },
    successContent: @Composable (T) -> Unit
) {
    when (state) {
        is UiState.Loading -> loadingContent()
        is UiState.Error -> errorContent(state)
        is UiState.Success -> successContent(state.data)
    }
}

