package com.example.campusconnect.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages network connectivity state.
 *
 * Provides real-time updates on network availability.
 */
@Singleton
class ConnectivityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ConnectivityManager"
    }

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isConnected = MutableStateFlow(isNetworkAvailable())
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionType = MutableStateFlow(ConnectionType.NONE)
    val connectionType: StateFlow<ConnectionType> = _connectionType.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Network available")
            _isConnected.value = true
            updateConnectionType()
        }

        override fun onLost(network: Network) {
            Log.d(TAG, "Network lost")
            _isConnected.value = false
            _connectionType.value = ConnectionType.NONE
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            Log.d(TAG, "Network capabilities changed")
            updateConnectionType()
        }
    }

    init {
        registerNetworkCallback()
    }

    /**
     * Check if network is currently available.
     */
    fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Check if connected to WiFi.
     */
    fun isWifiConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * Check if connected to cellular.
     */
    fun isCellularConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    /**
     * Get current connection type.
     */
    fun getCurrentConnectionType(): ConnectionType {
        if (!isNetworkAvailable()) return ConnectionType.NONE

        return when {
            isWifiConnected() -> ConnectionType.WIFI
            isCellularConnected() -> ConnectionType.CELLULAR
            else -> ConnectionType.OTHER
        }
    }

    private fun registerNetworkCallback() {
        try {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build()

            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
            Log.d(TAG, "Network callback registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
        }
    }

    private fun updateConnectionType() {
        _connectionType.value = getCurrentConnectionType()
    }

    fun unregister() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            Log.d(TAG, "Network callback unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister network callback", e)
        }
    }

    enum class ConnectionType {
        WIFI,
        CELLULAR,
        OTHER,
        NONE
    }
}

