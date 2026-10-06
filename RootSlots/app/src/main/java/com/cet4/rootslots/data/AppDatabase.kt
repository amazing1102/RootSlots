package com.cet4.rootslots.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject

@Database(
    entities = [WordEntity::class, MorphEntity::class, FamilyEntity::class, ComboEntity::class, FavoriteEntity::class, SpinEntity::class, SrsEntity::class, ReviewLogEntity::class, DuelEntity::class],
    version = 8,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordsDao(): WordsDao
    abstract fun morphsDao(): MorphsDao
    abstract fun familiesDao(): FamiliesDao
    abstract fun combosDao(): CombosDao
    abstract fun favoritesDao(): FavoritesDao
    abstract fun spinsDao(): SpinsDao
    abstract fun srsDao(): SrsDao
    abstract fun reviewLogDao(): ReviewLogDao
    abstract fun duelsDao(): DuelsDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): AppDatabase {
            val db = Room.databaseBuilder(context, AppDatabase::class.java, "rootslots.db")
                .addMigrations(MIGRATION_6_7, MIGRATION_7_8)
                .build()
            return db
            // 预填唯一入口是 Repository.ensurePrefilled()(带 Mutex、空库判定)。
            // 这里绝不能再挂 onCreate 预填:首装时两条路径并发各灌一次,
            // 自增主键表(morphs/combos)会整体翻倍——真实踩过的坑。
        }

        /** v6 → v7:words 表新增多义项释义/例句/考试标签(非破坏,首例真迁移,后续升版照此范式) */
        private val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE words ADD COLUMN exam_tags TEXT")
                db.execSQL("ALTER TABLE words ADD COLUMN detail_gloss TEXT")
                db.execSQL("ALTER TABLE words ADD COLUMN sen_en TEXT")
                db.execSQL("ALTER TABLE words ADD COLUMN sen_zh TEXT")
            }
        }

        /** v7 → v8:好友 PK 新增 duels 表(纯新增,不动旧表,收藏/SRS 无损) */
        private val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `duels` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `role` TEXT NOT NULL,
                        `code_id` TEXT NOT NULL,
                        `opponent` TEXT NOT NULL,
                        `bet` INTEGER NOT NULL,
                        `quiz_json` TEXT NOT NULL,
                        `my_score` INTEGER NOT NULL,
                        `my_ms` INTEGER NOT NULL,
                        `opp_score` INTEGER NOT NULL,
                        `opp_ms` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `settled_at` INTEGER)""",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_duels_code_id` ON `duels` (`code_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_duels_status` ON `duels` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_duels_created_at` ON `duels` (`created_at`)")
            }
        }

        /** 从 assets 的四个 JSON 预填全部表 */
        suspend fun prefill(context: Context) {
            val db = get(context)

            val words = JSONArray(readAsset(context, "words.json"))
            db.wordsDao().insertAll(
                List(words.length()) { i ->
                    val o = words.getJSONObject(i)
                    WordEntity(
                        w = o.getString("w"),
                        g = if (o.isNull("g")) null else o.getString("g"),
                        ipa = if (o.isNull("i")) null else o.optString("i").ifBlank { null },
                        examTags = if (o.isNull("x")) null else o.optString("x").let { ",$it," },
                        detailGloss = if (o.isNull("gd")) null else o.optString("gd").ifBlank { null },
                        senEn = if (o.isNull("se")) null else o.optString("se").ifBlank { null },
                        senZh = if (o.isNull("sz")) null else o.optString("sz").ifBlank { null },
                    )
                }
            )

            fillStructTables(context)
        }

        /** 结构表(morphs/families/combos)从 assets 填充;首装预填与升级刷新共用 */
        suspend fun fillStructTables(context: Context) {
            val db = get(context)
            val morphs = JSONObject(readAsset(context, "morphs.json"))
            val morphRows = mutableListOf<MorphEntity>()
            for (type in listOf("prefixes", "roots", "suffixes")) {
                val arr = morphs.getJSONArray(type)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    morphRows += MorphEntity(
                        type = when (type) {
                            "prefixes" -> "P"
                            "roots" -> "R"
                            else -> "S"
                        },
                        key = o.getString("key"),
                        surfaceList = jsonJoin(o.getJSONArray("surfaces")),
                        meaning = o.optString("meaning"),
                        examples = o.optString("examples"),
                    )
                }
            }
            db.morphsDao().insertAll(morphRows)

            val families = JSONArray(readAsset(context, "families.json"))
            db.familiesDao().insertAll(
                List(families.length()) { i ->
                    val o = families.getJSONObject(i)
                    FamilyEntity(
                        key = o.getString("key"),
                        meaning = o.getString("meaning"),
                        count = o.getInt("count"),
                        wordList = jsonJoin(o.getJSONArray("words")),
                    )
                }
            )

            val combos = JSONArray(readAsset(context, "combos.json"))
            db.combosDao().insertAll(
                List(combos.length()) { i ->
                    val o = combos.getJSONObject(i)
                    ComboEntity(
                        w = o.getString("w"),
                        family = o.getString("family"),
                        pattern = o.getString("pattern"),
                        segsJson = o.getJSONArray("segs").toString(),
                    )
                }
            )
        }

        private fun readAsset(context: Context, name: String): String =
            context.assets.open(name).bufferedReader().use { it.readText() }

        /** JSONArray -> 无引号逗号串(不能用 join(),它会给元素加引号) */
        private fun jsonJoin(arr: JSONArray): String =
            List(arr.length()) { arr.optString(it) }.joinToString(",")
    }
}

/** 解析 combo.segsJson 为段列表 */
fun parseSegs(segsJson: String): List<Seg> {
    val arr = JSONArray(segsJson)
    return List(arr.length()) { i ->
        val o = arr.getJSONObject(i)
        Seg(t = o.getString("t"), s = o.getString("s"), k = o.getString("k"))
    }
}
