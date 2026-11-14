package com.example.jarvis

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Always-listening loop using Android's built-in speech recognition (no Picovoice needed).
 * Say "Jarvis open WhatsApp" in one sentence, or say "Jarvis", wait for "Yes?", then the command.
 */
class WakeWordService : Service() {
    private var sr: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private val main = Handler(Looper.getMainLooper())
    private var running = false
    private var busy = false
    private var waitingCommand = false
    private val wake = Regex("jarvis|jervis|jarvi|service")

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        val ch = "jarvis"
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(ch, "Jarvis", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, ch).setContentTitle("Jarvis is listening")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now).build()
        startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        if (!running) {
            running = true
            tts = TextToSpeech(this) { tts?.language = Locale.getDefault() }
            main.postDelayed({ listen() }, 1000)
        }
        return START_STICKY
    }

    private fun speak(t: String) { tts?.speak(t, TextToSpeech.QUEUE_ADD, null, "j") }

    private fun listen() {
        if (!running || busy) return
        sr?.destroy()
        val r = SpeechRecognizer.createSpeechRecognizer(this)
        sr = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onResults(b: Bundle?) {
                val t = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.trim().orEmpty()
                handle(t)
            }
            override fun onError(e: Int) { main.postDelayed({ listen() }, 500) }
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(b: Bundle?) {}
            override fun onEvent(t: Int, b: Bundle?) {}
        })
        r.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        )
    }

    private fun handle(text: String) {
        if (waitingCommand) {
            waitingCommand = false
            if (text.isNotEmpty()) { run(text); return }
            listen(); return
        }
        val m = wake.find(text.lowercase())
        if (m == null) { listen(); return }
        val cmd = text.substring(m.range.last + 1).trim(' ', ',', '.', '!', '?')
        if (cmd.isEmpty()) {
            waitingCommand = true
            speak("Yes?")
            main.postDelayed({ listen() }, 1000)
        } else {
            run(cmd)
        }
    }

    private fun run(cmd: String) {
        busy = true
        sr?.destroy(); sr = null
        Thread {
            AgentLoop.run(this, cmd) { s -> main.post { speak(s) } }
            main.postDelayed({ busy = false; listen() }, 2000)
        }.start()
    }

    override fun onDestroy() {
        running = false
        sr?.destroy()
        tts?.shutdown()
        super.onDestroy()
    }
}
