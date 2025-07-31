package com.example.nfccardtaptopayv101.ui.viewmodel.ble

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
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
import java.util.UUID

private const val BASE_URL = "https://nfc-fastapi-backend.onrender.com"

sealed class BleAdvertisingState {
    object Idle : BleAdvertisingState()
    object Starting : BleAdvertisingState()
    object Broadcasting : BleAdvertisingState()
    object Stopped : BleAdvertisingState()
    data class Error(val message: String) : BleAdvertisingState()
}

data class Screen3UiState(
    val advertisingState: BleAdvertisingState = BleAdvertisingState.Idle,
    val hasRequiredPermissions: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val statusMessage: String = "Preparing to broadcast...",
    val userId: String = "",
    val userName: String = "",
    val userProfileImageUrl: String = "",
    val isLoadingUserData: Boolean = false
)

class Screen3ViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        private const val TAG = "Screen3ViewModel"
        // Use your own custom company ID for manufacturer data (avoid using Apple's 0x004C)
        private const val CUSTOM_COMPANY_ID = 0x1269 // Replace with your registered company ID
    }

    private val context = getApplication<Application>()
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null

    // HTTP client for API calls
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val _uiState = MutableStateFlow(Screen3UiState())
    val uiState: StateFlow<Screen3UiState> = _uiState.asStateFlow()

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            Log.d(TAG, "BLE Advertising started successfully")
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(
                    advertisingState = BleAdvertisingState.Broadcasting,
                    statusMessage = "Broadcasting availability..."
                )
            }
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            val errorMessage = when (errorCode) {
                ADVERTISE_FAILED_ALREADY_STARTED -> "Advertising already started"
                ADVERTISE_FAILED_DATA_TOO_LARGE -> "Advertising data too large"
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "Advertising not supported on this device"
                ADVERTISE_FAILED_INTERNAL_ERROR -> "Internal advertising error"
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many advertisers active"
                else -> "Unknown advertising error: $errorCode"
            }
            Log.e(TAG, "BLE Advertising failed: $errorMessage")
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(
                    advertisingState = BleAdvertisingState.Error(errorMessage),
                    statusMessage = "Broadcasting failed: $errorMessage"
                )
            }
        }
    }

    init {
        checkPermissionsAndBluetooth()
    }

    fun setUserId(userId: String) {
        _uiState.value = _uiState.value.copy(userId = userId)
        Log.d(TAG, "UserId set: $userId")

        // Fetch user data when userId is set
        if (userId.isNotEmpty()) {
            fetchUserData(userId)
        }
    }

    private fun fetchUserData(userId: String) {
        Log.d(TAG, "Fetching user data for userId: $userId")
        _uiState.value = _uiState.value.copy(isLoadingUserData = true)

        viewModelScope.launch {
            try {
                // Fetch profile data
                val profileData = fetchUserProfile(userId)
                // Fetch profile image URL
                val imageUrl = fetchProfileImageUrl(userId)

                val userName = if (profileData != null) {
                    "${profileData.first} ${profileData.second}".trim()
                } else {
                    "User"
                }

                _uiState.value = _uiState.value.copy(
                    userName = userName,
                    userProfileImageUrl = imageUrl ?: "",
                    isLoadingUserData = false
                )

                Log.d(TAG, "User data loaded: $userName, imageUrl: $imageUrl")

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching user data", e)
                _uiState.value = _uiState.value.copy(
                    userName = "User",
                    userProfileImageUrl = "",
                    isLoadingUserData = false
                )
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
                        Log.e(TAG, "Profile fetch failed: ${response.code}")
                        null
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error fetching profile", e)
                null
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error fetching profile", e)
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
                        Log.e(TAG, "Profile image fetch failed: ${response.code}")
                        null
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error fetching profile image", e)
                null
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error fetching profile image", e)
                null
            }
        }
    }

    fun startAdvertising() {
        Log.d(TAG, "startAdvertising() called")

        if (_uiState.value.userId.isEmpty()) {
            Log.e(TAG, "Cannot start advertising: userId is empty")
            _uiState.value = _uiState.value.copy(
                advertisingState = BleAdvertisingState.Error("User ID not set"),
                statusMessage = "Error: User ID missing"
            )
            return
        }

        if (!_uiState.value.hasRequiredPermissions) {
            Log.e(TAG, "Cannot start advertising: Missing permissions")
            _uiState.value = _uiState.value.copy(
                advertisingState = BleAdvertisingState.Error("Missing permissions"),
                statusMessage = "Missing Bluetooth permissions"
            )
            return
        }

        if (!_uiState.value.isBluetoothEnabled) {
            Log.e(TAG, "Cannot start advertising: Bluetooth disabled")
            _uiState.value = _uiState.value.copy(
                advertisingState = BleAdvertisingState.Error("Bluetooth disabled"),
                statusMessage = "Bluetooth is disabled"
            )
            return
        }

        bluetoothLeAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        if (bluetoothLeAdvertiser == null) {
            Log.e(TAG, "BLE Advertiser not available")
            _uiState.value = _uiState.value.copy(
                advertisingState = BleAdvertisingState.Error("BLE not supported"),
                statusMessage = "BLE advertising not supported"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            advertisingState = BleAdvertisingState.Starting,
            statusMessage = "Starting to broadcast..."
        )

        try {
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(false)
                .setTimeout(0) // Advertise indefinitely
                .build()

            // Use the compact version to avoid "data too large" error
            val data = createAdvertiseData(_uiState.value.userId)

            // Debug the size
            debugAdvertiseDataSize(data)

            Log.d(TAG, "Starting BLE advertising for userId: ${_uiState.value.userId}")
            bluetoothLeAdvertiser?.startAdvertising(settings, data, advertiseCallback)

        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception starting advertising", e)
            _uiState.value = _uiState.value.copy(
                advertisingState = BleAdvertisingState.Error("Permission denied"),
                statusMessage = "Bluetooth permission denied"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting advertising", e)
            _uiState.value = _uiState.value.copy(
                advertisingState = BleAdvertisingState.Error("Failed to start"),
                statusMessage = "Failed to start broadcasting"
            )
        }
    }

    fun stopAdvertising() {
        Log.d(TAG, "stopAdvertising() called")

        try {
            bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
            _uiState.value = _uiState.value.copy(
                advertisingState = BleAdvertisingState.Stopped,
                statusMessage = "Broadcasting stopped"
            )
            Log.d(TAG, "BLE Advertising stopped")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception stopping advertising", e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception stopping advertising", e)
        }
    }

    // Compact advertisement data using manufacturer data (most space-efficient)
    private fun createAdvertiseData(userId: String): AdvertiseData {
        val idInt = userId.toIntOrNull() ?: 0

        // Use only 2 bytes for user ID (supports IDs 0-65535)
        val userIdBytes = ByteArray(2).apply {
            this[0] = (idInt shr 8).toByte()
            this[1] = idInt.toByte()
        }

        return AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            // Using manufacturer data - most space efficient approach
            .addManufacturerData(CUSTOM_COMPANY_ID, userIdBytes)
            .build()
    }

    // Debug function to estimate advertisement size
    private fun debugAdvertiseDataSize(data: AdvertiseData): Int {
        var size = 0

        // Flags: 3 bytes (always present)
        size += 3

        // Manufacturer Data
        data.manufacturerSpecificData?.let { sparseArray ->
            for (i in 0 until sparseArray.size()) {
                size += 2 // Company ID (2 bytes)
                size += 1 // Length field
                size += 1 // Type field
                size += sparseArray.valueAt(i).size // Actual data
            }
        }

        // Service UUIDs (if any)
        data.serviceUuids?.forEach { uuid ->
            size += if (uuid.uuid.toString().length > 8) 18 else 4 // 128-bit vs 16-bit UUID
        }

        // Service Data (if any)
        data.serviceData?.forEach { (uuid, bytes) ->
            size += if (uuid.uuid.toString().length > 8) 18 else 4 // UUID size
            size += bytes.size // Data size
        }

        Log.d(TAG, "Estimated advertisement size: $size bytes (limit: 31 bytes)")
        return size
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
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED &&
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
                Manifest.permission.BLUETOOTH_ADVERTISE,
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

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared, stopping advertising!")
        stopAdvertising()
    }
}