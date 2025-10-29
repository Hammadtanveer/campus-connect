package com.example.campusconnect

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MoreBottomSheet(modifier: Modifier, viewModel: MainViewModel) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(colorScheme.primary)
    ) {
        Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = modifier.padding(16.dp)) {
                Icon(
                    modifier = Modifier.padding(end = 8.dp),
                    painter = painterResource(id = R.drawable.baseline_settings_24),
                    contentDescription = "Settings",
                    tint = colorScheme.onPrimary
                )
                Text(text = "Settings", fontSize = 20.sp, color = colorScheme.onPrimary)
            }
            Row(modifier = modifier.padding(16.dp)) {
                Icon(
                    modifier = Modifier.padding(end = 8.dp),
                    painter = painterResource(id = R.drawable.baseline_share_24),
                    contentDescription = "Share",
                    tint = colorScheme.onPrimary
                )
                Text(
                    text = "Share",
                    fontSize = 20.sp,
                    color = colorScheme.onPrimary
                )
            }
            Row(modifier = modifier.padding(16.dp)) {
                Icon(
                    modifier = Modifier.padding(end = 8.dp),
                    painter = painterResource(id = R.drawable.outline_help_24),
                    contentDescription = "Help",
                    tint = colorScheme.onPrimary
                )
                Text(
                    text = "Help",
                    fontSize = 20.sp,
                    color = colorScheme.onPrimary
                )
            }
            // Sign out option to return to Welcome screen
            Row(
                modifier = modifier
                    .padding(16.dp)
                    .clickable { viewModel.signOut() }
            ) {
                Icon(
                    modifier = Modifier.padding(end = 8.dp),
                    painter = painterResource(id = R.drawable.outline_account_circle_24),
                    contentDescription = "Sign Out",
                    tint = colorScheme.onPrimary
                )
                Text(
                    text = "Sign Out",
                    fontSize = 20.sp,
                    color = colorScheme.onPrimary
                )
            }
        }
    }
}