package com.cet4.rootslots.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * 按压缩放微交互:与 clickable 共用同一个 interactionSource,
 * 按下时整体缩到 pressedScale,松手弹簧回弹。用法:
 *   val ia = remember { MutableInteractionSource() }
 *   Modifier.pressScale(ia).clickable(interactionSource = ia, indication = LocalIndication.current) { … }
 */
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/** 记得一个现成 interactionSource 的便捷版(自带 pressScale + 无涟漪 clickable) */
@Composable
fun rememberPressInteraction(): MutableInteractionSource = remember { MutableInteractionSource() }
