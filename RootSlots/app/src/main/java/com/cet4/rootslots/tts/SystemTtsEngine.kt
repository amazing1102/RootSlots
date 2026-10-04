package com.cet4.rootslots.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** 系统 TTS 引擎(兜底):无内置模型时使用,读音质量取决于设备引擎 */
class SystemTtsEngine(context: Context) : SpeechEngine, TextToSpeech.OnInitListener {
    override val name = "系统 TTS(en-US)"
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    @Volatile private var ready = false
    @Volatile private var rate = 0.85f

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setSpeechRate(rate)
            ready = true
        }
    }

    override fun speak(text: String) {
        if (!ready) return
        tts?.setSpeechRate(rate)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "rootslots")
    }

    override fun setRate(rate: Float) {
        this.rate = rate.coerceIn(0.5f, 1.5f)
        if (ready) tts?.setSpeechRate(this.rate)
    }

    override fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
