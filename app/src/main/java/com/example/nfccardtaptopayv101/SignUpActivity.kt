package com.example.nfccardtaptopayv101

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

class SignUpActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private val client = OkHttpClient()

    private var pendingJson: JSONObject? = null
    private var isWaitingForCard = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            SignUpScreen { jsonString ->
                pendingJson = JSONObject(jsonString)
                isWaitingForCard = true
                Toast.makeText(this, "✅ Now tap your NFC card to continue...", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ---------- NFC foreground dispatch ----------
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

    // ---------- Receive card tap ----------
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (!isWaitingForCard || pendingJson == null) return

        val tag: Tag? = intent?.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        val uid = tag?.id?.joinToString("") { "%02X".format(it) } ?: return

        pendingJson?.put("card_uid", uid)
        isWaitingForCard = false       // block double‑reads

        lifecycleScope.launch(Dispatchers.IO) {
            val body = pendingJson.toString()
                .toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://promoted-quetzal-visually.ngrok-free.app/register_user/")
                .post(body)
                .build()

            try {
                client.newCall(request).execute().use { resp ->
                    val msg = if (resp.isSuccessful) {
                        JSONObject(resp.body?.string() ?: "{}")
                            .optString("message", "User registered")
                    } else "Registration error: ${resp.code}"

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SignUpActivity, msg, Toast.LENGTH_LONG).show()
                        pendingJson = null
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SignUpActivity, "Submission failed", Toast.LENGTH_SHORT).show()
                    pendingJson = null
                }
            }
        }
    }
}
