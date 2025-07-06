package com.example.nfccardtaptopayv101.ui

import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import com.example.nfccardtaptopayv101.LoginActivity
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

@Composable
fun MerchantScreen() {
    var amountInput by remember { mutableStateOf("") }
    var isAwaitingTap by remember { mutableStateOf(false) }
    var amountConfirmed by remember { mutableStateOf(0f) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (!isAwaitingTap) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Enter Amount to Charge", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = amountInput,
                onValueChange = { amountInput = it },
                label = { Text("Amount (CAD)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                amountConfirmed = amountInput.toFloatOrNull() ?: 0f
                if (amountConfirmed > 0f) {
                    isAwaitingTap = true
                    Toast.makeText(context, "Awaiting card tap for $$amountConfirmed", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("Request Payment")
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Charge: $${String.format("%.2f", amountConfirmed)}", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(32.dp))
            Text("Please tap the customer's NFC card", style = MaterialTheme.typography.bodyLarge)
        }

        // Listen to NFC taps from Activity
        (context as? ComponentActivity)?.intent?.let { intent ->
            val action = intent.action
            if (action == NfcAdapter.ACTION_TAG_DISCOVERED ||
                action == NfcAdapter.ACTION_TECH_DISCOVERED
            ) {
                val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
                val uid = tag?.id?.joinToString("") { "%02X".format(it) } ?: "N/A"

                val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                val merchantJson = prefs.getString("user_data", null)
                val merchantEmail = merchantJson?.let { JSONObject(it).getString("email") } ?: return

                val client = OkHttpClient()
                val mediaType = "application/json".toMediaType()
                val json = JSONObject().apply {
                    put("uid", uid)
                    put("merchant_email", merchantEmail)
                    put("amount", amountConfirmed)
                }
                val body = json.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url("https://b3bf-207-253-242-126.ngrok-free.app/purchase")
                    .post(body)
                    .build()

                scope.launch {
                    try {
                        client.newCall(request).execute().use { response ->
                            val responseBody = response.body?.string() ?: ""
                            if (response.isSuccessful) {
                                Toast.makeText(context, "✅ Payment successful!", Toast.LENGTH_LONG).show()
                            } else {
                                val msg = JSONObject(responseBody).optString("detail", "Payment failed")
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    } finally {
                        amountInput = ""
                        isAwaitingTap = false
                        amountConfirmed = 0f
                    }
                }
            }
        }
    }
}
