package com.example.nfccardtaptopayv101.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.*

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

                // Email - not editable
                EditableField("Email", email, isEditing) { email = it }

                EditableField("Phone Number", phoneNumber, isEditing) { phoneNumber = it }

                // Date of Birth - only editable via calendar
                DateOfBirthPicker(dob, isEditing) { dob = it }

                // Balance - not editable
                StaticField("Balance", balance)

                EditableField("Card UID", cardUid, isEditing) { cardUid = it }

                // Cadet, Student, Patient - not editable
                StaticField("Cadet", isCadet)
                StaticField("Student", isStudent)
                StaticField("Patient", isHospitalUser)

                Spacer(modifier = Modifier.height(12.dp))
                val prefs = LocalContext.current.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                val userId = prefs.getInt("user_id", -1)
                Button(onClick = {
                    if (isEditing) {
                        val updatedJson = JSONObject().apply {
                            put("first_name", firstName)
                            put("last_name", lastName)
                            put("email", email)
                            put("phone_number", phoneNumber)
                            put("dob", dob)
                            put("balance", balance.toDoubleOrNull() ?: 0.0)
                            put("card_uid", cardUid)
                            put("is_cadet", isCadet)
                            put("is_student", isStudent)
                            put("is_hospital_user", isHospitalUser)
                        }
                        if (userId != -1) {
                            sendUpdateRequest(context, updatedJson, userId)
                        } else {
                            Toast.makeText(context, "User ID missing. Please log in again.", Toast.LENGTH_LONG).show()
                        }
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

@Composable
fun StaticField(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun DateOfBirthPicker(dob: String, isEditing: Boolean, onDateChange: (String) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val year = try { dob.substring(0, 4).toInt() } catch (e: Exception) { calendar.get(Calendar.YEAR) }
    val month = try { dob.substring(5, 7).toInt() - 1 } catch (e: Exception) { calendar.get(Calendar.MONTH) }
    val day = try { dob.substring(8, 10).toInt() } catch (e: Exception) { calendar.get(Calendar.DAY_OF_MONTH) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = isEditing) {
                    DatePickerDialog(
                        context,
                        { _, selectedYear, selectedMonth, selectedDay ->
                            val newDob = "%04d-%02d-%02d".format(selectedYear, selectedMonth + 1, selectedDay)
                            onDateChange(newDob)
                        },
                        year,
                        month,
                        day
                    ).show()
                }
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Date of Birth", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(dob, style = MaterialTheme.typography.bodyLarge)
                }
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Pick date",
                    tint = if (isEditing) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        }
    }
}

private fun sendUpdateRequest(context: Context, data: JSONObject, userId: Int) {
    val client = OkHttpClient()
    val mediaType = "application/json".toMediaType()
    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val userId = prefs.getInt("user_id", -1)  // -1 means missing
    data.put("user_id", userId)
    val requestBody = data.toString().toRequestBody(mediaType)
    val request = Request.Builder()
        .url("https://nfc-fastapi-backend.onrender.com/update-profile")
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
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
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
    val userId = prefs.getInt("user_id", -1)
    if (userId == -1) {
        // user_id missing — no profile to fetch
        onResult(null)
        return
    }

    val json = JSONObject().apply {
        put("user_id", userId)
    }

    val client = OkHttpClient()
    val mediaType = "application/json".toMediaType()
    val requestBody = json.toString().toRequestBody(mediaType)
    val request = Request.Builder()
        .url("https://nfc-fastapi-backend.onrender.com/profile")
        .post(requestBody)
        .build()

    CoroutineScope(Dispatchers.IO).launch {
        try {
            client.newCall(request).execute().use { response ->
                val responseText = response.body?.string()
                val jsonResponse = responseText?.let { JSONObject(it) }
                CoroutineScope(Dispatchers.Main).launch {
                    onResult(jsonResponse)
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