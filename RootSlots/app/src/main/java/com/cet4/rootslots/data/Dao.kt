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
}
