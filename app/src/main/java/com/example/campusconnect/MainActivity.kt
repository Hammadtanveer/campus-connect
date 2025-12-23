package com.example.campusconnect

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.example.campusconnect.ui.screens.AuthGate
import com.example.campusconnect.ui.theme.CampusConnectTheme
import com.example.campusconnect.util.CloudinaryConfig
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verify Cloudinary Configuration
        if (CloudinaryConfig.isConfigured()) {
            // Config is present, but we don't show a toast for success to avoid spamming
            // If you want to verify, you can uncomment:
            // Toast.makeText(this, "Cloudinary Configured", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Cloudinary NOT Configured! Check credentials.", Toast.LENGTH_LONG).show()
        }

        enableEdgeToEdge()
        setContent {
            val prefs = getSharedPreferences("campusconnect_prefs", MODE_PRIVATE)
            val systemDark = isSystemInDarkTheme()
            var darkTheme by rememberSaveable { mutableStateOf(prefs.getBoolean("pref_dark_mode", systemDark)) }

            CampusConnectTheme(darkTheme = darkTheme) {
                AuthGate(darkTheme = darkTheme)
            }
        }
    }
}