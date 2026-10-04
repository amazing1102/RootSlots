package com.cet4.rootslots.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Database(
    entities = [WordEntity::class, MorphEntity::class, FamilyEntity::class, ComboEntity::class, FavoriteEntity::class, SpinEntity::class, SrsEntity::class],
    version = 4,
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

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): AppDatabase {
            val db = Room.databaseBuilder(context, AppDatabase::class.java, "rootslots.db")
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                prefill(context)
                                android.util.Log.i("Prefill", "done")
                            } catch (e: Exception) {
                                android.util.Log.e("Prefill", "FAILED", e)
                            }
                        }
                    }
                })
                .build()
            return db
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
                    )
                }
            )

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
