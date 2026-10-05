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
    var engineStatus by mutableStateOf("发音引擎初始化中…"); private set

    // HUD
    var coins by mutableStateOf(GamePrefs.COINS_START); private set
    var energy by mutableStateOf(GamePrefs.ENERGY_MAX); private set
    var totalSpins by mutableStateOf(0); private set
    var spinsToday by mutableStateOf(0); private set     // 今日已转(每日目标进度)
    var dailyGoal by mutableStateOf(GamePrefs.DAILY_GOAL_DEFAULT); private set
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

    // 落定开考(方案A):停轴后 4 选 1 猜释义,答对 1.5× / 答错 -3 且词进生词本 / 跳过半价
    // 锈词折损(方案C):生词本逾期未复习的词再转出,收益固定 +2 且连击清零
    data class QuizState(val options: List<String>, val answer: String)
    var quiz by mutableStateOf<QuizState?>(null); private set
    var quizPicked by mutableStateOf<String?>(null); private set   // 已选选项;"·skip·"=跳过
    var quizResult by mutableStateOf<String?>(null); private set   // 结算提示行
    var lastRust by mutableStateOf(false); private set
    var wrongDialogWord by mutableStateOf<String?>(null); private set // 答错弹窗:展示「已移入生词本」(null=不弹/已关)
    private var pendingRust = false

    val wrongNotify: StateFlow<Boolean> = prefs.wrongNotify.stateIn(viewModelScope, SharingStarted.Eagerly, GamePrefs.WRONG_NOTIFY_DEFAULT)

    /** 关闭答错弹窗;勾选「下次不用提醒」时静默化后续答错 */
    fun dismissWrongDialog(dontAskAgain: Boolean) {
        wrongDialogWord = null
        if (dontAskAgain) viewModelScope.launch { prefs.setWrongNotify(false) }
    }

    // 转轴:轴数=段数,每根轴落一个真实词段(动态段轴,不再固定 P/R/S 三轴)
    var reels by mutableStateOf<List<ReelSpec>>(emptyList()); private set

    /** 一根转轴:type 决定颜色与 filler 池,target 是该词的真实段 */
    data class ReelSpec(val type: String, val target: String, val fillers: List<String>)

    // 设置(设置页读写,MainActivity 据此套主题)
    val themeMode: StateFlow<Int> = prefs.theme.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val speechRate: StateFlow<Float> = prefs.speechRate.stateIn(viewModelScope, SharingStarted.Eagerly, GamePrefs.SPEECH_RATE_DEFAULT)

    private var lastFamilyKey: String? = null

    init {
        engineStatus = tts.lastStatus
        tts.onStatus = { engineStatus = it }
        viewModelScope.launch {
            repo.awaitReady()
            ready = true
            prefs.wallet.collect { w ->
                coins = w.coins; energy = w.energy; totalSpins = w.totalSpins
                spinsToday = w.spinsToday; dailyGoal = w.dailyGoal
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
    fun updateDailyGoal(n: Int) { viewModelScope.launch { prefs.setDailyGoal(n) } }

    fun prefixFillers(): List<String> = repo.prefixFillers
    fun rootFillers(): List<String> = repo.rootFillers
    fun suffixFillers(): List<String> = repo.suffixFillers

    fun meaningOf(type: String, key: String): String = repo.meaningOf(type, key)
    fun comboFor(w: String): ComboEntity? = repo.comboMap[w]
    fun glossOf(w: String): String? = repo.word(w)?.g
    fun ipaOf(w: String): String? = repo.ipaOf(w)
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
    suspend fun weeklyReviews() = repo.weeklyReviews()
    suspend fun memoryAchievement() = repo.memoryAchievement()
    suspend fun addCoins(n: Int) = prefs.addCoins(n)

    /** 组一套测验题:从有释义的词池抽(音标题只取有音标的词),四模式 */
    fun buildQuiz(mode: String, count: Int): List<Q> = buildList {
        val base = repo.glossedWords()
        val pool = if (mode == "ipa") base.filter { !it.ipa.isNullOrBlank() } else base
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
                "ipa" -> add(Q(
                    kind = "ipa", prompt = "/${entry.ipa}/", aux = entry.g ?: "",
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

            // 锈词判定(C):已收藏且到期未复习(未毕业)
            val srs = repo.srsOf(c.w)
            pendingRust = srs != null &&
                srs.dueAt <= System.currentTimeMillis() &&
                srs.stage < Repository.STAGE_GRADUATED
            lastRust = pendingRust

            // 结算延后到答题后(A);此刻只出题面
            quiz = null; quizPicked = null; quizResult = null; lastReward = 0
            wrongDialogWord = null
            repo.recordSpin(c.w)
            prefs.incSpins()
            current = c
            segs = parseSegs(c.segsJson)
            gloss = repo.word(c.w)?.g
            reels = segs.map { seg ->
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
            spinning = true
            spinId += 1
        }
    }

    /** 转轴停稳回调:左→右依次停;全部停稳后落定开考 */
    fun onReelStopped(index: Int) {
        litIndices = litIndices + index
        if (litIndices.size >= reels.size && spinning) {
            spinning = false
            viewModelScope.launch { buildQuizForCurrentWord() }
        }
    }

    private suspend fun buildQuizForCurrentWord() {
        val c = current ?: return
        val correct = gloss?.takeIf { it.isNotBlank() }
        val pool = repo.glossedWords()
            .filter { it.w != c.w && !it.g.isNullOrBlank() && it.g != correct }
            .distinctBy { it.g }
            .shuffled()
        if (correct == null || pool.size < 3) {
            settleQuiz(null)   // 无释义/词池不足,无法出题 → 按跳过结算
            return
        }
        val options = (pool.take(3).mapNotNull { it.g } + correct).shuffled()
        quiz = QuizState(options, correct)
    }

    fun answerQuiz(option: String) {
        val q = quiz ?: return
        if (quizPicked != null) return
        quizPicked = option
        settleQuiz(option == q.answer)
    }

    fun skipQuiz() {
        if (quiz == null || quizPicked != null) return
        quizPicked = "·skip·"
        settleQuiz(null)
    }

    /**
     * 统一结算:correct = true 答对(1.5×) / false 答错(-3,词进生词本,连击清零) /
     * null 跳过(0.5×)。锈词无论结果收益固定 +2 且连击清零。
     */
    private fun settleQuiz(correct: Boolean?) {
        val c = current ?: return
        val w = c.w
        viewModelScope.launch {
            if (correct == false) {
                combo = 0; familyStreak = 0; lastFamilyKey = null
                lastReward = 0
                val delta = -minOf(3, coins)
                prefs.addCoins(delta)
                val newly = !repo.isFavorite(w)
                if (newly) repo.toggleFavorite(w)   // 错词自动进生词本排期复习
                quizResult = if (newly) "答错了 −3🪙 · 已收进生词本" else "答错了 −3🪙"
                if (newly && wrongNotify.value) wrongDialogWord = w
            } else if (pendingRust) {
                combo = 0; familyStreak = 0; lastFamilyKey = null
                lastReward = 2
                prefs.addCoins(2)
                quizResult = "锈词 +2 · 先去复习"
            } else {
                combo += 1
                if (c.family == lastFamilyKey) familyStreak += 1 else familyStreak = 1
                lastFamilyKey = c.family
                val graduated = repo.srsOf(w)?.stage?.let { it >= Repository.STAGE_GRADUATED } == true
                val base = if (graduated) 15 else 10
                var reward = (base * (1 + minOf(combo, 20) * 0.05)).roundToInt()
                if (familyStreak >= 2) reward += 5 * (familyStreak - 1)
                reward = if (correct == true) (reward * 1.5).roundToInt() else (reward * 0.5).roundToInt()
                lastReward = reward
                prefs.addCoins(reward)
                quizResult = when (correct) {
                    true -> "答对了 · 1.5× +$reward🪙"
                    else -> "跳过 · 半价 +$reward🪙"
                }
            }
            tts.speak(w)
        }
    }
    override fun onCleared() {
        tts.shutdown()
        super.onCleared()
    }
}
