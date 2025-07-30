package com.example.nfccardtaptopayv101.activities

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import androidx.compose.runtime.Composable
import com.example.nfccardtaptopayv101.R
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
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

    companion object {
        private const val TAG = "CheckoutTapPayment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() called")

        // Check NFC support first
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)) {
            Log.e(TAG, "NFC not supported on this device")
            Toast.makeText(this, "NFC not supported", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Log.e(TAG, "NFC adapter is null")
            Toast.makeText(this, "NFC not available", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (!nfcAdapter!!.isEnabled) {
            Log.w(TAG, "NFC is disabled")
            Toast.makeText(this, "Please enable NFC", Toast.LENGTH_SHORT).show()
        }

        // Get merchant ID
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userData = prefs.getString("user_data", null)
        Log.d(TAG, "User data from prefs: $userData")

        merchantId = userData?.let { JSONObject(it).optString("user_id", "") } ?: ""
        Log.d(TAG, "Merchant ID: $merchantId")

        if (merchantId.isEmpty()) {
            Log.e(TAG, "Merchant ID is empty")
            Toast.makeText(this, "Merchant ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Get amount
        amountToCharge = intent.getFloatExtra("amount_to_charge", 0f)
        Log.d(TAG, "Amount to charge: $amountToCharge")

        if (amountToCharge <= 0f) {
            Log.e(TAG, "Invalid amount: $amountToCharge")
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d(TAG, "Setting up UI...")
        setContent {
            var screenState by remember { mutableStateOf(ScreenState.WAIT_TAP) }
            var successMessage by remember { mutableStateOf("") }
            val context = LocalContext.current

            fun postPurchase(cardUid: String) {
                Log.d(TAG, "postPurchase called with UID: $cardUid")
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val json = JSONObject().apply {
                            put("uid", cardUid)
                            put("merchant_id", merchantId)
                            put("amount", amountToCharge.toDouble())  // Convert to Double
                        }
                        Log.d(TAG, "Sending request: ${json.toString()}")

                        val body = json.toString().toRequestBody("application/json".toMediaType())
                        val request = Request.Builder()
                            .url("https://nfc-fastapi-backend.onrender.com/transfer")
                            .post(body)
                            .build()

                        client.newCall(request).execute().use { resp ->
                            val responseText = resp.body?.string() ?: "{}"
                            Log.d(TAG, "Response: ${resp.code} - $responseText")

                            val jsonResponse = JSONObject(responseText)
                            val message = if (resp.isSuccessful) {
                                jsonResponse.optString("message", "✅ Payment successful")
                            } else {
                                jsonResponse.optString("detail", "❌ Payment failed")
                            }

                            withContext(Dispatchers.Main) {
                                Log.d(TAG, "Updating UI with message: $message")
                                successMessage = message
                                screenState = ScreenState.SUCCESS

                                if (resp.isSuccessful) {
                                    setResult(Activity.RESULT_OK)
                                    lifecycleScope.launch {
                                        kotlinx.coroutines.delay(2000)
                                        Log.d(TAG, "Auto-finishing activity")
                                        finish()
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Network error", e)
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
        Log.d(TAG, "onCreate() completed")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume() called")

        try {
            val pi = PendingIntent.getActivity(
                this, 0,
                Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))
            val techList = arrayOf(arrayOf(android.nfc.tech.IsoDep::class.java.name))
            nfcAdapter?.enableForegroundDispatch(this, pi, filters, techList)
            Log.d(TAG, "NFC foreground dispatch enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable NFC foreground dispatch", e)
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause() called")
        try {
            nfcAdapter?.disableForegroundDispatch(this)
            Log.d(TAG, "NFC foreground dispatch disabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disable NFC foreground dispatch", e)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent() called with action: ${intent?.action}")

        val postPurchase = postPurchaseCallback
        if (postPurchase == null) {
            Log.e(TAG, "postPurchaseCallback is null")
            return
        }

        val tag: Tag? = intent?.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        if (tag == null) {
            Log.e(TAG, "No NFC tag found in intent")
            return
        }

        val uid = tag.id?.joinToString("") { "%02X".format(it) }
        if (uid == null) {
            Log.e(TAG, "Failed to get UID from tag")
            return
        }

        Log.d(TAG, "NFC tag detected with UID: $uid")
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
            Text(
                "$${"%.2f".format(amount)}",
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 48.sp)
            )
            Spacer(Modifier.height(24.dp))
            Text("Please tap the customer's card", style = MaterialTheme.typography.bodyLarge)
        }
    }

    @Composable
    private fun SuccessScreen(message: String) {
        val isSuccess = message.contains("success", ignoreCase = true) || message.contains("✅")
        val animationRes = if (isSuccess) R.raw.success_animation else R.raw.failure_animation
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(animationRes))
        val progress by animateLottieCompositionAsState(
            composition,
            iterations = 1,
            speed = 1.5f,
            restartOnPlay = false
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LottieAnimation(
                composition,
                progress,
                modifier = Modifier.size(180.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isSuccess) "Payment Successful!" else "Payment Failed",
                style = MaterialTheme.typography.headlineMedium,
                color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message.replace("✅", "").replace("❌", "").trim(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}