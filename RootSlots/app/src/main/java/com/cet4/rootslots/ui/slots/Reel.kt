package com.cet4.rootslots.ui.slots

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cet4.rootslots.ui.theme.LocalAppColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val ReelEasing = CubicBezierEasing(0.12f, 0.75f, 0.25f, 1f)

/**
 * 一根转轴:实体感的纸面条(米白纸 + 墨字,明暗主题不变——滚筒永远是纸做的),
 * 类型由底纹与下方色条表达。动画三段:回拉蓄力 → 快速滚动 → 过冲回弹落位。
 * spinId == 0 为静态初始态;每次 +1 触发一次滚动动画。
 */
@Composable
fun Reel(
    target: String,
    fillers: List<String>,
    color: Color,
    spinId: Int,
    durationMs: Long,
    lit: Boolean,
    cellHeight: Dp = 62.dp,
    reelWidth: Dp = 108.dp,
    onStopped: () -> Unit,
) {
    val c = LocalAppColors.current
    val items = remember(target, spinId) {
        if (spinId == 0) listOf("", target, "")
        else List(12) { fillers.randomOrNull() ?: target } + target +
            (fillers.randomOrNull() ?: "")      // 尾部补一格,目标面才能停在窗口中间
    }
    val cellPx = with(LocalDensity.current) { cellHeight.toPx() }
    val finalOffset = (items.size - 3) * cellPx
    val targetIndex = if (spinId == 0) 1 else items.size - 2
    val offset = remember { Animatable(0f) }
    val streak = remember { Animatable(0f) }   // 1 = 高速(字迹减淡 + 拉伸)

    LaunchedEffect(spinId, target) {
        if (spinId > 0) {
            offset.snapTo(0f)
            streak.snapTo(1f)
            // 字迹减淡跟随速度:前 40% 保持,后 45% 随减速恢复
            launch {
                streak.animateTo(1f, tween((durationMs * 0.40).toInt(), easing = LinearEasing))
                streak.animateTo(0f, tween((durationMs * 0.45).toInt(), easing = EaseInOutCubic))
            }
            offset.animateTo(-0.22f * cellPx, tween(70, easing = LinearEasing))
            offset.animateTo(finalOffset + 0.5f * cellPx, tween((durationMs * 0.94).toInt(), easing = ReelEasing))
            offset.animateTo(finalOffset, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
            streak.snapTo(0f)
            onStopped()
        } else {
            offset.snapTo(finalOffset)
        }
    }

    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .width(reelWidth)
            .height(cellHeight * 3)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(c.reelPaperHi, c.reelPaperLo)))
            .border(
                width = if (lit) 2.dp else 1.dp,
                color = if (lit) color else c.reelInk.copy(alpha = 0.5f),
                shape = shape,
            )
    ) {
        Column(
            modifier = Modifier
                .offset { IntOffset(0, -offset.value.roundToInt()) }
                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                .graphicsLayer {
                    alpha = 1f - 0.45f * streak.value
                    scaleY = 1f + 0.22f * streak.value
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
        ) {
            items.forEachIndexed { i, s ->
                val isTarget = i == targetIndex
                Box(
                    modifier = Modifier
                        .height(cellHeight)
                        .fillMaxWidth()
                        .padding(horizontal = 5.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isTarget) color.copy(alpha = if (lit) 0.15f else 0.06f)
                            else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = s,
                        color = when {
                            isTarget && lit -> c.reelInk
                            isTarget -> c.reelInk.copy(alpha = 0.6f)
                            else -> c.reelInk.copy(alpha = 0.32f)
                        },
                        fontSize = fontSizeFor(s).sp,
                        fontWeight = if (isTarget) FontWeight.Black else FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                    if (isTarget && s.isNotEmpty()) {
                        // 类型色条:品牌色小横条标出该轴的词性
                        Box(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 5.dp)
                                .width(reelWidth * 0.46f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(color.copy(alpha = if (lit) 1f else 0.35f))
                        )
                    }
                }
            }
        }
        // 两侧纸面阴影(滚筒曲率)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to c.reelPaperLo.copy(alpha = 0.6f),
                        0.16f to Color.Transparent,
                        0.84f to Color.Transparent,
                        1f to c.reelPaperLo.copy(alpha = 0.6f),
                    )
                )
        )
        // 上下渐隐,滚筒没入机柜窗口
        Box(
            Modifier
                .fillMaxWidth()
                .height(cellHeight * 0.9f)
                .background(Brush.verticalGradient(listOf(c.cabinet, Color.Transparent)))
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(cellHeight * 0.9f)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, c.cabinet)))
        )
    }
}

private fun fontSizeFor(s: String): Int = when {
    s.length <= 3 -> 26
    s.length <= 6 -> 21
    s.length <= 9 -> 17
    else -> 14
}
