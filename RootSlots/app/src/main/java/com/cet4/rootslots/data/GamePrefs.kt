package com.cet4.rootslots.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val Context.gameStore by preferencesDataStore(name = "game")

/** 一天的学习记录:转动次数与当日的目标值(用于日历热力图与连续达标天数) */
data class DailyStat(val day: LocalDate, val count: Int, val goal: Int)

/** 金币 / 能量 / 累计转数的持久化(能量按时间回充)+ 外观与发音设置 */
class GamePrefs(private val context: Context) {
    companion object {
        val COINS = intPreferencesKey("coins")
        val ENERGY = intPreferencesKey("energy")
        val LAST_REGEN = longPreferencesKey("last_regen")
        val TOTAL_SPINS = intPreferencesKey("total_spins")
        val THEME = intPreferencesKey("theme")              // 0 跟随系统 / 1 深色 / 2 浅色
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val WRONG_NOTIFY = booleanPreferencesKey("wrong_notify") // 答错弹窗提醒(关=静默入生词本)
        val DAILY_GOAL = intPreferencesKey("daily_goal")     // 每日学习目标(转动次数)
        val SPINS_TODAY = intPreferencesKey("spins_today")   // 今日已转(按 DAY_STAMP 归零)
        val DAY_STAMP = longPreferencesKey("day_stamp")      // SPINS_TODAY 所属日(LocalDate epochDay)
        val DAILY_STATS = stringSetPreferencesKey("daily_stats") // 历史日统计 "yyyymmdd:count:goal"
        const val ENERGY_MAX = 30
        const val REGEN_MS = 10_000L
        const val COINS_START = 200
        const val SPEECH_RATE_DEFAULT = 0.85f
        const val WRONG_NOTIFY_DEFAULT = true
        const val DAILY_GOAL_DEFAULT = 50
    }

    data class Wallet(val coins: Int, val energy: Int, val totalSpins: Int, val spinsToday: Int, val dailyGoal: Int)

    val wallet: Flow<Wallet> = context.gameStore.data.map { p ->
        val last = p[LAST_REGEN] ?: 0L
        val stored = p[ENERGY] ?: ENERGY_MAX
        val regen = if (last == 0L) 0 else ((System.currentTimeMillis() - last) / REGEN_MS).toInt()
        val today = java.time.LocalDate.now().toEpochDay()
        Wallet(
            coins = p[COINS] ?: COINS_START,
            energy = minOf(ENERGY_MAX, stored + regen),
            totalSpins = p[TOTAL_SPINS] ?: 0,
            spinsToday = if (p[DAY_STAMP] == today) p[SPINS_TODAY] ?: 0 else 0,
            dailyGoal = p[DAILY_GOAL] ?: DAILY_GOAL_DEFAULT,
        )
    }

    /** 每 REGEN_MS 调一次:能量 +1 */
    suspend fun onRegenTick() {
        context.gameStore.edit { p ->
            val stored = p[ENERGY] ?: ENERGY_MAX
            p[ENERGY] = minOf(ENERGY_MAX, stored + 1)
            p[LAST_REGEN] = System.currentTimeMillis()
        }
    }

    /** 尝试消耗 1 能量;能量不足返回 false */
    suspend fun tryConsumeEnergy(): Boolean {
        var ok = false
        context.gameStore.edit { p ->
            val stored = p[ENERGY] ?: ENERGY_MAX
            if (stored >= 1) {
                p[ENERGY] = stored - 1
                ok = true
            }
            p[LAST_REGEN] = System.currentTimeMillis()
        }
        return ok
    }

    suspend fun addCoins(n: Int) {
        context.gameStore.edit { it[COINS] = (it[COINS] ?: COINS_START) + n }
    }

    suspend fun incSpins() {
        context.gameStore.edit { p ->
            val today = java.time.LocalDate.now().toEpochDay()
            val stamp = p[DAY_STAMP]
            p[SPINS_TODAY] = if (stamp == today) (p[SPINS_TODAY] ?: 0) + 1 else 1
            p[DAY_STAMP] = today
            p[TOTAL_SPINS] = (p[TOTAL_SPINS] ?: 0) + 1
        }
    }

    suspend fun setDailyGoal(n: Int) {
        context.gameStore.edit { it[DAILY_GOAL] = n.coerceIn(1, 999) }
    }

    /** 历史日统计(供学习日历渲染),key = LocalDate */
    val dailyStats: Flow<Map<LocalDate, DailyStat>> = context.gameStore.data.map { p ->
        (p[DAILY_STATS] ?: emptySet()).mapNotNull { e ->
            runCatching {
                val s = e.split(":")
                DailyStat(LocalDate.parse(s[0], DateTimeFormatter.BASIC_ISO_DATE), s[1].toInt(), s[2].toInt())
            }.getOrNull()
        }.associateBy { it.day }
    }

    /** 每转一次,当日历史 +1(DataStore 集合读改写) */
    suspend fun recordDailySpin() {
        context.gameStore.edit { p ->
            val today = LocalDate.now()
            val key = today.format(DateTimeFormatter.BASIC_ISO_DATE)
            val set = p[DAILY_STATS] ?: emptySet()
            val cur = set.firstOrNull { it.startsWith("$key:") }
            val count = (cur?.split(":")?.getOrNull(1)?.toIntOrNull() ?: 0) + 1
            val goal = p[DAILY_GOAL] ?: DAILY_GOAL_DEFAULT
            p[DAILY_STATS] = set.filterNot { it.startsWith("$key:") }.toSet() + "$key:$count:$goal"
        }
    }

    val theme: Flow<Int> = context.gameStore.data.map { it[THEME] ?: 0 }
    val speechRate: Flow<Float> = context.gameStore.data.map { it[SPEECH_RATE] ?: SPEECH_RATE_DEFAULT }
    val wrongNotify: Flow<Boolean> = context.gameStore.data.map { it[WRONG_NOTIFY] ?: WRONG_NOTIFY_DEFAULT }

    suspend fun setTheme(mode: Int) {
        context.gameStore.edit { it[THEME] = mode.coerceIn(0, 2) }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.gameStore.edit { it[SPEECH_RATE] = rate.coerceIn(0.5f, 1.2f) }
    }

    suspend fun setWrongNotify(notify: Boolean) {
        context.gameStore.edit { it[WRONG_NOTIFY] = notify }
    }
}
