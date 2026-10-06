package com.cet4.rootslots.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * 一场异步战书(好友 PK,零和押注;双方各自本地结算,配对键 = code_id)。
 * quiz_json 存战书码 q 数组原文(题目自包含,不依赖双方词库版本)。
 * status: sent(已发出待回执) / received(已导入待作答) / finished(已答完待回执导入, M19b)
 *         / settled(已结算) / expired(超时或作废,押注已退回)
 */
@Entity(
    tableName = "duels",
    indices = [Index("code_id"), Index("status"), Index("created_at")],
)
data class DuelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,                                     // challenger / defender
    @ColumnInfo(name = "code_id") val codeId: String,
    val opponent: String,                                 // 下战书方发出时未知,空串占位(回执导入时回填)
    val bet: Int,
    @ColumnInfo(name = "quiz_json") val quizJson: String,
    @ColumnInfo(name = "my_score") val myScore: Int = -1, // -1 = 未打
    @ColumnInfo(name = "my_ms") val myMs: Long = 0L,
    @ColumnInfo(name = "opp_score") val oppScore: Int = -1,
    @ColumnInfo(name = "opp_ms") val oppMs: Long = 0L,
    val status: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "settled_at") val settledAt: Long? = null,
)

@Dao
interface DuelsDao {
    @Insert
    suspend fun insert(d: DuelEntity): Long

    @Query("SELECT * FROM duels ORDER BY created_at DESC")
    suspend fun all(): List<DuelEntity>

    @Query("SELECT * FROM duels WHERE code_id = :codeId LIMIT 1")
    suspend fun byCodeId(codeId: String): DuelEntity?

    @Query("SELECT * FROM duels WHERE status IN ('sent','received','finished')")
    suspend fun active(): List<DuelEntity>

    @Query("UPDATE duels SET status = :status, settled_at = :settledAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, settledAt: Long?)

    @Query("UPDATE duels SET opponent = :opponent, opp_score = :oppScore, opp_ms = :oppMs, status = :status WHERE id = :id")
    suspend fun applyReceipt(id: Long, opponent: String, oppScore: Int, oppMs: Long, status: String)
}
