package com.cet4.rootslots.ui.theme

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cet4.rootslots.R

/**
 * 语义色 token:界面代码禁止硬编码色值,一律从 LocalAppColors 取。
 * typeColor("P"/"R"/"S") = 前缀蓝 / 词根金 / 后缀紫,全 App 统一。
 * 机台硬件组:老虎机页把「手机当机柜」——cabinet 是机壳,reelPaper/Ink 是
 * 滚筒纸面(实体滚筒永远是米白纸条,明暗主题不变),bulbDim 是跑马灯熄灭态。
 */
data class AppColors(
    val bg: Color,
    val surface: Color,        // 卡片
    val surfaceAlt: Color,     // 卡片次级/输入面
    val text: Color,
    val textMid: Color,        // 正文次级(释义)
    val textDim: Color,        // 说明文字
    val textFaint: Color,      // 弱化/未点亮
    val accent: Color,         // 主按钮/金币/高亮
    val onAccent: Color,       // accent 上的文字
    val prefix: Color,
    val root: Color,
    val suffix: Color,
    val success: Color,
    val successBg: Color,
    val danger: Color,
    val dangerBg: Color,
    val chip: Color,           // 小胶囊底
    val stroke: Color,         // 卡片描边
    val fillerText: Color,     // 转轴 filler 字
    val disabledBg: Color,
    val disabledText: Color,
    val cabinet: Color,        // 机壳/机身(老虎机页外框、LED 底)
    val reelPaperHi: Color,    // 滚筒纸面亮部
    val reelPaperLo: Color,    // 滚筒纸面暗部(渐变/两侧阴影)
    val reelInk: Color,        // 纸面上的墨色字
    val bulbDim: Color,        // 跑马灯泡熄灭态
) {
    fun typeColor(t: String) = when (t) { "P" -> prefix; "R" -> root; else -> suffix }
    fun typeName(t: String) = when (t) { "P" -> "前缀"; "R" -> "词根"; else -> "后缀" }
}

/** 深色:赌场夜场——墨紫底(非纯黑)+ 暖金,金色不再发闷 */
val DarkColors = AppColors(
    bg = Color(0xFF131120),
    surface = Color(0xFF201C33),
    surfaceAlt = Color(0xFF2A2542),
    text = Color(0xFFF1EDE2),
    textMid = Color(0xFFC9C4DB),
    textDim = Color(0xFF9A94B8),
    textFaint = Color(0xFF6F6987),
    accent = Color(0xFFF5B942),
    onAccent = Color(0xFF221B04),
    prefix = Color(0xFF5FA8FF),
    root = Color(0xFFF5B942),
    suffix = Color(0xFFB989FF),
    success = Color(0xFF4CD487),
    successBg = Color(0xFF1B3326),
    danger = Color(0xFFFF6B6B),
    dangerBg = Color(0xFF39201F),
    chip = Color(0x14FFFFFF),
    stroke = Color(0x26FFFFFF),
    fillerText = Color(0x66FFFFFF),
    disabledBg = Color(0xFF2A2542),
    disabledText = Color(0xFF6F6987),
    cabinet = Color(0xFF0D0B16),
    reelPaperHi = Color(0xFFFBF5E6),
    reelPaperLo = Color(0xFFE3D7BB),
    reelInk = Color(0xFF2B2439),
    bulbDim = Color(0xFF413A5C),
)

/** 浅色:纸面暖光——米白底 + 深金/墨蓝/深紫,构词三色降饱和保可读 */
val LightColors = AppColors(
    bg = Color(0xFFF6F1E7),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFEFE8D9),
    text = Color(0xFF252133),
    textMid = Color(0xFF4A4560),
    textDim = Color(0xFF6E6880),
    textFaint = Color(0xFF9A94A8),
    accent = Color(0xFFA66E0A),
    onAccent = Color(0xFFFFF8EC),
    prefix = Color(0xFF2563C9),
    root = Color(0xFFA66E0A),
    suffix = Color(0xFF6D3FB8),
    success = Color(0xFF1E9E5A),
    successBg = Color(0xFFDFF3E6),
    danger = Color(0xFFC93A3A),
    dangerBg = Color(0xFFF9E2E0),
    chip = Color(0x14252133),
    stroke = Color(0x22252133),
    fillerText = Color(0x55252133),
    disabledBg = Color(0xFFE7E0D1),
    disabledText = Color(0xFF9A94A8),
    cabinet = Color(0xFFDCCCA9),
    reelPaperHi = Color(0xFFFFFDF4),
    reelPaperLo = Color(0xFFEAE0C6),
    reelInk = Color(0xFF2E2742),
    bulbDim = Color(0xFFC0AF89),
)

val LocalAppColors = staticCompositionLocalOf { DarkColors }

/** 品牌字体:Bungee 招牌体(logo/SPIN);DSEG7 七段管数字体(金币/能量计数)。OFL,见 README */
val FontBrand = FontFamily(Font(R.font.bungee_regular))
val FontLed = FontFamily(Font(R.font.dseg7_classic_bold))

/** themeMode:0 跟随系统 / 1 深色 / 2 浅色 */
@Composable
fun RootSlotsTheme(themeMode: Int, content: @Composable () -> Unit) {
    val dark = themeMode == 1 || (themeMode == 0 && isSystemInDarkTheme())
    val c = if (dark) DarkColors else LightColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.accent, onPrimary = c.onAccent,
            secondary = c.suffix, tertiary = c.prefix,
            background = c.bg, onBackground = c.text,
            surface = c.surface, onSurface = c.text,
            error = c.danger,
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = c.onAccent,
            secondary = c.suffix, tertiary = c.prefix,
            background = c.bg, onBackground = c.text,
            surface = c.surface, onSurface = c.text,
            error = c.danger,
        )
    }

    // 状态栏图标颜色随主题切换
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var ctx = view.context
            while (ctx is ContextWrapper && ctx !is Activity) ctx = ctx.baseContext
            if (ctx is Activity) {
                val window = ctx.window
                androidx.core.view.WindowCompat.getInsetsController(window, view)
                    .isAppearanceLightStatusBars = !dark
            }
        }
    }

    CompositionLocalProvider(LocalAppColors provides c) {
        MaterialTheme(
            colorScheme = scheme,
            typography = RootSlotsTypography,
            content = content,
        )
    }
}

val RootSlotsTypography = Typography(
    displayLarge = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Black),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
)
