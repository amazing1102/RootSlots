package com.cet4.rootslots.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
        const val ASSETS_VER = "20261005d"

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
}

