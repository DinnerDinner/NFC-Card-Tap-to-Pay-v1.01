package com.example.nfccardtaptopayv101.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        fetchProfile(context) { response ->
            profileData = response
            isLoading = false
        }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        profileData?.let { data ->
            val scrollState = rememberScrollState()

            var firstName by remember { mutableStateOf(data.optString("first_name")) }
            var lastName by remember { mutableStateOf(data.optString("last_name")) }
            var email by remember { mutableStateOf(data.optString("email")) }
            var phoneNumber by remember { mutableStateOf(data.optString("phone_number")) }
            var dob by remember { mutableStateOf(data.optString("dob")) }
            var balance by remember { mutableStateOf(data.optDouble("balance").toString()) }
            var cardUid by remember { mutableStateOf(data.optString("card_uid")) }
            var isCadet by remember { mutableStateOf(data.optString("is_cadet")) }
            var isStudent by remember { mutableStateOf(data.optString("is_student")) }
            var isHospitalUser by remember { mutableStateOf(data.optString("is_hospital_user")) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Welcome back, $firstName.", style = MaterialTheme.typography.headlineMedium)
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                EditableField("First Name", firstName, isEditing) { firstName = it }
                EditableField("Last Name", lastName, isEditing) { lastName = it }

                // Email - always text, never editable
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Email", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Text(email, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                EditableField("Phone Number", phoneNumber, isEditing) { phoneNumber = it }
                EditableField("Date of Birth", dob, isEditing) { dob = it }

                // Balance - always text, never editable
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Balance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Text(balance, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                EditableField("Card UID", cardUid, isEditing) { cardUid = it }
                EditableField("Cadet", isCadet, isEditing) { isCadet = it }
                EditableField("Student", isStudent, isEditing) { isStudent = it }
                EditableField("Patient", isHospitalUser, isEditing) { isHospitalUser = it }

                Spacer(modifier = Modifier.height(12.dp))

                Button(onClick = {
                    if (isEditing) {
                        val updatedJson = JSONObject().apply {
                            put("first_name", firstName)
                            put("last_name", lastName)
                            put("email", email)  // send unchanged email
                            put("phone_number", phoneNumber)
                            put("dob", dob)
                            put("balance", balance.toDoubleOrNull() ?: 0.0) // sending balance unchanged
                            put("card_uid", cardUid)
                            put("is_cadet", isCadet)
                            put("is_student", isStudent)
                            put("is_hospital_user", isHospitalUser)
                        }
                        sendUpdateRequest(context, updatedJson)
                    }
                    isEditing = !isEditing
                }) {
                    Text(if (isEditing) "Save Changes" else "Edit Profile")
                }
            }
        } ?: run {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error loading profile.")
            }
        }
    }
}

@Composable
fun EditableField(label: String, value: String, isEditing: Boolean, onValueChange: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            if (isEditing) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(value, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

private fun sendUpdateRequest(context: Context, data: JSONObject) {
    val client = OkHttpClient()
    val mediaType = "application/json".toMediaType()
    val requestBody = data.toString().toRequestBody(mediaType)
    val request = Request.Builder()
        .url("https://promoted-quetzal-visually.ngrok-free.app/update-profile")
        .post(requestBody)
        .build()

    CoroutineScope(Dispatchers.IO).launch {
        try {
            client.newCall(request).execute().use { response ->
                val responseText = response.body?.string() ?: ""

                CoroutineScope(Dispatchers.Main).launch {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Profile updated successfully.", Toast.LENGTH_SHORT).show()
                    } else {
                        val errorMessage = try {
                            val json = JSONObject(responseText)
                            json.optString("detail", json.optString("message", "Unknown error"))
                        } catch (e: Exception) {
                            responseText.ifBlank { "Failed to update profile." }
                        }
                        Toast.makeText(context, "$errorMessage", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: Exception) {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
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
        .url("https://promoted-quetzal-visually.ngrok-free.app/profile")
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
