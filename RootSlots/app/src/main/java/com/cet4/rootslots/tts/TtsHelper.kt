package com.cet4.rootslots.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** 单词发音(美音);TTS 引擎异步初始化,未就绪时静默跳过;语速可在设置中调 */
class TtsHelper(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    @Volatile private var ready = false
    @Volatile private var rate: Float = 0.85f

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setSpeechRate(rate)
            ready = true
        }
    }

    fun setRate(r: Float) {
        rate = r.coerceIn(0.5f, 1.5f)
        if (ready) tts?.setSpeechRate(rate)
    }

    fun speak(text: String) {
        if (!ready) return
        tts?.setSpeechRate(rate)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "rootslots")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
