package com.example.nfccardtaptopayv101

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.nfccardtaptopayv101.ui.theme.NFCCardTapToPayV101Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject






class LoginActivity : ComponentActivity() {

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NFCCardTapToPayV101Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LoginScreen(
                        onLoginSuccess = { userJsonString ->
                            // Save user JSON locally
                            saveUserData(userJsonString)

                            // Navigate to MainActivity
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        },
                        onNavigateToSignUp = {
                            startActivity(Intent(this, SignUpActivity::class.java))
                        }
                    )
                }
            }
        }
    }

    private fun saveUserData(userJsonString: String) {
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("user_data", userJsonString).apply()
    }

    @Composable
    fun LoginScreen(
        onLoginSuccess: (String) -> Unit,
        onNavigateToSignUp: () -> Unit
    ) {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        val context = LocalContext.current

        var isLoading by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Login", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "Email and password cannot be empty", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true

                    CoroutineScope(Dispatchers.IO).launch {
                        val mediaType = "application/json".toMediaType()
                        val json = JSONObject().apply {
                            put("email", email.trim())
                            put("password", password)
                        }
                        val body = json.toString().toRequestBody(mediaType)
                        val request = Request.Builder()
                            .url("https://promoted-quetzal-visually.ngrok-free.app/login") // Your backend login URL
                            .post(body)
                            .build()

                        try {
                            client.newCall(request).execute().use { response ->
                                val responseBody = response.body?.string() ?: ""
                                if (response.isSuccessful) {
                                    // Successful login, pass user JSON to callback
                                    withContext(Dispatchers.Main) {
                                        onLoginSuccess(responseBody)
                                    }
                                } else {
                                    val errorMsg = try {
                                        JSONObject(responseBody).optString("detail", "Login failed")
                                    } catch (e: Exception) {
                                        "Login failed"
                                    }
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        } finally {
                            withContext(Dispatchers.Main) {
                                isLoading = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Submit")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onNavigateToSignUp) {
                Text("Don't have an account? Sign Up")
            }
        }
    }
}


