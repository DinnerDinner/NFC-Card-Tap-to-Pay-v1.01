package com.example.nfccardtaptopayv101.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    var profileData by remember { mutableStateOf<JSONObject?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        fetchProfile(context) { response ->
            profileData = response
            isLoading = false
        }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        profileData?.let { data ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("👤 Profile \nWelcome back ${data.optString("first_name")}", style = MaterialTheme.typography.headlineMedium)
                Divider()

                Text("First Name: ${data.optString("first_name")}")
                Text("Last Name: ${data.optString("last_name")}")
                Text("Email: ${data.optString("email")}")
                Text("Phone Number: ${data.optString("phone_number")}")
                Text("Date of Birth: ${data.optString("dob")}")
                Text("Balance: $${data.optDouble("balance")}")
                Text("Card UID: ${data.optString("card_uid")}")
                Text("Cadet: ${data.optString("is_cadet")}")
                Text("Student: ${data.optString("is_student")}")
                Text("Patient: ${data.optString("is_hospital_user")}")
            }
        } ?: run {
            Text("Error loading profile.")
        }
    }
}

private fun fetchProfile(context: Context, onResult: (JSONObject?) -> Unit) {
    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val userJson = prefs.getString("user_data", null)

    if (userJson == null) {
        onResult(null)
        return
    }

    val email = try {
        JSONObject(userJson).getString("email")
    } catch (e: Exception) {
        onResult(null)
        return
    }

    val json = JSONObject().apply {
        put("email", email)
    }

    val client = OkHttpClient()
    val mediaType = "application/json".toMediaType()
    val requestBody = json.toString().toRequestBody(mediaType)
    val request = Request.Builder()
        .url("https://ec42a9411756.ngrok-free.app/profile")
        .post(requestBody)
        .build()

    CoroutineScope(Dispatchers.IO).launch {
        try {
            client.newCall(request).execute().use { response ->
                val responseText = response.body?.string()
                val json = responseText?.let { JSONObject(it) }
                CoroutineScope(Dispatchers.Main).launch {
                    onResult(json)
                }
            }
        } catch (e: Exception) {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Failed to load profile: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                onResult(null)
            }
        }
    }
}
