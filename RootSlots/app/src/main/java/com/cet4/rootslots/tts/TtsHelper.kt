package com.cet4.rootslots.tts

import android.content.Context

/**
 * 发音门面:先用系统 TTS 立即可用,后台加载内置 Piper;
 * Piper 就绪后自动切换(更自然的美式读音),失败则留在系统 TTS。
 * 对外 API 与旧 TtsHelper 一致(speak/setRate/shutdown),调用方无感。
 */
class TtsHelper(context: Context) {
    @Volatile private var engine: SpeechEngine? = null
    var lastStatus: String = "发音引擎初始化中…"; private set
    var onStatus: ((String) -> Unit)? = null

    init {
        val sys = SystemTtsEngine(context)
        engine = sys
        setStatus("系统 TTS · ${sys.name}(内置语音加载中)")

        val piper = PiperEngine(context)
        piper.onReady = { ok, msg ->
            if (ok) {
                engine = piper
                sys.shutdown()
                setStatus(piper.name)
            } else {
                piper.shutdown()
                setStatus("系统 TTS(Piper 不可用:$msg)")
            }
        }
    }

    private fun setStatus(s: String) {
        lastStatus = s
        onStatus?.invoke(s)
    }

    fun speak(text: String) = engine?.speak(text)

    fun setRate(rate: Float) = engine?.setRate(rate)

    fun shutdown() = engine?.shutdown()
}
