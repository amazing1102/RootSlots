package com.cet4.rootslots.tts

/** 发音引擎抽象:内置 Piper(优先)与系统 TTS(兜底)共用 */
interface SpeechEngine {
    val name: String
    /** 播报;重复调用时打断上一次(QUEUE_FLUSH 语义) */
    fun speak(text: String)
    /** 语速,0.5~1.5,默认 0.85(与系统 TTS 语义对齐) */
    fun setRate(rate: Float)
    fun shutdown()
}
