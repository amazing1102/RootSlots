package com.cet4.rootslots.data

import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.withLock

/**
 * 内存词库:一次加载全部组合词/词表/词族/词法成分,供老虎机随机抽取。
 * 首次启动时等待 Room 的 assets 预填协程写完。
 */
class Repository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val db = AppDatabase.get(appContext)
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

        /** 档位名:0=新学,1..9=节点档,10=已毕业 */
        fun stageLabel(stage: Int): String = when {
            stage <= 0 -> "新学"
            stage >= STAGE_GRADUATED -> "已毕业"
            else -> "${NODE_LABELS[stage - 1]}档"
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

    suspend fun awaitReady() {
        if (ready) return
        ensurePrefilled()                            // 空库(首装/迁移/上次失败)就地补跑预填
        var tries = 0
        while (tries < 200 && db.combosDao().count() == 0) {
            delay(150); tries++
        }
        load()
        if (combos.isEmpty()) {
            android.util.Log.e("Repo", "awaitReady: combos still empty after ${tries} polls")
        }
        ready = true
    }

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

    /** 随机抽组合词,避免与上一词重复;词库未就绪返回 null */
    fun randomCombo(exclude: String? = null): ComboEntity? {
        if (combos.isEmpty()) return null
        repeat(8) {
            val c = combos[rng.nextInt(combos.size)]
            if (c.w != exclude) return c
        }
        return combos[rng.nextInt(combos.size)]
    }

    fun word(w: String): WordEntity? = words[w]
    fun glossedWords(): List<WordEntity> = words.values.filter { !it.g.isNullOrBlank() }
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

    /** 生词本条目:收藏 + 释义 + SRS 档案 */
    data class FavRow(val w: String, val g: String?, val srs: SrsEntity?)

    suspend fun favorites(): List<FavRow> {
        val now = System.currentTimeMillis()
        return db.favoritesDao().all().map { f ->
            FavRow(f.w, words[f.w]?.g, db.srsDao().byWord(f.w))
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
        return e
    }

    suspend fun srsOf(w: String): SrsEntity? = db.srsDao().byWord(w)
    suspend fun spinTotal(): Int = db.spinsDao().count()
    suspend fun spunDistinct(): Int = db.spinsDao().distinctWords()
    suspend fun spunWordSet(): Set<String> = db.spinsDao().distinctWordList().toSet()
    fun familyWordList(key: String): List<String> =
        db // 占位使单例结构清晰;实际取 families 内存
            .let { families[key]?.wordList?.split(",")?.filter { s -> s.isNotBlank() } ?: emptyList() }

}

