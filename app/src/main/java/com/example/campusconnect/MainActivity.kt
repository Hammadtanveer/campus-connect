package com.example.campusconnect

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.campusconnect.notifications.NotificationIntentRouter
import com.example.campusconnect.ui.screens.AuthGate
import com.example.campusconnect.ui.theme.CampusConnectTheme
import com.example.campusconnect.notifications.NotificationSubscriptionManager
import com.example.campusconnect.util.CloudinaryConfig
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        private var isFcmInitializedThisSession = false
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    private var pendingNotificationRoute by mutableStateOf<String?>(null)

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

        requestNotificationPermissionIfNeeded()
        initFcmOncePerSession()
        pendingNotificationRoute = NotificationIntentRouter.consumeRoute(intent)

        enableEdgeToEdge()
        setContent {
            val prefs = getSharedPreferences("campusconnect_prefs", MODE_PRIVATE)
            val systemDark = isSystemInDarkTheme()
            var darkTheme by rememberSaveable { mutableStateOf(prefs.getBoolean("pref_dark_mode", systemDark)) }

            CampusConnectTheme(darkTheme = darkTheme) {
                AuthGate(
                    darkTheme = darkTheme,
                    notificationRoute = pendingNotificationRoute,
                    onNotificationRouteConsumed = { pendingNotificationRoute = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        NotificationIntentRouter.consumeRoute(intent)?.let { route ->
            pendingNotificationRoute = route
        }
    }

    private fun initFcmOncePerSession() {
        if (isFcmInitializedThisSession) {
            return
        }
        isFcmInitializedThisSession = true

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    return@addOnCompleteListener
                }
            }

        FirebaseMessaging.getInstance().subscribeToTopic("all_students")
            .addOnCompleteListener { _ -> }

        NotificationSubscriptionManager.subscribeAllTopics()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}