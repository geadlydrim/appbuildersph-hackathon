// PROTOTYPE: throwaway check of Android's built-in ON-DEVICE speech recognizer for the voice issue.
// Not the product. Reports which languages work offline, then lets you speak and timestamps results.
package ph.commutenity.sttcheck

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

private const val TAG = "STT"
private val LANGS = listOf("en-GB", "en-US", "fil-PH")

class MainActivity : Activity() {
    private lateinit var out: TextView
    private var recognizer: SpeechRecognizer? = null
    private var t0 = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        out = TextView(this).apply { textSize = 13f; typeface = Typeface.MONOSPACE; setPadding(24, 24, 24, 24) }
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        LANGS.forEach { lang -> buttons.addView(Button(this).apply { text = lang; setOnClickListener { listen(lang) } }) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            fitsSystemWindows = true
            addView(buttons, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(ScrollView(this@MainActivity).apply { addView(out) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        setContentView(root)
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
        val auto = intent.getStringExtra("lang")
        if (auto == null) report() else listen(auto)
    }

    /** `adb shell am start ... --es lang en-GB` starts a recording without touching the screen. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra("lang")?.let { listen(it) }
    }

    private fun log(line: String) {
        Log.i(TAG, line)
        runOnUiThread { out.append(line + "\n") }
    }

    private fun report() {
        log("onDeviceAvailable=${SpeechRecognizer.isOnDeviceRecognitionAvailable(this)} anyAvailable=${SpeechRecognizer.isRecognitionAvailable(this)}")
        if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) return
        val r = SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        r.checkRecognitionSupport(intent, mainExecutor, object : RecognitionSupportCallback {
            override fun onSupportResult(s: RecognitionSupport) {
                log("INSTALLED_OFFLINE=${s.installedOnDeviceLanguages}")
                log("SUPPORTED_OFFLINE=${s.supportedOnDeviceLanguages}")
                log("PENDING=${s.pendingOnDeviceLanguages}")
                log("ONLINE_ONLY=${s.onlineLanguages.size} langs")
                r.destroy()
            }
            override fun onError(error: Int) { log("SUPPORT_ERROR=$error"); r.destroy() }
        })
    }

    private fun listen(lang: String) {
        recognizer?.destroy()
        val r = SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) { t0 = System.nanoTime(); log("[$lang] READY - speak now") }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() { t0 = System.nanoTime(); log("[$lang] end of speech") }
            override fun onError(e: Int) {
                // 11 = server disconnected, 6 = no speech, 7 = no match. Keep going either way.
                log("[$lang] ERROR=$e")
                again(lang)
            }
            override fun onPartialResults(b: Bundle?) {
                // Partials are logged but not shown, so the screen stays readable.
                b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { Log.i(TAG, "[$lang] partial: $it") }
            }
            override fun onResults(b: Bundle?) {
                val ms = (System.nanoTime() - t0) / 1_000_000
                log("[$lang] FINAL (${ms} ms after end of speech): ${b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)}")
                again(lang)
            }
            override fun onEvent(t: Int, p: Bundle?) {}
        })
        r.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        })
    }

    /** Continuous mode: start listening again right after each result or error. */
    private fun again(lang: String) {
        out.postDelayed({ if (!isFinishing) listen(lang) }, 400)
    }

    override fun onDestroy() { recognizer?.destroy(); super.onDestroy() }
}
