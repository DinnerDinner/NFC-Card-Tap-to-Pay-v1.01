//package com.example.nfccardtaptopayv101
//
//import android.app.PendingIntent
//import android.content.Context
//import android.content.Intent
//import android.content.IntentFilter
//import android.nfc.NfcAdapter
//import android.nfc.Tag
//import android.os.Bundle
//import android.widget.Toast
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.lifecycleScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import okhttp3.MediaType.Companion.toMediaType
//import okhttp3.OkHttpClient
//import okhttp3.Request
//import okhttp3.RequestBody.Companion.toRequestBody
//import org.json.JSONObject
//
//class MerchantActivity : ComponentActivity() {
//
//    private var nfcAdapter: NfcAdapter? = null
//    private val client = OkHttpClient()
//
//    private var isAwaitingCard = false
//    private var amountToCharge: Float = 0f
//    private var merchantEmail: String? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
//
//        // Get logged in user email
//        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
//        merchantEmail = prefs.getString("user_data", null)?.let {
//            JSONObject(it).getString("email")
//        }
//
//        setContent {
//            MerchantScreenUI { enteredAmount ->
//                amountToCharge = enteredAmount
//                isAwaitingCard = true
//                Toast.makeText(this, "Tap customer's card to charge $$enteredAmount", Toast.LENGTH_LONG).show()
//            }
//        }
//    }
//
//    // -------- NFC Setup --------
//    override fun onResume() {
//        super.onResume()
//        val pi = PendingIntent.getActivity(
//            this, 0,
//            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
//            PendingIntent.FLAG_MUTABLE
//        )
//        val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))
//        val techList = arrayOf(arrayOf(android.nfc.tech.IsoDep::class.java.name))
//        nfcAdapter?.enableForegroundDispatch(this, pi, filters, techList)
//    }
//
//    override fun onPause() {
//        super.onPause()
//        nfcAdapter?.disableForegroundDispatch(this)
//    }
//
//    override fun onNewIntent(intent: Intent?) {
//        super.onNewIntent(intent)
//        if (!isAwaitingCard || merchantEmail == null) return
//
//        val tag: Tag? = intent?.getParcelableExtra(NfcAdapter.EXTRA_TAG)
//        val uid = tag?.id?.joinToString("") { "%02X".format(it) } ?: return
//
//        val json = JSONObject().apply {
//            put("uid", uid)
//            put("merchant_email", merchantEmail)
//            put("amount", amountToCharge)
//        }
//
//        isAwaitingCard = false
//
//        lifecycleScope.launch(Dispatchers.IO) {
//            val body = json.toString().toRequestBody("application/json".toMediaType())
//            val request = Request.Builder()
//                .url("https://b3bf-207-253-242-126.ngrok-free.app/purchase")
//                .post(body)
//                .build()
//
//            try {
//                client.newCall(request).execute().use { resp ->
//                    val msg = if (resp.isSuccessful) {
//                        JSONObject(resp.body?.string() ?: "{}").optString("message", "✅ Payment successful")
//                    } else {
//                        JSONObject(resp.body?.string() ?: "{}").optString("detail", "❌ Payment failed")
//                    }
//
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(this@MerchantActivity, msg, Toast.LENGTH_LONG).show()
//                        setContent {
//                            MerchantScreenUI(onSubmit = {
//                                amountToCharge = it
//                                isAwaitingCard = true
//                                Toast.makeText(this@MerchantActivity, "Tap customer's card to charge $$it", Toast.LENGTH_LONG).show()
//                            })
//                        }
//                    }
//                }
//            } catch (_: Exception) {
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(this@MerchantActivity, "❌ Network error", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun MerchantScreenUI(onSubmit: (Float) -> Unit) {
//    var inputAmount by remember { mutableStateOf("") }
//    val context = LocalContext.current
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(24.dp),
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Text("Enter Amount to Charge", style = MaterialTheme.typography.headlineSmall)
//        Spacer(Modifier.height(16.dp))
//        OutlinedTextField(
//            value = inputAmount,
//            onValueChange = { inputAmount = it },
//            label = { Text("Amount (CAD)") },
//            modifier = Modifier.fillMaxWidth()
//        )
//        Spacer(Modifier.height(24.dp))
//        Button(onClick = {
//            val amt = inputAmount.toFloatOrNull()
//            if (amt != null && amt > 0f) {
//                onSubmit(amt)
//            } else {
//                Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
//            }
//        }) {
//            Text("Request Payment")
//        }
//    }
//}
