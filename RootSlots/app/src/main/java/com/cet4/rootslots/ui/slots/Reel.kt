package com.cet4.rootslots.ui.slots

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cet4.rootslots.ui.theme.LocalAppColors
import kotlin.math.roundToInt

private val ReelEasing = CubicBezierEasing(0.12f, 0.75f, 0.25f, 1f)

/**
 * 一根转轴:竖向滚动的成分面,停稳时目标面正好落在窗口中间。
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

    LaunchedEffect(spinId, target) {
        if (spinId > 0) {
            offset.snapTo(0f)
            offset.animateTo(finalOffset, tween(durationMs.toInt(), easing = ReelEasing))
            onStopped()
        } else {
            offset.snapTo(finalOffset)
        }
    }

    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .width(reelWidth)
            .height(cellHeight * 3)
            .clip(shape)
            .background(c.surface)
            .border(
                width = if (lit) 2.dp else 1.dp,
                color = if (lit) color else c.stroke,
                shape = shape,
            )
    ) {
        Column(
            modifier = Modifier
                .offset { IntOffset(0, -offset.value.roundToInt()) }
                .wrapContentHeight(align = Alignment.Top, unbounded = true)
        ) {
            items.forEachIndexed { i, s ->
                val isTarget = i == targetIndex
                Box(
                    modifier = Modifier
                        .height(cellHeight)

                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .background(
                            if (isTarget) color.copy(alpha = if (lit) 0.16f else 0.07f)
                            else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = s,
                        color = if (isTarget) color else c.fillerText,
                        fontSize = fontSizeFor(s).sp,
                        fontWeight = if (isTarget) FontWeight.Black else FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
        // 上下渐隐,增强滚筒质感
        Box(
            Modifier
                .fillMaxWidth()
                .height(cellHeight)
                .background(Brush.verticalGradient(listOf(c.bg, Color.Transparent)))
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(cellHeight)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, c.bg)))
        )
    }
}

private fun fontSizeFor(s: String): Int = when {
    s.length <= 3 -> 26
    s.length <= 6 -> 21
    s.length <= 9 -> 17
    else -> 14
}
