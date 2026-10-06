package com.cet4.rootslots.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

/**
 * 内存词库:一次加载全部组合词/词表/词族/词法成分,供老虎机随机抽取。
 * 首次启动时等待 Room 的 assets 预填协程写完。
 */
class Repository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val db = AppDatabase.get(appContext)
    private val prefs = GamePrefs(appContext)
    private val prefillMutex = kotlinx.coroutines.sync.Mutex()

    companion object SrsRules {
        /**
         * 艾宾浩斯记忆节点:stage 1..9 依次对应下方间隔,stage 10 = 已毕业(不再进入复习队列)。
         * 认识 → 下一节点;忘记 → 回到节点 1(5 分钟档)重新爬曲线。
         */
        val NODE_LABELS = listOf("5分钟", "30分钟", "12小时", "1天", "2天", "4天", "7天", "15天", "30天")
        val NODE_SHORT = listOf("5分", "30分", "12时", "1天", "2天", "4天", "7天", "15天", "30天")
        val NODE_MS = longArrayOf(
            5 * 60_000L,
            30 * 60_000L,
            12 * 3_600_000L,
            86_400_000L,
            2 * 86_400_000L,
            4 * 86_400_000L,
            7 * 86_400_000L,
            15 * 86_400_000L,
            30 * 86_400_000L,
        )
        const val STAGE_GRADUATED = 10
        const val DAY_MS = 86_400_000L

        /** 词库资产补灌版本:gd/例句/考试标签/扩库新词更新时递增,触发一次 enrichIfNeeded 重跑 */
        const val ASSETS_VER = "20261006c"

        /** 战书超时:发出/接受后 7 天未推进(无回执/未作答)自动作废,押注退回 */
        const val DUEL_TTL_MS = 7L * DAY_MS

        /** 下战书固定 7 题:释义 4 选 1 ×4 + 拼写补全 ×3,交替排列 */
        const val DUEL_QUESTIONS = 7
        const val DUEL_TIME_PER_Q_MS = 15_000L

        /** 档位名:0=新学,1..9=节点档,10=已毕业 */
        fun stageLabel(stage: Int): String = when {
            stage <= 0 -> "新学"
            stage >= STAGE_GRADUATED -> "已毕业"
            else -> "${NODE_LABELS[stage - 1]}档"
        }

        /**
         * 遗忘进度(0..1):上次复习后,当前档位间隔被"消耗"的比例。
         * 1 = 已到期/逾期;毕业词返回 -1(供 UI 显示满条金色"已完成");
         * 未排期返回 null。
         */
        fun forgetProgress(srs: SrsEntity?, now: Long = System.currentTimeMillis()): Float? = when {
            srs == null -> null
            srs.stage >= STAGE_GRADUATED -> -1f
            else -> {
                val interval = NODE_MS[(srs.stage - 1).coerceIn(0, NODE_MS.size - 1)].toFloat()
                (1f - (srs.dueAt - now) / interval).coerceIn(0f, 1f)
            }
        }

        /** 到期时间友好显示(含当天内的分钟/小时档) */
        fun dueLabel(dueAt: Long, now: Long = System.currentTimeMillis()): String = when {
            dueAt == Long.MAX_VALUE -> "长期记忆"
            dueAt <= now -> "今天复习"
            else -> {
                val diff = dueAt - now
                when {
                    diff < 3_600_000L -> "${(diff + 59_999) / 60_000}分钟后"
                    diff < DAY_MS -> "${(diff + 3_599_999) / 3_600_000}小时后"
                    else -> "${(diff + DAY_MS - 1) / DAY_MS}天后"
                }
            }
        }

        @Volatile private var inst: Repository? = null
        fun get(context: Context): Repository =
            inst ?: synchronized(this) {
                inst ?: Repository(context.applicationContext).also { inst = it }
            }
    }

    val combos = mutableListOf<ComboEntity>()
    val comboMap = mutableMapOf<String, ComboEntity>()
    val words = mutableMapOf<String, WordEntity>()
    val families = mutableMapOf<String, FamilyEntity>()
    private val morphByKey = mutableMapOf<Pair<String, String>, MorphEntity>()

    /** 三个转轴的滚动填充面 */
    val prefixFillers = mutableListOf<String>()
    val rootFillers = mutableListOf<String>()
    val suffixFillers = mutableListOf<String>()

    @Volatile var ready = false; private set

    // ---- 目标考试池(词库扩展 §8):标签交集过滤,收藏/SRS/统计永不过滤 ----
    private val _exams = MutableStateFlow(setOf(GamePrefs.EXAM_DEFAULT))
    val exams: StateFlow<Set<String>> = _exams

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            prefs.exams.collect { _exams.value = it }
        }
    }

    /** 词是否落在当前目标考试并集内(无标签的旧行按 cet4 兜底) */
    fun wordInPool(e: WordEntity): Boolean {
        val tags = e.examTags?.split(',')?.filter { it.isNotBlank() }?.ifEmpty { null }
            ?: setOf(GamePrefs.EXAM_DEFAULT)
        return tags.any { it in _exams.value }
    }

    suspend fun awaitReady() {
        if (ready) return
        ensurePrefilled()                            // 空库(首装/迁移/上次失败)就地补跑预填
        var tries = 0
        while (tries < 200 && db.combosDao().count() == 0) {
            delay(150); tries++
        }
        enrichIfNeeded()                             // 升级安装补灌新字段(多义项/例句/考试标签)
        load()
        if (combos.isEmpty()) {
            android.util.Log.e("Repo", "awaitReady: combos still empty after ${tries} polls")
        }
        ready = true
    }

    /**
     * 升级补灌:①DB v7 新增的 exam_tags/detail_gloss/sen_en/sen_zh,老安装经迁移升级后
     * 这些列是空的,从 assets 一次性补齐;②词库扩库后 words.json 超出库里行数的新词
     * (如阶段一的 gaokao 词)就地补插,只加行不动行,收藏/SRS 不受影响。
     * 闸门 = DataStore 的 ASSETS_VER,资产数据更新时递增 Repository.ASSETS_VER 即可重跑一次。
     */
    private suspend fun enrichIfNeeded() {
        if (prefs.assetsVer() == ASSETS_VER) return
        try {
            val gd = assetStringMap("gd.json")
            val exams = assetStringMap("exams.json")
            val sen = mutableMapOf<String, List<String>>()
            val ctx = appContext
            for (f in ctx.assets.list("sentences") ?: emptyArray()) {
                if (!f.endsWith(".json")) continue
                val obj = org.json.JSONObject(readAsset(ctx, "sentences/$f"))
                for (key in obj.keys()) {
                    val arr = obj.optJSONArray(key) ?: continue
                    sen[key] = listOf(arr.optString(0), arr.optString(1))
                }
            }
            val all = (gd.keys + exams.keys + sen.keys).distinct()
            for (w in all) {
                val tags = exams[w] ?: "cet4"
                db.wordsDao().enrichRow(w, gd[w], sen[w]?.getOrNull(0), sen[w]?.getOrNull(1), ",$tags,")
            }
            // 扩库新词补插:assets words.json 里库里还没有的行(IGNORE,不覆盖已有行)
            val known = db.wordsDao().allWords().toSet()
            val arr = org.json.JSONArray(readAsset(ctx, "words.json"))
            val fresh = mutableListOf<WordEntity>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val w = o.getString("w")
                if (w in known) continue
                fresh += WordEntity(
                    w = w,
                    g = if (o.isNull("g")) null else o.getString("g"),
                    ipa = if (o.isNull("i")) null else o.optString("i").ifBlank { null },
                    examTags = if (o.isNull("x")) null else o.optString("x").let { ",$it," },
                    detailGloss = if (o.isNull("gd")) null else o.optString("gd").ifBlank { null },
                    senEn = if (o.isNull("se")) null else o.optString("se").ifBlank { null },
                    senZh = if (o.isNull("sz")) null else o.optString("sz").ifBlank { null },
                )
            }
            if (fresh.isNotEmpty()) db.wordsDao().insertIgnore(fresh)
            // 结构表刷新:morphs/families/combos 为派生数据(不含用户数据),
            // 拆词/词根/词族资产更新时清表重灌(阶段二/三的自动拆词由此进库)
            db.morphsDao().clear()
            db.familiesDao().clear()
            db.combosDao().clear()
            AppDatabase.fillStructTables(appContext)
            prefs.setAssetsVer(ASSETS_VER)
            android.util.Log.i("Enrich", "done: gd=${gd.size} sen=${sen.size} exams=${exams.size} newWords=${fresh.size}")
        } catch (e: Exception) {
            android.util.Log.e("Enrich", "FAILED", e)
        }
    }

    private fun assetStringMap(name: String): Map<String, String> {
        val obj = org.json.JSONObject(readAsset(appContext, name))
        val out = mutableMapOf<String, String>()
        for (k in obj.keys()) out[k] = obj.optString(k)
        return out
    }

    private fun readAsset(context: Context, name: String): String =
        context.assets.open(name).bufferedReader().use { it.readText() }

    /** 幂等:库里没数据才从 assets 预填(不依赖 Room onCreate 时机) */
    private suspend fun ensurePrefilled() = prefillMutex.withLock {
        if (db.combosDao().count() == 0) {
            try {
                AppDatabase.prefill(appContext)
                android.util.Log.i("Prefill", "done(auto)")
            } catch (e: Exception) {
                android.util.Log.e("Prefill", "FAILED", e)
            }
        }
    }

    private suspend fun load() {
        if (combos.isNotEmpty()) return
        combos += db.combosDao().all()
        for (c in combos) comboMap[c.w] = c
        for (w in db.wordsDao().all()) words[w.w] = w
        for (f in db.familiesDao().all()) families[f.key] = f
        for (m in db.morphsDao().all()) {
            morphByKey[m.type to m.key] = m
            val sink = when (m.type) {
                "P" -> prefixFillers; "R" -> rootFillers; else -> suffixFillers
            }
            for (s in m.surfaceList.split(",")) if (s.isNotBlank()) sink += s
        }
    }

    private val rng = java.util.Random()

    /** 当前考试池内的组合词(缓存,考试集合变更时重算) */
    @Volatile private var poolCombos: List<ComboEntity> = emptyList()
    private var poolKey: Set<String> = emptySet()

    private fun examPoolCombos(): List<ComboEntity> {
        if (poolKey != _exams.value) {
            poolCombos = combos.filter { c -> words[c.w]?.let(::wordInPool) == true }
            poolKey = _exams.value
        }
        return poolCombos
    }

    /** 随机抽组合词(限当前考试池),避免与上一词重复;词库未就绪/池空返回 null */
    fun randomCombo(exclude: String? = null): ComboEntity? {
        val pool = examPoolCombos()
        if (pool.isEmpty()) return null
        repeat(8) {
            val c = pool[rng.nextInt(pool.size)]
            if (c.w != exclude) return c
        }
        return pool[rng.nextInt(pool.size)]
    }

    fun word(w: String): WordEntity? = words[w]

    /** 测验题池:有释义且落在当前考试池内 */
    fun glossedWords(): List<WordEntity> =
        words.values.filter { !it.g.isNullOrBlank() && wordInPool(it) }
    fun ipaOf(w: String): String? = words[w]?.ipa?.takeIf { it.isNotBlank() }
    fun family(key: String): FamilyEntity? = families[key]

    /** 某段的词法释义(P/R/S + key) */
    fun meaningOf(type: String, key: String): String {
        morphByKey[type to key]?.meaning?.takeIf { it.isNotBlank() }?.let { return it }
        return families[key]?.meaning ?: ""
    }

    /** 收藏/取消;收藏时建立 SRS 档案(新词立即首复习) */
    suspend fun toggleFavorite(w: String): Boolean {
        val dao = db.favoritesDao()
        return if (dao.isFavoriteNow(w)) {
            dao.delete(w); false
        } else {
            dao.insert(FavoriteEntity(w = w, addedAt = System.currentTimeMillis()))
            val now = System.currentTimeMillis()
            if (db.srsDao().byWord(w) == null) {
                db.srsDao().upsert(SrsEntity(w = w, stage = 0, dueAt = now, addedAt = now))
            }
            true
        }
    }

    suspend fun isFavorite(w: String): Boolean = db.favoritesDao().isFavoriteNow(w)

    suspend fun recordSpin(w: String) = db.spinsDao().insert(SpinEntity(w = w, at = System.currentTimeMillis()))

    /** 生词本条目:收藏 + 释义 + 音标 + SRS 档案 */
    data class FavRow(val w: String, val g: String?, val ipa: String?, val srs: SrsEntity?)

    suspend fun favorites(): List<FavRow> {
        val now = System.currentTimeMillis()
        return db.favoritesDao().all().map { f ->
            FavRow(f.w, words[f.w]?.g, ipaOf(f.w), db.srsDao().byWord(f.w))
        }
    }

    suspend fun dueWords(now: Long = System.currentTimeMillis()): List<String> =
        db.srsDao().due(now).map { it.w }

    suspend fun dueCount(): Int = db.srsDao().dueCount(System.currentTimeMillis())

    /** 复习作答(艾宾浩斯):认识 → 下一记忆节点(9 为 30 天,过后毕业);
     *  忘记 → 回到节点 1(5 分钟档)重新爬曲线。毕业词 dueAt=MAX 不再进队列。 */
    suspend fun answerReview(w: String, known: Boolean): SrsEntity? {
        val cur = db.srsDao().byWord(w) ?: return null
        val ns = if (known) (cur.stage + 1).coerceIn(1, STAGE_GRADUATED) else 1
        val dueAt = if (ns >= STAGE_GRADUATED) Long.MAX_VALUE
            else System.currentTimeMillis() + NODE_MS[ns - 1]
        val e = cur.copy(
            stage = ns,
            dueAt = dueAt,
            reviews = cur.reviews + 1,
            lapses = cur.lapses + if (known) 0 else 1,
        )
        db.srsDao().upsert(e)
        db.reviewLogDao().insert(
            ReviewLogEntity(w = w, at = System.currentTimeMillis(), known = known)
        )
        return e
    }

    /** 近 7 天每天的复习量(旧→新,今天在最后) */
    suspend fun weeklyReviews(): List<Int> {
        val day = DAY_MS
        val today0 = System.currentTimeMillis() / day * day   // UTC 零点;粗粒度统计足够
        return (6 downTo 0).map { back ->
            val from = today0 - back * day
            db.reviewLogDao().countBetween(from, from + day)
        }
    }

    /** 记忆曲线达成率:未逾期词占全部在学词的比例(0~1) */
    suspend fun memoryAchievement(): Float {
        val tracked = db.srsDao().count()
        if (tracked == 0) return 0f
        return 1f - db.srsDao().dueCount(System.currentTimeMillis()) / tracked.toFloat()
    }

    suspend fun srsOf(w: String): SrsEntity? = db.srsDao().byWord(w)
    suspend fun spinTotal(): Int = db.spinsDao().count()
    suspend fun spunDistinct(): Int = db.spinsDao().distinctWords()
    suspend fun spunWordSet(): Set<String> = db.spinsDao().distinctWordList().toSet()

    /** 图鉴族列表:词表按当前考试池过滤,空族不显示,count 为过滤后词数(§8.2) */
    fun familyEntriesFiltered(): List<FamilyEntity> =
        families.values.map { f ->
            f.copy(count = familyWordListFiltered(f.key).size)
        }.filter { it.count > 0 }
            .sortedByDescending { it.count }

    fun familyWordList(key: String): List<String> = familyWordListFiltered(key)

    /** 全库族词表(详情页「同族词」用:详情页永不过滤,§8.2) */
    fun familyWordListAll(key: String): List<String> =
        families[key]?.wordList?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

    private fun familyWordListFiltered(key: String): List<String> =
        families[key]?.wordList
            ?.split(",")
            ?.filter { s -> s.isNotBlank() && words[s]?.let(::wordInPool) == true }
            ?: emptyList()

    // ================= 好友 PK · 异步战书(方案 M19a) =================

    /** 下战书来源:薄弱 = 生词本按遗忘进度倒序;族 = 指定词根族内;随机 = 当前考试池 */
    enum class DuelSource { WEAK, FAMILY, RANDOM }

    /**
     * 挑词候选(多返回,由 UI 截取 DUEL_QUESTIONS 个):只取有释义的词(释义题必需)。
     * 薄弱序按 forgetProgress 倒序(最接近遗忘在前,毕业词垫底)。
     */
    suspend fun duelWordCandidates(source: DuelSource, familyKey: String?): List<WordEntity> {
        val pool: List<WordEntity> = when (source) {
            DuelSource.WEAK -> {
                val favs = db.favoritesDao().all().map { it.w }.toSet()
                val rows = mutableListOf<Pair<WordEntity, Float>>()
                for (w in favs) {
                    val e = words[w] ?: continue
                    val p = db.srsDao().byWord(w)?.let { forgetProgress(it) } ?: -2f
                    rows += e to p
                }
                rows.sortedByDescending { it.second }.map { it.first }
            }
            DuelSource.FAMILY -> familyWordListAll(familyKey ?: "").mapNotNull { words[it] }.shuffled(rng)
            DuelSource.RANDOM -> words.values.filter { wordInPool(it) }.shuffled(rng)
        }
        return pool.filter { !it.g.isNullOrBlank() }
    }

    /** 当前考试池内可做释义题干扰的词(词面集合) */
    private fun duelDistractorPool(exclude: Set<String>): List<String> =
        glossedWords().map { it.w }.filter { it !in exclude }.distinct()

    /**
     * 组对战题:题目/选项/答案在生成时全部固化(入码后跨版本不变)。
     * 释义题选项 = 3 个干扰词 + 正确词;拼写题按既有挖位算法,干扰项 = 缺失串同长变形。
     */
    fun buildDuelQuestions(pool: List<WordEntity>): List<ChallengeCodec.DuelQ> {
        val picked = pool.take(DUEL_QUESTIONS)
        if (picked.size < DUEL_QUESTIONS) return emptyList()
        val gr = kotlin.random.Random(System.nanoTime())
        val letters = "abcdefghijklmnopqrstuvwxyz"
        val distractors = duelDistractorPool(picked.map { it.w }.toSet())
        return picked.mapIndexed { i, e ->
            val w = e.w
            if (i % 2 == 0) {   // 偶数位 = 释义 4 选 1(题面 = 释义,选项 = 单词)
                val opts = (distractors.shuffled(gr).take(3) + w).shuffled(gr)
                ChallengeCodec.DuelQ(
                    k = "g", w = w, g = e.g ?: "", m = null,
                    o = opts, a = opts.indexOf(w),
                )
            } else {            // 奇数位 = 拼写补全(题面 = 挖位词面,选项 = 缺失字母串)
                val k = (w.length / 4).coerceIn(1, 4)
                val positions = (w.indices).shuffled(gr).take(k).toSortedSet()
                val missing = positions.map { w[it] }.joinToString("")
                val masked = w.mapIndexed { ci, ch -> if (ci in positions) '_' else ch }.joinToString("")
                val opts = mutableSetOf(missing)
                while (opts.size < 4) {
                    val mutated = missing.map { c ->
                        if (gr.nextInt(3) == 0) letters[gr.nextInt(26)] else c
                    }.joinToString("")
                    if (mutated != missing) opts.add(mutated)
                }
                val shuffled = opts.toList().shuffled(gr)
                ChallengeCodec.DuelQ(
                    k = "s", w = w, g = e.g ?: "", m = masked,
                    o = shuffled, a = shuffled.indexOf(missing),
                )
            }
        }
    }

    /** 生成战书(下战书方):托管扣除押注,落库 sent;余额不足返回 null */
    suspend fun createDuel(code: ChallengeCodec.Challenge): DuelEntity? {
        if (db.duelsDao().byCodeId(code.id) != null) return null
        val coins = prefs.wallet.first().coins
        if (coins < code.bet) return null
        prefs.addCoins(-code.bet)
        val e = DuelEntity(
            role = "challenger", codeId = code.id, opponent = "", bet = code.bet,
            quizJson = code.fullJson(),
            myScore = code.challengerScore, myMs = code.challengerMs,
            status = "sent", createdAt = System.currentTimeMillis(),
        )
        db.duelsDao().insert(e)
        return e
    }

    /** 导入战书码(应战方):解析失败/重复导入返回 null */
    suspend fun parseChallenge(codeText: String): ChallengeCodec.Challenge? {
        val c = ChallengeCodec.decodeChallenge(codeText) ?: return null
        if (db.duelsDao().byCodeId(c.id) != null) return null
        return c
    }

    /** 接受应战(应战方):托管扣除押注,落库 received;余额不足/重复返回 false */
    suspend fun acceptChallenge(c: ChallengeCodec.Challenge): Boolean {
        if (db.duelsDao().byCodeId(c.id) != null) return false
        val coins = prefs.wallet.first().coins
        if (coins < c.bet) return false
        prefs.addCoins(-c.bet)
        db.duelsDao().insert(
            DuelEntity(
                role = "defender", codeId = c.id, opponent = c.from, bet = c.bet,
                quizJson = c.fullJson(),          // 全量:题目 + 对方基准(含幽灵序列)
                status = "received", createdAt = System.currentTimeMillis(),
            ),
        )
        return true
    }

    /** 单方作废(v1 简化,朋友场景):押注原路退回 */
    suspend fun voidDuel(d: DuelEntity): Boolean {
        if (d.status != "sent" && d.status != "received") return false
        prefs.addCoins(d.bet)
        db.duelsDao().updateStatus(d.id, "expired", System.currentTimeMillis())
        return true
    }

    /** 超时清理(列表加载时调):sent/received 超 7 天自动作废退回,返回清理条数 */
    suspend fun expireStaleDuels(): Int {
        val now = System.currentTimeMillis()
        var n = 0
        for (d in db.duelsDao().active()) {
            if (d.status == "sent" || d.status == "received") {
                if (now - d.createdAt > DUEL_TTL_MS && voidDuel(d)) n++
            }
        }
        return n
    }

    suspend fun duels(): List<DuelEntity> = db.duelsDao().all()

    // ---------------- M19b:应战答题 / 回执结算 / 战绩 ----------------

    /** 一场战书在本端的结算结果(win = 1 胜 / 0 负 / -1 平;myMs 供回执重组) */
    data class DuelOutcome(val win: Int, val coinDelta: Int, val myScore: Int, val oppScore: Int, val myMs: Long = 0L)

    /** 导入码的类型判定(战书码与回执码同为 RS1 前缀,内容区分) */
    sealed class DuelImport {
        data class Ch(val c: ChallengeCodec.Challenge) : DuelImport()
        data class Rc(val r: ChallengeCodec.Receipt) : DuelImport()
        object None : DuelImport()
    }

    /** 自动识别粘贴内容:战书码(未导入过)/ 回执码(可结算)/ 无法识别 */
    suspend fun parseAny(codeText: String): DuelImport {
        val ch = ChallengeCodec.decodeChallenge(codeText)
        if (ch != null) {
            return if (db.duelsDao().byCodeId(ch.id) == null) DuelImport.Ch(ch) else DuelImport.None
        }
        val rc = ChallengeCodec.decodeReceipt(codeText) ?: return DuelImport.None
        val d = db.duelsDao().byCodeId(rc.id) ?: return DuelImport.None
        return if (d.role == "challenger" && d.status == "sent") DuelImport.Rc(rc) else DuelImport.None
    }

    /** 应战方开始作答:校验待作答状态,从落库全量 JSON 重组 Challenge(含幽灵序列) */
    suspend fun startDefense(d: DuelEntity): ChallengeCodec.Challenge? {
        if (d.role != "defender" || d.status != "received") return null
        return ChallengeCodec.Challenge.fromFullJson(d.codeId, d.opponent, d.bet, d.quizJson)
    }

    /**
     * 应战方答完:回填我方成绩、立即本端结算(胜 +2×bet / 平退 bet / 负不退,零和)、
     * status → settled;返回结算结果与待回传的回执信息。
     * oppScore/oppMs 在 challenge 里(战书码自带),落库供列表展示。
     */
    suspend fun finishDefense(d: DuelEntity, score: Int, ms: Long, opponentScore: Int, opponentMs: Long): DuelOutcome? {
        if (d.role != "defender" || d.status != "received") return null
        val win = judgeWin(score, opponentScore, ms, opponentMs)
        val delta = when (win) {
            1 -> d.bet * 2
            -1 -> d.bet
            else -> 0
        }
        if (delta > 0) prefs.addCoins(delta)
        db.duelsDao().applyReceipt(d.id, d.opponent, opponentScore, opponentMs, "settled")
        db.duelsDao().updateMyResult(d.id, score, ms)
        db.duelsDao().updateStatus(d.id, "settled", System.currentTimeMillis())
        return DuelOutcome(win, delta, score, opponentScore, ms)
    }

    /** 下战书方导入回执:回填对方成绩、结算划转、status → settled */
    suspend fun settleWithReceipt(rc: ChallengeCodec.Receipt): DuelOutcome? {
        val d = db.duelsDao().byCodeId(rc.id) ?: return null
        if (d.role != "challenger" || d.status != "sent") return null
        val win = judgeWin(d.myScore, rc.score, d.myMs, rc.ms)
        val delta = when (win) {
            1 -> d.bet * 2
            -1 -> d.bet
            else -> 0
        }
        if (delta > 0) prefs.addCoins(delta)
        db.duelsDao().applyReceipt(d.id, rc.to, rc.score, rc.ms, "settled")
        db.duelsDao().updateStatus(d.id, "settled", System.currentTimeMillis())
        return DuelOutcome(win, delta, d.myScore, rc.score)
    }

    /** 胜负判定:先比分,平分比用时(短者胜),完全相同为平局(1 胜 / 0 负 / -1 平,UI 复用) */
    fun judgeWin(myScore: Int, oppScore: Int, myMs: Long, oppMs: Long): Int = when {
        myScore > oppScore -> 1
        myScore < oppScore -> 0
        myMs < oppMs -> 1
        myMs > oppMs -> 0
        else -> -1
    }

    /** 战书战绩:总场次/胜/平/负/当前连胜(按结算时间倒序连续胜) */
    data class DuelStats(val total: Int, val wins: Int, val draws: Int, val losses: Int, val streak: Int)

    suspend fun duelStats(): DuelStats {
        var wins = 0; var draws = 0; var losses = 0
        val settled = db.duelsDao().all()
            .filter { it.status == "settled" && it.myScore >= 0 && it.oppScore >= 0 }
            .sortedByDescending { it.settledAt ?: 0L }
        for (d in settled) {
            when (judgeWin(d.myScore, d.oppScore, d.myMs, d.oppMs)) {
                1 -> wins++; 0 -> losses++; else -> draws++
            }
        }
        var streak = 0
        for (d in settled) {
            val w = judgeWin(d.myScore, d.oppScore, d.myMs, d.oppMs)
            if (w == 1) streak++ else break
        }
        return DuelStats(settled.size, wins, draws, losses, streak)
    }
}

