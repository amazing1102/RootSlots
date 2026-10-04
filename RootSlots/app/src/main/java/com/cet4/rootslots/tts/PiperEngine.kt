package com.cet4.rootslots.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import java.util.concurrent.Executors

/**
 * 内置离线神经语音:sherpa-onnx + Piper en_US-amy(fp16,assets/tts/)。
 * 模型在 assets 中由 OfflineTts(assetManager) 直接读取;合成+播放走单线程队列,
 * generation 计数实现"新播报打断旧播报"。
 */
class PiperEngine(private val context: Context) : SpeechEngine {
    override val name = "内置美式语音 · Piper amy"

    private val executor = Executors.newSingleThreadExecutor()
    private var tts: OfflineTts? = null
    private var track: AudioTrack? = null
    @Volatile private var ready = false
    @Volatile private var rate = 0.85f
    @Volatile private var generation = 0L
    /** (是否成功, 说明) —— 加载完成后回调一次 */
    var onReady: ((Boolean, String) -> Unit)? = null

    init {
        executor.execute {
            try {
                val t0 = System.currentTimeMillis()
                val config = OfflineTtsConfig(
                    model = OfflineTtsModelConfig(
                        vits = OfflineTtsVitsModelConfig(
                            model = "tts/en_US-amy-medium.onnx",
                            tokens = "tts/tokens.txt",
                            // espeak-ng-data 必须是真实文件路径(官方示例 copyDataDir),
                            // 首次启动从 assets 递归拷到 filesDir,之后幂等跳过
                            dataDir = copyEspeakData(),
                        ),
                    ),
                )
                val t = OfflineTts(assetManager = context.assets, config = config)
                tts = t
                ready = true
                Log.i(TAG, "piper ready in ${System.currentTimeMillis() - t0}ms")
                onReady?.invoke(true, name)
            } catch (e: Throwable) {
                Log.e(TAG, "piper init failed", e)
                onReady?.invoke(false, e.message ?: "init failed")
            }
        }
    }

    /** assets/tts/espeak-ng-data → filesDir/tts/espeak-ng-data(递归复制,一次) */
    private fun copyEspeakData(): String {
        val outDir = java.io.File(context.filesDir, "tts/espeak-ng-data")
        if (outDir.isDirectory && (outDir.list()?.size ?: 0) > 100) return outDir.absolutePath
        fun walk(assetPath: String, out: java.io.File) {
            out.mkdirs()
            val names = context.assets.list(assetPath) ?: emptyArray()
            for (name in names) {
                val a = "$assetPath/$name"
                val f = java.io.File(out, name)
                if ((context.assets.list(a)?.size ?: 0) > 0) {
                    walk(a, f)
                } else {
                    context.assets.open(a).use { input ->
                        f.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }
        }
        walk("tts/espeak-ng-data", outDir)
        return outDir.absolutePath
    }

    override fun speak(text: String) {
        if (!ready) return
        val t = tts ?: return
        val gen = ++generation
        executor.execute {
            if (gen != generation) return@execute   // 已被更新的播报打断
            try {
                stopTrack()
                val audio = t.generate(
                    text = text,
                    sid = 0,
                    // 语速滑杆以系统 TTS 0.85=常态标定,换算回 piper 的 speed(1.0=常态)
                    speed = (rate / 0.85f).coerceIn(0.4f, 2f),
                )
                Log.i(TAG, "generated ${audio.samples.size} samples @${audio.sampleRate}Hz")
                play(audio.samples, audio.sampleRate, gen)
            } catch (e: Throwable) {
                Log.e(TAG, "piper speak failed", e)
            }
        }
    }

    private fun play(samples: FloatArray, sampleRate: Int, gen: Long) {
        if (samples.isEmpty()) return
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT
        ).coerceAtLeast(4096)
        val tr = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            maxOf(minBuf, 8192), AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE,
        )
        track = tr
        tr.play()
        var off = 0
        val chunk = 8192
        while (off < samples.size && gen == generation) {   // 播放中被打断则停止写入
            val n = minOf(chunk, samples.size - off)
            tr.write(samples, off, n, AudioTrack.WRITE_BLOCKING)
            off += n
        }
        tr.stop()
        tr.release()
        if (track === tr) track = null
    }

    private fun stopTrack() {
        track?.let { runCatching { it.stop() }; runCatching { it.release() } }
        track = null
    }

    override fun setRate(rate: Float) {
        this.rate = rate.coerceIn(0.5f, 1.5f)
    }

    override fun shutdown() {
        generation++          // 打断合成/播放
        stopTrack()
        executor.execute {
            runCatching { tts?.release() }
            tts = null
        }
        executor.shutdown()
    }

    companion object { private const val TAG = "TTS" }
}
