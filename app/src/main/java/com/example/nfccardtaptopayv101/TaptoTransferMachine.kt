package com.example.nfccardtaptopayv101
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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

private enum class ScreenState { ENTER_AMOUNT, WAIT_TAP, SUCCESS }

class TaptoTransferMachine : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private val client = OkHttpClient()

    private var screenState by mutableStateOf(ScreenState.ENTER_AMOUNT)
    private var amountToCharge by mutableStateOf(0f)
    private var successMessage by mutableStateOf("")
    private lateinit var merchantEmail: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        merchantEmail = prefs.getString("user_data", null)
            ?.let { JSONObject(it).getString("email") }
            ?: ""

        setContent {
            MerchantRootUI()
        }
    }

    override fun onResume() {
        super.onResume()
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
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
        if (screenState != ScreenState.WAIT_TAP) return

        val tag: Tag? = intent?.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        val uid = tag?.id?.joinToString("") { "%02X".format(it) } ?: return

        postPurchase(uid)
    }

    private fun postPurchase(cardUid: String) {
        val json = JSONObject().apply {
            put("uid", cardUid)
            put("merchant_email", merchantEmail)
            put("amount", amountToCharge)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://promoted-quetzal-visually.ngrok-free.app/transfer")
                .post(body)
                .build()

            try {
                client.newCall(request).execute().use { resp ->
                    val responseText = resp.body?.string() ?: "{}"
                    val jsonResponse = JSONObject(responseText)

                    successMessage = if (resp.isSuccessful) {
                        jsonResponse.optString("message", "✅ Payment successful")
                    } else {
                        // Show detailed backend error message
                        jsonResponse.optString("detail", "❌ Payment failed")
                    }

                    withContext(Dispatchers.Main) {
                        screenState = ScreenState.SUCCESS
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

    @Composable
    private fun MerchantRootUI() {
        when (screenState) {
            ScreenState.ENTER_AMOUNT -> EnterAmountScreen()
            ScreenState.WAIT_TAP -> TapCardScreen()
            ScreenState.SUCCESS -> SuccessScreen()
        }
    }

    @Composable
    private fun EnterAmountScreen() {
        var input by remember { mutableStateOf("") }
        val context = LocalContext.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Enter Amount to Charge", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Amount (CAD)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    val amt = input.toFloatOrNull()
                    if (amt != null && amt > 0f) {
                        amountToCharge = amt
                        screenState = ScreenState.WAIT_TAP
                        Toast.makeText(
                            context,
                            "Tap customer card to charge $${String.format("%.2f", amt)}",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Request Payment")
            }
        }
    }

    @Composable
    private fun TapCardScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$${String.format("%.2f", amountToCharge)}",
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 48.sp)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Please tap the customer's NFC card",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }

    @Composable
    private fun SuccessScreen() {
        val context = LocalContext.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(successMessage, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(32.dp))
            Button(onClick = {
                screenState = ScreenState.ENTER_AMOUNT
                amountToCharge = 0f
            }) {
                Text("New Payment")
            }
            Spacer(Modifier.height(16.dp))
            val context = LocalContext.current
            Button(onClick = {
                context.startActivity(Intent(context, MainActivity::class.java).apply {
                    putExtra("goToProfile", true)
                })
                (context as? Activity)?.finish()
            }) {
                Text("Back to Profile")
            }
        }
    }
}
