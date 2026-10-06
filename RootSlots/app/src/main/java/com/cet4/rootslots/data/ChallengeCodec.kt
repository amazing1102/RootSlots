package com.cet4.rootslots.data

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.DataFormatException
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * 战书码编解码:JSON → Deflate → Base64URL,`RS1.<payload>.<crc16hex>`。
 * 题目连释义带选项整体入码,双方词库版本无需一致;CRC 校验和防手滑损坏
 * (v1 朋友威胁模型只防损坏不防篡改,二期再上 HMAC)。文本码 ~1KB,微信可直接发送。
 */
object ChallengeCodec {
    const val CODE_VERSION = 1
    private const val PREFIX = "RS1"

    /** 一道固化进码的对战题:k = g 释义选词 / s 拼写补全;拼写题 m = 挖位后的词面 */
    data class DuelQ(val k: String, val w: String, val g: String, val m: String?, val o: List<String>, val a: Int)

    data class Challenge(
        val id: String,
        val from: String,
        val bet: Int,
        val questions: List<DuelQ>,
        val challengerScore: Int,
        val challengerMs: Long,
    ) {
        /** q 数组原文,落库 duels.quiz_json 供应战方答题(M19b)使用 */
        fun questionsJson(): String {
            val arr = JSONArray()
            for (q in questions) {
                val o = JSONObject()
                o.put("k", q.k); o.put("w", q.w); o.put("g", q.g)
                if (q.m != null) o.put("m", q.m)
                o.put("o", JSONArray(q.o)); o.put("a", q.a)
                arr.put(o)
            }
            return arr.toString()
        }

        companion object {
            fun fromQuestionsJson(id: String, from: String, bet: Int, score: Int, ms: Long, quizJson: String): Challenge {
                val arr = JSONArray(quizJson)
                val qs = mutableListOf<DuelQ>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val opts = mutableListOf<String>()
                    val oa = o.getJSONArray("o")
                    for (j in 0 until oa.length()) opts.add(oa.getString(j))
                    qs.add(DuelQ(o.getString("k"), o.getString("w"), o.getString("g"),
                        o.optString("m").ifBlank { null }, opts, o.getInt("a")))
                }
                return Challenge(id, from, bet, qs, score, ms)
            }
        }
    }

    data class Receipt(val id: String, val to: String, val score: Int, val ms: Long)

    fun newDuelId(): String = "%08x".format(java.util.Random().nextLong() and 0xffffffffL)

    fun encodeChallenge(c: Challenge): String {
        val root = JSONObject()
        root.put("v", CODE_VERSION)
        root.put("id", c.id)
        root.put("from", c.from)
        root.put("bet", c.bet)
        val arr = JSONArray()
        for (q in c.questions) {
            val o = JSONObject()
            o.put("k", q.k); o.put("w", q.w); o.put("g", q.g)
            if (q.m != null) o.put("m", q.m)
            o.put("o", JSONArray(q.o)); o.put("a", q.a)
            arr.put(o)
        }
        root.put("q", arr)
        root.put("my", JSONObject().put("s", c.challengerScore).put("ms", c.challengerMs))
        return pack(root.toString())
    }

    /** 任何一步失败(格式不识别/码损坏)返回 null,由 UI 提示重试 */
    fun decodeChallenge(code: String): Challenge? = runCatching {
        val root = JSONObject(unpack(code.trim()))
        val arr = root.getJSONArray("q")
        val qs = mutableListOf<DuelQ>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val opts = mutableListOf<String>()
            val oa = o.getJSONArray("o")
            for (j in 0 until oa.length()) opts.add(oa.getString(j))
            qs.add(DuelQ(o.getString("k"), o.getString("w"), o.getString("g"),
                o.optString("m").ifBlank { null }, opts, o.getInt("a")))
        }
        val my = root.getJSONObject("my")
        Challenge(root.getString("id"), root.getString("from"), root.getInt("bet"),
            qs, my.getInt("s"), my.getLong("ms"))
    }.getOrNull()

    fun encodeReceipt(r: Receipt): String = pack(
        JSONObject()
            .put("v", CODE_VERSION)
            .put("id", r.id)
            .put("to", r.to)
            .put("s", r.score)
            .put("ms", r.ms)
            .toString()
    )

    fun decodeReceipt(code: String): Receipt? = runCatching {
        val o = JSONObject(unpack(code.trim()))
        Receipt(o.getString("id"), o.getString("to"), o.getInt("s"), o.getLong("ms"))
    }.getOrNull()

    private fun pack(json: String): String {
        val def = Deflater(Deflater.BEST_COMPRESSION, true)
        def.setInput(json.toByteArray(Charsets.UTF_8))
        def.finish()
        val buf = ByteArrayOutputStream()
        val out = ByteArray(4096)
        while (!def.finished()) {
            val n = def.deflate(out)
            if (n == 0) break
            buf.write(out, 0, n)
        }
        def.end()
        val bytes = buf.toByteArray()
        val crc = CRC32().apply { update(bytes) }.value
        val b64 = Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        return "$PREFIX.$b64.${java.lang.Long.toHexString(crc)}"
    }

    private fun unpack(code: String): String {
        val parts = code.split(".")
        require(parts.size == 3 && parts[0] == PREFIX) { "not a duel code" }
        val bytes = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val actual = CRC32().apply { update(bytes) }.value
        require(parts[2].toLong(16) == actual) { "crc mismatch" }
        val inf = Inflater(true)
        inf.setInput(bytes)
        val buf = ByteArrayOutputStream()
        val out = ByteArray(4096)
        while (!inf.finished()) {
            val n = try { inf.inflate(out) } catch (e: DataFormatException) { throw IllegalStateException("inflate failed", e) }
            if (n == 0) {
                if (inf.needsInput()) throw IllegalStateException("truncated")
            } else {
                buf.write(out, 0, n)
            }
        }
        inf.end()
        return buf.toString("UTF-8")
    }
}
