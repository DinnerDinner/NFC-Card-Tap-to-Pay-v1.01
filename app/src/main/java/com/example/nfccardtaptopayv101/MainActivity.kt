package com.example.nfccardtaptopayv101
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.example.nfccardtaptopayv101.ui.SignUpScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private val client = OkHttpClient()

    private var pendingJson: JSONObject? = null
    private var isWaitingForCard = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            SignUpScreen { jsonString ->
                // Save form as JSON, wait for card tap
                pendingJson = JSONObject(jsonString)
                isWaitingForCard = true
                Toast.makeText(this, "✅ Now tap your NFC card to continue...", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )
        val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))
        val techList = arrayOf(arrayOf(android.nfc.tech.IsoDep::class.java.name))
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, filters, techList)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (!isWaitingForCard || pendingJson == null) return

        val tag: Tag? = intent?.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        val uid = tag?.id?.joinToString("") { "%02X".format(it) } ?: return

        pendingJson?.put("card_uid", uid)

        // Disable repeated reads
        isWaitingForCard = false

        // Send to backend
        lifecycleScope.launch(Dispatchers.IO) {
            val mediaType = "application/json".toMediaType()
            val body = pendingJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("https://b3bf-207-253-242-126.ngrok-free.app/register_user/")
                .post(body)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val msg = if (response.isSuccessful) {
                        JSONObject(response.body?.string() ?: "").optString("message", "User registered")
                    } else {
                        "Registration error: ${response.code}"
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                        val loggedInIntent = Intent(this@MainActivity, LoggedInActivity::class.java).apply {
                            putExtra("user_json", pendingJson.toString())
                        }
                        startActivity(loggedInIntent)
                        pendingJson = null
                    }


                }

            } catch (e: Exception) {
                e.printStackTrace()  // logs full exception to Logcat
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Submission failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    pendingJson = null
                }

            }
        }

    }
}

