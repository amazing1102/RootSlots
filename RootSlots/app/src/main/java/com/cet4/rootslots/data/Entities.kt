package com.cet4.rootslots.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 词表(6286 词 + 释义 + 美式 IPA + 多义项释义 + 例句 + 考试标签) */
@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey val w: String,
    val g: String? = null,
    val ipa: String? = null,
    @ColumnInfo(name = "exam_tags") val examTags: String? = null,     // 考试标签,包裹式 ",cet4,gaokao,"
    @ColumnInfo(name = "detail_gloss") val detailGloss: String? = null, // 多义项释义,义项以 \n 分行
    @ColumnInfo(name = "sen_en") val senEn: String? = null,           // 主例句(英)
    @ColumnInfo(name = "sen_zh") val senZh: String? = null,           // 主例句(中)
)

/** 词法成分:type = P 前缀 / R 词根 / S 后缀;surfaceList 以逗号分隔 */
@Entity(tableName = "morphs")
data class MorphEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val key: String,
    val surfaceList: String,
    val meaning: String,
    val examples: String = "",
)

/** 词根家族(图鉴) */
@Entity(tableName = "families")
data class FamilyEntity(
    @PrimaryKey val key: String,
    val meaning: String,
    val count: Int,
    val wordList: String,
)

/** 一条可拼出的词及其分段(段面拼接恒等于 w) */
@Entity(tableName = "combos", indices = [Index("w"), Index("family")])
data class ComboEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val w: String,
    val family: String,
    val pattern: String,
    val segsJson: String, // [{"t":"P","s":"under","k":"under"},...]
)

/** 一段词法成分的运行时展示模型 */
data class Seg(val t: String, val s: String, val k: String)

/** 生词本收藏(M5 在此之上做 SRS 排期) */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val w: String,
    val addedAt: Long,
)

/** 每次转动一条(驱动图鉴进度与统计) */
@Entity(tableName = "spins", indices = [androidx.room.Index("w")])
data class SpinEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val w: String,
    val at: Long,
)

/** SRS 排期:stage 0=新学(立即首复习),1..5 对应间隔 [1,3,7,14,30] 天 */
@Entity(tableName = "srs")
data class SrsEntity(
    @PrimaryKey val w: String,
    val stage: Int = 0,
    val dueAt: Long = 0L,
    val addedAt: Long,
    val reviews: Int = 0,
    val lapses: Int = 0,
)

/** 每次复习作答记录(驱动「我的」页复习统计图表) */
@Entity(tableName = "review_logs", indices = [androidx.room.Index("at")])
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val w: String,
    val at: Long,
    val known: Boolean,
)

/** 测验题(三模式共用) */
data class Q(
    val kind: String,        // gloss / sound / spell
    val prompt: String,
    val aux: String,
    val options: List<String>,
    val answer: String,
    val word: String,
)
