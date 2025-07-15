package com.example.nfccardtaptopayv101
import android.app.Activity

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class CheckoutTapPaymentActivity : ComponentActivity() {

    enum class ScreenState { WAIT_TAP, SUCCESS }

    private var nfcAdapter: NfcAdapter? = null
    private val client = OkHttpClient()
    private lateinit var merchantId: String
    private var amountToCharge: Float = 0f

    private var postPurchaseCallback: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        merchantId = prefs.getString("user_data", null)
            ?.let { JSONObject(it).optString("user_id", "") }
            ?: ""

        amountToCharge = intent.getFloatExtra("amount_to_charge", 0f)
        if (amountToCharge <= 0f) {
            finish()
            return
        }

        setContent {
            var screenState by remember { mutableStateOf(ScreenState.WAIT_TAP) }
            var successMessage by remember { mutableStateOf("") }
            val context = LocalContext.current

            fun postPurchase(cardUid: String) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val json = JSONObject().apply {
                        put("uid", cardUid)
                        put("merchant_id", merchantId)
                        put("amount", amountToCharge)
                    }
                    val body = json.toString().toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("https://promoted-quetzal-visually.ngrok-free.app/transfer")
                        .post(body)
                        .build()

                    try {
                        client.newCall(request).execute().use { resp ->
                            val responseText = resp.body?.string() ?: "{}"
                            val jsonResponse = JSONObject(responseText)
                            val message = if (resp.isSuccessful) {
                                jsonResponse.optString("message", "✅ Payment successful")
                            } else {
                                jsonResponse.optString("detail", "❌ Payment failed")
                            }
                            withContext(Dispatchers.Main) {
                                successMessage = message
                                screenState = ScreenState.SUCCESS

                                // After 3 seconds auto-finish and return intent
                                if (resp.isSuccessful) {
                                    setResult(Activity.RESULT_OK)
                                    // Finish after 2 seconds or so
                                    lifecycleScope.launch {
                                        kotlinx.coroutines.delay(2000)
                                        finish()
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            successMessage = "❌ Network error: ${e.localizedMessage}"
                            screenState = ScreenState.SUCCESS
                        }
                    }
                }
            }

            postPurchaseCallback = ::postPurchase

            when (screenState) {
                ScreenState.WAIT_TAP -> WaitTapScreen(amountToCharge)
                ScreenState.SUCCESS -> SuccessScreen(successMessage)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))
        val techList = arrayOf(arrayOf(android.nfc.tech.IsoDep::class.java.name))
        nfcAdapter?.enableForegroundDispatch(this, pi, filters, techList)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val postPurchase = postPurchaseCallback ?: return

        val tag: Tag? = intent?.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        val uid = tag?.id?.joinToString("") { "%02X".format(it) } ?: return

        postPurchase(uid)
    }

    @Composable
    private fun WaitTapScreen(amount: Float) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Charge Customer", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))
            Text("$${"%.2f".format(amount)}", style = MaterialTheme.typography.displayMedium.copy(fontSize = 48.sp))
            Spacer(Modifier.height(24.dp))
            Text("Please tap the customer's NFC card", style = MaterialTheme.typography.bodyLarge)
        }
    }

    @Composable
    private fun SuccessScreen(message: String) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(message, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))
            Text("Returning to Sales Page...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}




//package com.example.nfccardtaptopayv101
//
//import android.app.Activity
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
//import androidx.compose.ui.unit.sp
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
//class CheckoutTapPaymentActivity : ComponentActivity() {
//
//    enum class ScreenState { WAIT_TAP, SUCCESS }
//
//    private var nfcAdapter: NfcAdapter? = null
//    private val client = OkHttpClient()
//    private lateinit var merchantId: String
//    private var amountToCharge: Float = 0f
//
//    private var postPurchaseCallback: ((String) -> Unit)? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
//
//        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
//        merchantId = prefs.getString("user_data", null)
//            ?.let { JSONObject(it).optString("user_id", "") }
//            ?: ""
//
//        amountToCharge = intent.getFloatExtra("amount_to_charge", 0f)
//        if (amountToCharge <= 0f) {
//            Toast.makeText(this, "Invalid amount to charge", Toast.LENGTH_LONG).show()
//            finish()
//            return
//        }
//
//        setContent {
//            var screenState by remember { mutableStateOf(ScreenState.WAIT_TAP) }
//            var successMessage by remember { mutableStateOf("") }
//
//            // Define postPurchase inside Compose so it can update Compose state
//            fun postPurchase(cardUid: String) {
//                lifecycleScope.launch(Dispatchers.IO) {
//                    val json = JSONObject().apply {
//                        put("uid", cardUid)
//                        put("merchant_id", merchantId)
//                        put("amount", amountToCharge)
//                    }
//                    val body = json.toString().toRequestBody("application/json".toMediaType())
//                    val request = Request.Builder()
//                        .url("https://promoted-quetzal-visually.ngrok-free.app/transfer")
//                        .post(body)
//                        .build()
//
//                    try {
//                        client.newCall(request).execute().use { resp ->
//                            val responseText = resp.body?.string() ?: "{}"
//                            val jsonResponse = JSONObject(responseText)
//                            val message = if (resp.isSuccessful) {
//                                jsonResponse.optString("message", "✅ Payment successful")
//                            } else {
//                                jsonResponse.optString("detail", "❌ Payment failed")
//                            }
//                            withContext(Dispatchers.Main) {
//                                successMessage = message
//                                screenState = ScreenState.SUCCESS
//                            }
//                        }
//                    } catch (e: Exception) {
//                        withContext(Dispatchers.Main) {
//                            successMessage = "❌ Network error: ${e.localizedMessage}"
//                            screenState = ScreenState.SUCCESS
//                        }
//                    }
//                }
//            }
//
//            // Provide postPurchase callback for onNewIntent
//            postPurchaseCallback = ::postPurchase
//
//            when (screenState) {
//                ScreenState.WAIT_TAP -> WaitTapScreen(amountToCharge)
//                ScreenState.SUCCESS -> SuccessScreen(
//                    successMessage,
//                    onNewPayment = {
//                        screenState = ScreenState.WAIT_TAP
//                        successMessage = ""
//                    },
//                    onBackToProfile = {
//                        startActivity(Intent(this@CheckoutTapPaymentActivity, MainActivity::class.java).apply {
//                            putExtra("goToProfile", true)
//                        })
//                        finish()
//                    }
//                )
//            }
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//        val pi = PendingIntent.getActivity(
//            this, 0,
//            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
//            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
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
//
//        // Only proceed if postPurchase callback is set (Compose initialized)
//        val postPurchase = postPurchaseCallback ?: return
//
//        val tag: Tag? = intent?.getParcelableExtra(NfcAdapter.EXTRA_TAG)
//        val uid = tag?.id?.joinToString("") { "%02X".format(it) } ?: return
//
//        postPurchase(uid)
//    }
//
//    @Composable
//    private fun WaitTapScreen(amount: Float) {
//        Column(
//            modifier = Modifier.fillMaxSize().padding(32.dp),
//            verticalArrangement = Arrangement.Center,
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Text("Charge Customer", style = MaterialTheme.typography.headlineMedium)
//            Spacer(Modifier.height(24.dp))
//            Text("$${"%.2f".format(amount)}", style = MaterialTheme.typography.displayMedium.copy(fontSize = 48.sp))
//            Spacer(Modifier.height(24.dp))
//            Text("Please tap the customer's NFC card", style = MaterialTheme.typography.bodyLarge)
//        }
//    }
//
//    @Composable
//    private fun SuccessScreen(
//        message: String,
//        onNewPayment: () -> Unit,
//        onBackToProfile: () -> Unit
//    ) {
//        val context = LocalContext.current
//
//        Column(
//            modifier = Modifier.fillMaxSize().padding(32.dp),
//            verticalArrangement = Arrangement.Center,
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Text(message, style = MaterialTheme.typography.headlineMedium)
//            Spacer(Modifier.height(32.dp))
//            Button(onClick = onNewPayment) { Text("New Payment") }
//            Spacer(Modifier.height(16.dp))
//            Button(onClick = onBackToProfile) { Text("Back to Profile") }
//        }
//    }
//}
