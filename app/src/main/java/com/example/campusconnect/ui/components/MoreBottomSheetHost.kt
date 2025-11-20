package com.example.campusconnect.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.campusconnect.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreBottomSheetHost(
    viewModel: MainViewModel,
    isSheetVisible: MutableState<Boolean> = remember { mutableStateOf(false) },
    content: (@Composable (openMore: () -> Unit) -> Unit)
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Render the main screen content, passing a lambda to open the sheet
    content { isSheetVisible.value = true }

    if (isSheetVisible.value) {
        ModalBottomSheet(
            onDismissRequest = { isSheetVisible.value = false },
            sheetState = sheetState
        ) {
            MoreBottomSheet(
                onSignOut = {
                    viewModel.signOut()
                    isSheetVisible.value = false
                }
            )
        }
    }
}
