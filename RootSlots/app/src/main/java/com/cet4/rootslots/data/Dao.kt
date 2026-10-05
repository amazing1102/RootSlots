package com.cet4.rootslots.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WordsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<WordEntity>)

    @Query("SELECT COUNT(*) FROM words")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM words WHERE g IS NOT NULL")
    suspend fun countGlossed(): Int

    @Query("SELECT * FROM words")
    suspend fun all(): List<WordEntity>

    @Query("SELECT * FROM words WHERE w = :word")
    suspend fun byWord(word: String): WordEntity?

    @Query("SELECT * FROM words WHERE g IS NOT NULL ORDER BY RANDOM() LIMIT 1")
    suspend fun randomGlossed(): WordEntity?

    /** 升级补灌闸门:仍有未填充新字段的行 */
    @Query("SELECT COUNT(*) FROM words WHERE exam_tags IS NULL OR exam_tags = '' OR detail_gloss IS NULL OR sen_en IS NULL")
    suspend fun countUnenriched(): Int

    /** 只补空字段,不覆盖已有值;exam_tags 用包裹式 ",cet4," */
    @Query(
        """UPDATE words SET
           detail_gloss = COALESCE(detail_gloss, :gd),
           sen_en = COALESCE(sen_en, :en),
           sen_zh = COALESCE(sen_zh, :zh),
           exam_tags = CASE WHEN exam_tags IS NULL OR exam_tags = '' THEN :tags ELSE exam_tags END
           WHERE w = :w""",
    )
    suspend fun enrichRow(w: String, gd: String?, en: String?, zh: String?, tags: String)
}

@Dao
interface MorphsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(morphs: List<MorphEntity>)

    @Query("SELECT COUNT(*) FROM morphs")
    suspend fun count(): Int

    @Query("SELECT * FROM morphs")
    suspend fun all(): List<MorphEntity>

    @Query("SELECT * FROM morphs WHERE type = :type AND :surface IN (surfaceList) LIMIT 1")
    suspend fun bySurface(type: String, surface: String): MorphEntity?
}

@Dao
interface FamiliesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(families: List<FamilyEntity>)

    @Query("SELECT COUNT(*) FROM families")
    suspend fun count(): Int

    @Query("SELECT * FROM families")
    suspend fun all(): List<FamilyEntity>

    @Query("SELECT * FROM families ORDER BY count DESC")
    suspend fun allBySize(): List<FamilyEntity>
}

@Dao
interface CombosDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(combos: List<ComboEntity>)

    @Query("SELECT COUNT(*) FROM combos")
    suspend fun count(): Int

    @Query("SELECT * FROM combos")
    suspend fun all(): List<ComboEntity>

    @Query("SELECT * FROM combos WHERE w = :word LIMIT 1")
    suspend fun byWord(word: String): ComboEntity?

    @Query("SELECT * FROM combos ORDER BY RANDOM() LIMIT 1")
    suspend fun random(): ComboEntity?

    @Query("SELECT * FROM combos WHERE family = :family")
    suspend fun byFamily(family: String): List<ComboEntity>
}

@Dao
interface FavoritesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(f: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE w = :w")
    suspend fun delete(w: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE w = :w)")
    suspend fun isFavoriteNow(w: String): Boolean

    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun count(): Int

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    suspend fun all(): List<FavoriteEntity>
}

@Dao
interface SpinsDao {
    @Insert
    suspend fun insert(s: SpinEntity)

    @Query("SELECT COUNT(*) FROM spins")
    suspend fun count(): Int

    @Query("SELECT COUNT(DISTINCT w) FROM spins")
    suspend fun distinctWords(): Int

    @Query("SELECT DISTINCT w FROM spins")
    suspend fun distinctWordList(): List<String>
}

@Dao
interface ReviewLogDao {
    @Insert
    suspend fun insert(e: ReviewLogEntity)

    @Query("SELECT COUNT(*) FROM review_logs WHERE at >= :since AND at < :until")
    suspend fun countBetween(since: Long, until: Long): Int
}

@Dao
interface SrsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: SrsEntity)

    @Query("SELECT * FROM srs WHERE w = :w")
    suspend fun byWord(w: String): SrsEntity?

    @Query("SELECT * FROM srs ORDER BY dueAt ASC")
    suspend fun all(): List<SrsEntity>

    @Query("SELECT COUNT(*) FROM srs WHERE dueAt <= :now")
    suspend fun dueCount(now: Long): Int

    @Query("SELECT * FROM srs WHERE dueAt <= :now ORDER BY dueAt ASC")
    suspend fun due(now: Long): List<SrsEntity>

    @Query("SELECT COUNT(*) FROM srs")
    suspend fun count(): Int
}
