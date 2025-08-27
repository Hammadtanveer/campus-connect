package com.example.campusconnect.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.campusconnect.MainViewModel
import com.example.campusconnect.R

@Composable
fun AccountView(viewModel: MainViewModel) {
    val userProfile = viewModel.userProfile.value
    // `AuthGate` ensures non-null; guard remains for safety.
    if (userProfile == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(56.dp)
                        .padding(end = 8.dp)
                )
                Column {
                    Text(userProfile.displayName.ifBlank { "Student" })
                    Text(userProfile.email)
                }
            }
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null
                )
            }
        }
        if (userProfile.department.isNotEmpty() || userProfile.year.isNotEmpty()) {
            Row(modifier = Modifier.padding(top = 16.dp)) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_info_24),
                    contentDescription = "Education Info",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column {
                    Text(userProfile.department.ifBlank { "B.Tech CSE" })
                    Text(userProfile.year.ifBlank { "4th Year" })
                }
            }
        }
        Button(
            onClick = { viewModel.signOut() },
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text("Sign Out")
        }
        Divider(modifier = Modifier.padding(top = 16.dp))
    }
}