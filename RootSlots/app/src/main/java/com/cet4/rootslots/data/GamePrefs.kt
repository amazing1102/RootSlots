package com.cet4.rootslots.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.gameStore by preferencesDataStore(name = "game")

/** 金币 / 能量 / 累计转数的持久化(能量按时间回充)+ 外观与发音设置 */
class GamePrefs(private val context: Context) {
    companion object {
        val COINS = intPreferencesKey("coins")
        val ENERGY = intPreferencesKey("energy")
        val LAST_REGEN = longPreferencesKey("last_regen")
        val TOTAL_SPINS = intPreferencesKey("total_spins")
        val THEME = intPreferencesKey("theme")              // 0 跟随系统 / 1 深色 / 2 浅色
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        const val ENERGY_MAX = 30
        const val REGEN_MS = 10_000L
        const val COINS_START = 200
        const val SPEECH_RATE_DEFAULT = 0.85f
    }

    data class Wallet(val coins: Int, val energy: Int, val totalSpins: Int)

    val wallet: Flow<Wallet> = context.gameStore.data.map { p ->
        val last = p[LAST_REGEN] ?: 0L
        val stored = p[ENERGY] ?: ENERGY_MAX
        val regen = if (last == 0L) 0 else ((System.currentTimeMillis() - last) / REGEN_MS).toInt()
        Wallet(
            coins = p[COINS] ?: COINS_START,
            energy = minOf(ENERGY_MAX, stored + regen),
            totalSpins = p[TOTAL_SPINS] ?: 0,
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
        context.gameStore.edit { it[TOTAL_SPINS] = (it[TOTAL_SPINS] ?: 0) + 1 }
    }

    val theme: Flow<Int> = context.gameStore.data.map { it[THEME] ?: 0 }
    val speechRate: Flow<Float> = context.gameStore.data.map { it[SPEECH_RATE] ?: SPEECH_RATE_DEFAULT }

    suspend fun setTheme(mode: Int) {
        context.gameStore.edit { it[THEME] = mode.coerceIn(0, 2) }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.gameStore.edit { it[SPEECH_RATE] = rate.coerceIn(0.5f, 1.2f) }
    }
}
