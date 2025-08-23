package com.example.campusconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.campusconnect.ui.MainView
import com.example.campusconnect.ui.theme.CampusConnectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CampusConnectTheme {
                // Shows the auth navigation (Login / Signup / Home)
                MainView()
            }
        }

    }
}
