package com.example.campusconnect.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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

    if (userProfile == null) {
        // If not logged in, show login prompt
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("You need to sign in to view your profile")
            Button(
                onClick = { viewModel.showAuthDialog() },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Sign In")
            }
        }
    } else {
        // Display user profile
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
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Account",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column {
                        Text(userProfile.displayName)
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

            // Additional profile information
            if (userProfile.department.isNotEmpty() || userProfile.year.isNotEmpty()) {
                Row(modifier = Modifier.padding(top = 16.dp)) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_info_24),
                        contentDescription = "Education Info",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column {
                        if (userProfile.department.isNotEmpty()) {
                            Text(userProfile.department)
                        } else {
                            Text("B.Tech CSE") // Default department
                        }
                        if (userProfile.year.isNotEmpty()) {
                            Text(userProfile.year)
                        } else {
                            Text("4th Year") // Default year
                        }
                    }
                }
            }

            // Sign out button
            Button(
                onClick = { viewModel.signOut() },
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text("Sign Out")
            }

            Divider(modifier = Modifier.padding(top = 16.dp))
        }
    }
}