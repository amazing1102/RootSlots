package com.cet4.rootslots.ui.slots

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cet4.rootslots.data.ComboEntity
import com.cet4.rootslots.data.GamePrefs
import com.cet4.rootslots.data.Repository
import com.cet4.rootslots.data.Q
import com.cet4.rootslots.data.Seg
import com.cet4.rootslots.data.parseSegs
import com.cet4.rootslots.tts.TtsHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * 老虎机对局状态:结果先行(先抽真词,转轴再落位),
 * 词根轴先停 → 前缀 → 后缀;落定后发音并结算。
 */
class SlotViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository.get(app)
    private val prefs = GamePrefs(app)
    private val tts = TtsHelper(app)

    var ready by mutableStateOf(false); private set

    // HUD
    var coins by mutableStateOf(GamePrefs.COINS_START); private set
    var energy by mutableStateOf(GamePrefs.ENERGY_MAX); private set
    var totalSpins by mutableStateOf(0); private set
    var combo by mutableStateOf(0); private set          // 本场连续转动数
    var familyStreak by mutableStateOf(0); private set   // 同词根家族连续数
    var lastReward by mutableStateOf(0); private set

    // 当前词
    var current by mutableStateOf<ComboEntity?>(null); private set
    var segs by mutableStateOf<List<Seg>>(emptyList()); private set
    var gloss by mutableStateOf<String?>(null); private set
    var litIndices by mutableStateOf(emptySet<Int>()); private set   // 已停稳的轴(左→右逐段点亮)
    var spinning by mutableStateOf(false); private set
    var spinId by mutableStateOf(0); private set          // 每次转动 +1,驱动转轴重建

    // 转轴:轴数=段数,每根轴落一个真实词段(动态段轴,不再固定 P/R/S 三轴)
    var reels by mutableStateOf<List<ReelSpec>>(emptyList()); private set

    /** 一根转轴:type 决定颜色与 filler 池,target 是该词的真实段 */
    data class ReelSpec(val type: String, val target: String, val fillers: List<String>)

    // 设置(设置页读写,MainActivity 据此套主题)
    val themeMode: StateFlow<Int> = prefs.theme.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val speechRate: StateFlow<Float> = prefs.speechRate.stateIn(viewModelScope, SharingStarted.Eagerly, GamePrefs.SPEECH_RATE_DEFAULT)

    private var lastFamilyKey: String? = null
    private var pendingSpeak = false

    init {
        viewModelScope.launch {
            repo.awaitReady()
            ready = true
            prefs.wallet.collect { w ->
                coins = w.coins; energy = w.energy; totalSpins = w.totalSpins
            }
        }
        viewModelScope.launch {
            prefs.speechRate.collect { tts.setRate(it) }
        }
        viewModelScope.launch {
            while (true) {
                delay(GamePrefs.REGEN_MS)
                prefs.onRegenTick()
            }
        }
    }

    fun setThemeMode(mode: Int) { viewModelScope.launch { prefs.setTheme(mode) } }
    fun setSpeechRate(rate: Float) { viewModelScope.launch { prefs.setSpeechRate(rate); tts.setRate(rate) } }

    fun prefixFillers(): List<String> = repo.prefixFillers
    fun rootFillers(): List<String> = repo.rootFillers
    fun suffixFillers(): List<String> = repo.suffixFillers

    fun meaningOf(type: String, key: String): String = repo.meaningOf(type, key)
    fun comboFor(w: String): ComboEntity? = repo.comboMap[w]
    fun glossOf(w: String): String? = repo.word(w)?.g
    fun familyWordList(key: String): List<String> = repo.familyWordList(key)
    fun familyEntries(): List<com.cet4.rootslots.data.FamilyEntity> =
        repo.families.values.sortedByDescending { it.count }
    suspend fun spinTotal() = repo.spinTotal()
    suspend fun spunDistinct() = repo.spunDistinct()
    suspend fun spunWordSet() = repo.spunWordSet()
    fun pronounce(w: String) = tts.speak(w)
    suspend fun isFavorite(w: String): Boolean = repo.isFavorite(w)
    suspend fun toggleFavorite(w: String): Boolean = repo.toggleFavorite(w)
    suspend fun favorites() = repo.favorites()
    suspend fun dueWords() = repo.dueWords()
    suspend fun dueCount() = repo.dueCount()
    suspend fun answerReview(w: String, known: Boolean) = repo.answerReview(w, known)
    suspend fun srsOf(w: String) = repo.srsOf(w)
    suspend fun addCoins(n: Int) = prefs.addCoins(n)

    /** 组一套测验题:从有释义的词池抽,三模式 */
    fun buildQuiz(mode: String, count: Int): List<Q> = buildList {
        val pool = repo.glossedWords()
        if (pool.size < 8) return@buildList
        val rng = Random(System.nanoTime())
        val words = pool.shuffled(rng).take(count)
        val letters = "abcdefghijklmnopqrstuvwxyz"
        for (entry in words) {
            val distractors = pool.filter { it.w != entry.w }.shuffled(rng).take(3).map { it.w }
            when (mode) {
                "gloss" -> add(Q(
                    kind = "gloss", prompt = entry.g ?: "", aux = "",
                    options = (distractors + entry.w).shuffled(rng), answer = entry.w, word = entry.w,
                ))
                "sound" -> add(Q(
                    kind = "sound", prompt = "🔊 听发音,选出单词", aux = "",
                    options = (distractors + entry.w).shuffled(rng), answer = entry.w, word = entry.w,
                ))
                else -> {  // spell
                    val w = entry.w
                    if (w.length < 5) {
                        add(Q("spell", w, entry.g ?: "", listOf(w), w, w))  // 短词整词认读
                        continue
                    }
                    val k = (w.length / 4).coerceIn(2, 4)
                    val positions = (w.indices).shuffled(rng).take(k).toSortedSet()
                    val missing = positions.map { w[it] }.joinToString("")
                    val masked = w.mapIndexed { i, ch -> if (i in positions) '_' else ch }.joinToString("")
                    val opts = mutableSetOf(missing)
                    while (opts.size < 4) {
                        val mutated = missing.map { c ->
                            if (rng.nextInt(3) == 0) letters[rng.nextInt(26)] else c
                        }.joinToString("")
                        if (mutated != missing) opts.add(mutated)
                    }
                    add(Q(
                        kind = "spell", prompt = masked, aux = entry.g ?: "",
                        options = opts.toList().shuffled(rng), answer = missing, word = w,
                    ))
                }
            }
        }
    }
    fun familyMeaning(key: String): String = repo.family(key)?.meaning ?: ""
    fun familyWords(key: String): Int = repo.family(key)?.count ?: 0

    fun spin() {
        if (spinning || !ready) return
        viewModelScope.launch {
            if (!prefs.tryConsumeEnergy()) return@launch
            val c = repo.randomCombo(current?.w) ?: return@launch   // 词库未就绪,不消耗能量
            val wordSegs = parseSegs(c.segsJson)

            // 结算先行(转轴只是把结果演出来)
            combo += 1
            var reward = (10 * (1 + minOf(combo, 20) * 0.05)).roundToInt()
            if (c.family == lastFamilyKey) familyStreak += 1 else familyStreak = 1
            if (familyStreak >= 2) reward += 5 * (familyStreak - 1)
            lastFamilyKey = c.family
            lastReward = reward
            prefs.addCoins(reward)
            prefs.incSpins()

            repo.recordSpin(c.w)
            current = c
            segs = wordSegs
            gloss = repo.word(c.w)?.g
            reels = wordSegs.map { seg ->
                ReelSpec(
                    type = seg.t,
                    target = seg.s,
                    fillers = when (seg.t) {
                        "P" -> repo.prefixFillers
                        "S" -> repo.suffixFillers
                        else -> repo.rootFillers
                    },
                )
            }
            litIndices = emptySet()
            pendingSpeak = true
            spinning = true
            spinId += 1
        }
    }

    /** 转轴停稳回调:左→右依次停,点亮对应段;全部停稳后发音结算 */
    fun onReelStopped(index: Int) {
        litIndices = litIndices + index
        if (litIndices.size >= reels.size && spinning) {
            spinning = false
            val w = current?.w
            if (w != null && pendingSpeak) {
                pendingSpeak = false
                tts.speak(w)
            }
        }
    }

    override fun onCleared() {
        tts.shutdown()
        super.onCleared()
    }
}
