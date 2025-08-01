package com.example.nfccardtaptopayv101.ui.viewmodel.ble

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

private const val BASE_URL = "https://nfc-fastapi-backend.onrender.com"

sealed class BleScanningState {
    object Idle : BleScanningState()
    object Starting : BleScanningState()
    object Scanning : BleScanningState()
    object Stopped : BleScanningState()
    data class Error(val message: String) : BleScanningState()
}

data class NearbyUser(
    val userId: String,
    val userName: String = "",
    val userProfileImageUrl: String = "",
    val rssi: Int = 0,
    val lastSeen: Long = System.currentTimeMillis(),
    val isLoadingData: Boolean = true
)

data class Screen2UiState(
    val scanningState: BleScanningState = BleScanningState.Idle,
    val hasRequiredPermissions: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val statusMessage: String = "Preparing to scan...",
    val nearbyUsers: List<NearbyUser> = emptyList(),
    val isScanning: Boolean = false
)

class Screen2ViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        private const val TAG = "Screen2ViewModel"
        // Use the same company ID as Screen3 to filter advertisements
        private const val CUSTOM_COMPANY_ID = 0x1269
        private const val USER_TIMEOUT_MS = 30000L // 30 seconds timeout for users
        private const val SCAN_PERIOD_MS = 10000L // 10 seconds scan period
    }

    private val context = getApplication<Application>()
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var bluetoothLeScanner: BluetoothLeScanner? = null

    // HTTP client for API calls
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val _uiState = MutableStateFlow(Screen2UiState())
    val uiState: StateFlow<Screen2UiState> = _uiState.asStateFlow()

    // Keep track of discovered users and their data
    private val discoveredUsers = ConcurrentHashMap<String, NearbyUser>()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let { scanResult ->
                processScanResult(scanResult)
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results?.forEach { scanResult ->
                processScanResult(scanResult)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            val errorMessage = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "Scanning already started"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "App registration failed"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "Scanning not supported"
                SCAN_FAILED_INTERNAL_ERROR -> "Internal scanning error"
                SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> "Hardware resources unavailable"
                else -> "Unknown scanning error: $errorCode"
            }
            Log.e(TAG, "BLE Scan failed: $errorMessage")
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(
                    scanningState = BleScanningState.Error(errorMessage),
                    statusMessage = "Scanning failed: $errorMessage",
                    isScanning = false
                )
            }
        }
    }

    init {
        checkPermissionsAndBluetooth()
        startPeriodicCleanup()
    }

    private fun processScanResult(scanResult: ScanResult) {
        try {
            val manufacturerData = scanResult.scanRecord?.manufacturerSpecificData
            val deviceAddress = scanResult.device?.address ?: return
            val rssi = scanResult.rssi

            Log.d(TAG, "Scan result from: $deviceAddress, RSSI: $rssi")

            // Check if this advertisement contains our custom company ID
            val ourData = manufacturerData?.get(CUSTOM_COMPANY_ID)
            if (ourData != null && ourData.size >= 2) {
                // Decode user ID from manufacturer data (2 bytes)
                val userId = ((ourData[0].toInt() and 0xFF) shl 8) or (ourData[1].toInt() and 0xFF)
                val userIdString = userId.toString()

                Log.d(TAG, "Found user device: $deviceAddress, UserID: $userIdString, RSSI: $rssi")

                viewModelScope.launch {
                    updateOrAddUser(userIdString, rssi, deviceAddress)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing scan result", e)
        }
    }

    private suspend fun updateOrAddUser(userId: String, rssi: Int, deviceAddress: String) {
        val currentTime = System.currentTimeMillis()
        val existingUser = discoveredUsers[userId]

        if (existingUser != null) {
            // Update existing user
            val updatedUser = existingUser.copy(
                rssi = rssi,
                lastSeen = currentTime
            )
            discoveredUsers[userId] = updatedUser
        } else {
            // Add new user
            val newUser = NearbyUser(
                userId = userId,
                rssi = rssi,
                lastSeen = currentTime,
                isLoadingData = true
            )
            discoveredUsers[userId] = newUser

            // Fetch user data in background
            fetchUserData(userId)
        }

        // Update UI with current list sorted by RSSI (closest first)
        updateNearbyUsersList()
    }

    private fun updateNearbyUsersList() {
        val currentUsers = discoveredUsers.values
            .sortedWith(compareByDescending<NearbyUser> { it.rssi }.thenBy { it.userId })
            .toList()

        _uiState.value = _uiState.value.copy(
            nearbyUsers = currentUsers,
            statusMessage = if (currentUsers.isEmpty()) {
                "Scanning for nearby users..."
            } else {
                "Found ${currentUsers.size} nearby user${if (currentUsers.size == 1) "" else "s"}"
            }
        )
    }

    private fun fetchUserData(userId: String) {
        Log.d(TAG, "Fetching user data for userId: $userId")

        viewModelScope.launch {
            try {
                // Fetch profile data
                val profileData = fetchUserProfile(userId)
                // Fetch profile image URL
                val imageUrl = fetchProfileImageUrl(userId)

                val userName = if (profileData != null) {
                    "${profileData.first} ${profileData.second}".trim()
                } else {
                    "User $userId"
                }

                // Update the user in our map
                val existingUser = discoveredUsers[userId]
                if (existingUser != null) {
                    val updatedUser = existingUser.copy(
                        userName = userName,
                        userProfileImageUrl = imageUrl ?: "",
                        isLoadingData = false
                    )
                    discoveredUsers[userId] = updatedUser
                    updateNearbyUsersList()
                }

                Log.d(TAG, "User data loaded for $userId: $userName, imageUrl: $imageUrl")

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching user data for $userId", e)
                // Update user to show error state
                val existingUser = discoveredUsers[userId]
                if (existingUser != null) {
                    val updatedUser = existingUser.copy(
                        userName = "User $userId",
                        userProfileImageUrl = "",
                        isLoadingData = false
                    )
                    discoveredUsers[userId] = updatedUser
                    updateNearbyUsersList()
                }
            }
        }
    }

    private suspend fun fetchUserProfile(userId: String): Pair<String, String>? {
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("user_id", userId.toIntOrNull() ?: return@withContext null)
                }

                val mediaType = "application/json".toMediaType()
                val requestBody = json.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url("$BASE_URL/profile")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseText = response.body?.string()
                        responseText?.let {
                            val jsonResponse = JSONObject(it)
                            val firstName = jsonResponse.optString("first_name", "")
                            val lastName = jsonResponse.optString("last_name", "")
                            Pair(firstName, lastName)
                        }
                    } else {
                        Log.e(TAG, "Profile fetch failed for $userId: ${response.code}")
                        null
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error fetching profile for $userId", e)
                null
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error fetching profile for $userId", e)
                null
            }
        }
    }

    private suspend fun fetchProfileImageUrl(userId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/user/profile_picture_status?user_id=$userId")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseText = response.body?.string()
                        responseText?.let {
                            val jsonResponse = JSONObject(it)
                            val imageUrl = jsonResponse.optString("image_url", "")
                            if (imageUrl.isNotEmpty()) imageUrl else null
                        }
                    } else {
                        Log.e(TAG, "Profile image fetch failed for $userId: ${response.code}")
                        null
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error fetching profile image for $userId", e)
                null
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error fetching profile image for $userId", e)
                null
            }
        }
    }

    fun startScanning() {
        Log.d(TAG, "startScanning() called")

        if (!_uiState.value.hasRequiredPermissions) {
            Log.e(TAG, "Cannot start scanning: Missing permissions")
            _uiState.value = _uiState.value.copy(
                scanningState = BleScanningState.Error("Missing permissions"),
                statusMessage = "Missing Bluetooth permissions"
            )
            return
        }

        if (!_uiState.value.isBluetoothEnabled) {
            Log.e(TAG, "Cannot start scanning: Bluetooth disabled")
            _uiState.value = _uiState.value.copy(
                scanningState = BleScanningState.Error("Bluetooth disabled"),
                statusMessage = "Bluetooth is disabled"
            )
            return
        }

        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        if (bluetoothLeScanner == null) {
            Log.e(TAG, "BLE Scanner not available")
            _uiState.value = _uiState.value.copy(
                scanningState = BleScanningState.Error("BLE not supported"),
                statusMessage = "BLE scanning not supported"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            scanningState = BleScanningState.Starting,
            statusMessage = "Starting to scan...",
            isScanning = true
        )

        try {
            // Configure scan settings for better performance
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                .setReportDelay(0L)
                .build()

            // Create scan filters to only scan for our custom manufacturer data
            val scanFilters = listOf(
                ScanFilter.Builder()
                    .setManufacturerData(CUSTOM_COMPANY_ID, byteArrayOf())
                    .build()
            )

            Log.d(TAG, "Starting BLE scan with filters for company ID: $CUSTOM_COMPANY_ID")
            bluetoothLeScanner?.startScan(scanFilters, scanSettings, scanCallback)

            _uiState.value = _uiState.value.copy(
                scanningState = BleScanningState.Scanning,
                statusMessage = "Scanning for nearby users..."
            )

        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception starting scan", e)
            _uiState.value = _uiState.value.copy(
                scanningState = BleScanningState.Error("Permission denied"),
                statusMessage = "Bluetooth permission denied",
                isScanning = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting scan", e)
            _uiState.value = _uiState.value.copy(
                scanningState = BleScanningState.Error("Failed to start"),
                statusMessage = "Failed to start scanning",
                isScanning = false
            )
        }
    }

    fun stopScanning() {
        Log.d(TAG, "stopScanning() called")

        try {
            bluetoothLeScanner?.stopScan(scanCallback)
            _uiState.value = _uiState.value.copy(
                scanningState = BleScanningState.Stopped,
                statusMessage = "Scanning stopped",
                isScanning = false
            )
            Log.d(TAG, "BLE Scanning stopped")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception stopping scan", e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception stopping scan", e)
        }
    }

    private fun startPeriodicCleanup() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(5000L) // Check every 5 seconds
                cleanupOldUsers()
            }
        }
    }

    private fun cleanupOldUsers() {
        val currentTime = System.currentTimeMillis()
        val usersToRemove = mutableListOf<String>()

        discoveredUsers.forEach { (userId, user) ->
            if (currentTime - user.lastSeen > USER_TIMEOUT_MS) {
                usersToRemove.add(userId)
            }
        }

        if (usersToRemove.isNotEmpty()) {
            usersToRemove.forEach { userId ->
                discoveredUsers.remove(userId)
            }
            Log.d(TAG, "Cleaned up ${usersToRemove.size} old users")
            updateNearbyUsersList()
        }
    }

    private fun checkPermissionsAndBluetooth() {
        val hasPermissions = checkRequiredPermissions()
        val bluetoothEnabled = bluetoothAdapter?.isEnabled == true

        _uiState.value = _uiState.value.copy(
            hasRequiredPermissions = hasPermissions,
            isBluetoothEnabled = bluetoothEnabled
        )

        Log.d(TAG, "Permissions: $hasPermissions, Bluetooth: $bluetoothEnabled")
    }

    private fun checkRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ permissions
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            // Pre-Android 12 permissions
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    fun refreshPermissionsAndBluetooth() {
        checkPermissionsAndBluetooth()
    }

    fun onUserTapped(user: NearbyUser) {
        Log.d(TAG, "User tapped: ${user.userId} - ${user.userName}")
        // This will be handled by the screen to navigate to Screen 4
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared, stopping scanning!")
        stopScanning()
        discoveredUsers.clear()
    }
}