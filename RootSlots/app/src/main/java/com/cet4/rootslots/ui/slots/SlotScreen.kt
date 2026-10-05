package com.cet4.rootslots.ui.slots

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cet4.rootslots.ui.theme.FontBrand
import com.cet4.rootslots.ui.theme.FontLed
import com.cet4.rootslots.ui.theme.LocalAppColors
import com.cet4.rootslots.ui.theme.pressScale
import kotlinx.coroutines.delay

@Composable
fun SlotScreen(
    onOpenDetail: (String) -> Unit = {},
    vm: SlotViewModel = viewModel(),
) {
    val c = LocalAppColors.current
    Box(
        Modifier
            .fillMaxSize()
            .background(c.bg)
    ) {
        if (!vm.ready) {
            LoadingPane()
        } else {
            GamePane(vm = vm, onOpenDetail = onOpenDetail)
        }
    }
}

@Composable
private fun LoadingPane() {
    val c = LocalAppColors.current
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Text("ROOT SLOTS", fontFamily = FontBrand, color = c.accent, fontSize = 24.sp)
        Spacer(Modifier.height(18.dp))
        CircularProgressIndicator(color = c.accent)
        Spacer(Modifier.height(16.dp))
        Text("词库装载中…", color = c.textMid)
    }
}

@Composable
private fun GamePane(
    vm: SlotViewModel,
    onOpenDetail: (String) -> Unit,
) {
    val c = LocalAppColors.current
    val view = LocalView.current
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(4.dp))
        // ---- HUD:招牌 + LED 计数(金币/能量) ----
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "ROOT SLOTS",
                fontFamily = FontBrand,
                fontSize = 16.sp,
                color = c.accent,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                LedChip("🪙", "%04d".format(vm.coins), c.accent)
                Spacer(Modifier.size(6.dp))
                LedChip("⚡", "%02d".format(vm.energy), c.prefix)
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---- 机柜:跑马灯泡带 + 动态段轴窗口 ----
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(c.cabinet)
                .border(1.dp, c.stroke, RoundedCornerShape(22.dp))
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BulbStrip(
                    chasing = vm.spinning,
                    celebrate = !vm.spinning && vm.current != null,
                )
                Spacer(Modifier.height(8.dp))

                // 动态段轴:轴数=词段数,左→右依次停轴,逐段点亮
                val n = vm.reels.size
                val specs = if (n == 0) {
                    listOf(
                        SlotViewModel.ReelSpec("P", "—", emptyList()),
                        SlotViewModel.ReelSpec("R", "—", emptyList()),
                        SlotViewModel.ReelSpec("S", "—", emptyList()),
                    )
                } else vm.reels
                Row(
                    Modifier
                        .fillMaxWidth()
                        .then(if (specs.size > 4) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
                    horizontalArrangement = if (specs.size > 4) Arrangement.Start else Arrangement.Center,
                ) {
                    val idleColors = listOf(c.prefix, c.root, c.suffix)
                    specs.forEachIndexed { i, spec ->
                        Reel(
                            target = spec.target,
                            fillers = spec.fillers,
                            color = if (n == 0) idleColors[i % 3] else c.typeColor(spec.type),
                            spinId = vm.spinId,
                            durationMs = 900L + i * 450L,          // 左→右依次停
                            lit = i in vm.litIndices,
                            reelWidth = when (specs.size) {
                                1 -> 140.dp; 2 -> 124.dp; 3 -> 108.dp; 4 -> 86.dp; else -> 76.dp
                            },
                            onStopped = { vm.onReelStopped(i) },
                        )
                        if (i < specs.size - 1) {
                            Spacer(Modifier.size(if (specs.size >= 4) 6.dp else 10.dp))
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                BulbStrip(
                    chasing = vm.spinning,
                    celebrate = !vm.spinning && vm.current != null,
                    phaseOffset = 9,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---- 结算区(可滚动):结果卡 + 出题 + 浮字;SPIN 恒在底部 ----
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
        // ---- 结果卡:随转轴入场,完整词逐段点亮,停稳后胜利脉冲 ----
        val winScale = remember { Animatable(1f) }
        LaunchedEffect(vm.spinId, vm.spinning) {
            if (vm.spinId > 0 && !vm.spinning) {
                winScale.snapTo(1f)
                winScale.animateTo(1.05f, tween(130, easing = FastOutSlowInEasing))
                winScale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
            }
        }
        AnimatedContent(
            targetState = vm.current?.w,
            transitionSpec = {
                (fadeIn(tween(280, delayMillis = 60)) +
                    slideInVertically(tween(280, delayMillis = 60)) { it / 6 })
                    .togetherWith(fadeOut(tween(110)) + slideOutVertically(tween(110)) { -it / 10 })
            },
            label = "resultCard",
        ) { w ->
            if (w == null) {
                Box(Modifier.fillMaxWidth().height(118.dp), contentAlignment = Alignment.Center) {
                    Text("转动老虎机,拼出真词", color = c.textFaint, fontSize = 14.sp)
                }
            } else {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(c.surface)
                        .border(1.dp, c.stroke, RoundedCornerShape(20.dp))
                        .clickable(enabled = !vm.spinning && !(vm.quiz != null && vm.quizPicked == null)) { onOpenDetail(w) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        buildAnnotatedString {
                            vm.segs.forEachIndexed { i, seg ->
                                val lit = i in vm.litIndices || !vm.spinning
                                withStyle(SpanStyle(fontWeight = FontWeight.Black, color = if (lit) c.typeColor(seg.t) else c.textFaint)) {
                                    append(seg.s)
                                }
                            }
                        },
                        fontSize = if (vm.segs.sumOf { it.s.length } > 10) 30.sp else 36.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.graphicsLayer {
                            scaleX = winScale.value
                            scaleY = winScale.value
                        },
                    )
                    vm.ipaOf(w)?.let { ipa ->
                        Spacer(Modifier.height(2.dp))
                        Text("/$ipa/", color = c.textDim, fontSize = 13.sp)
                    }
                    if (!vm.spinning) {
                        if (vm.lastRust) {
                            Spacer(Modifier.height(4.dp))
                            Text("⚠ 生词本已逾期,先去复习", color = c.danger, fontSize = 11.sp)
                        }
                        val fam = vm.current?.family
                        if (fam != null) {
                            Spacer(Modifier.height(6.dp))
                            val fm = vm.familyMeaning(fam).takeIf { it.isNotBlank() && it != "?" }
                            Text(
                                if (fm != null) "词根族「$fam $fm」 · ${vm.familyWords(fam)} 词"
                                else "词根族「$fam」 · ${vm.familyWords(fam)} 词",
                                color = c.textDim, fontSize = 11.sp,
                            )
                        }
                        // 落定开考:出题中隐藏释义,答完显示并对色
                        val quiz = vm.quiz
                        if (quiz != null) {
                            Spacer(Modifier.height(8.dp))
                            val resolved = vm.quizPicked != null
                            quiz.options.forEach { opt ->
                                val isAnswer = opt == quiz.answer
                                val isPickedWrong = opt == vm.quizPicked && !isAnswer
                                Text(
                                    opt,
                                    color = when {
                                        resolved && isAnswer -> c.success
                                        isPickedWrong -> c.danger
                                        else -> c.text
                                    },
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            when {
                                                resolved && isAnswer -> c.successBg
                                                isPickedWrong -> c.dangerBg
                                                else -> c.surfaceAlt
                                            }
                                        )
                                        .clickable(enabled = !resolved) { vm.answerQuiz(opt) }
                                        .padding(vertical = 9.dp),
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                            if (resolved) {
                                // 答完:释义 + 结算提示随选项着色一并浮现
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    vm.gloss ?: "",
                                    color = c.textMid, fontSize = 15.sp, textAlign = TextAlign.Center,
                                )
                                vm.quizResult?.let {
                                    Spacer(Modifier.height(2.dp))
                                    Text(it, color = c.textDim, fontSize = 11.sp)
                                }
                            } else {
                                Text(
                                    "跳过 · 拿半价奖励",
                                    color = c.textFaint, fontSize = 11.sp,
                                    modifier = Modifier.clickable { vm.skipQuiz() },
                                )
                            }
                        } else {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                vm.gloss ?: "",
                                color = c.textMid, fontSize = 15.sp, textAlign = TextAlign.Center,
                            )
                            vm.quizResult?.let {
                                Spacer(Modifier.height(2.dp))
                                Text(it, color = c.textDim, fontSize = 11.sp)
                            }
                            Spacer(Modifier.height(2.dp))
                            Text("点击单词查看构词详情 →", color = c.textFaint, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // 结算浮字
        AnimatedVisibility(
            visible = vm.lastReward > 0 && !vm.spinning,
            enter = slideInVertically { it / 2 } + fadeIn(),
            exit = slideOutVertically { it / 2 } + fadeOut(),
        ) {
            Text(
                "+${vm.lastReward} 金币",
                color = c.accent,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (vm.familyStreak >= 2 && !vm.spinning) {
            Text("🎰 词根家族连击 ×${vm.familyStreak}", color = c.suffix, fontSize = 13.sp)
        }
        }

        // ---- SPIN 区:点击后褪去让位出题,答完停一拍再弹回(空间随动画收放) ----
        val quizUnanswered = vm.quiz != null && vm.quizPicked == null
        var spinShown by remember { mutableStateOf(true) }
        LaunchedEffect(vm.spinning, quizUnanswered, vm.quizPicked, vm.spinId) {
            when {
                vm.spinning || quizUnanswered -> spinShown = false
                vm.quiz != null -> { delay(1600); spinShown = true }   // 留时间看对错与释义
                else -> spinShown = true
            }
        }
        AnimatedVisibility(
            visible = spinShown,
            enter = fadeIn(tween(420)) +
                scaleIn(
                    initialScale = 0.55f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
                ),
            exit = fadeOut(tween(300)) + scaleOut(targetScale = 0.55f, animationSpec = tween(300)),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val enabled = !vm.spinning && vm.energy >= 1
                val spinIa = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .pressScale(spinIa, pressedScale = 0.9f)
                        .shadow(8.dp, CircleShape, clip = false)
                        .clip(CircleShape)
                        .background(if (enabled) c.accent else c.disabledBg)
                        .border(4.dp, c.cabinet, CircleShape)
                        .clickable(interactionSource = spinIa, indication = null, enabled = enabled) {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            vm.spin()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SPIN", fontFamily = FontBrand, fontSize = 26.sp,
                            color = if (enabled) c.onAccent else c.disabledText)
                        Text(if (vm.spinning) "转动中…" else "转动", fontSize = 12.sp,
                            color = if (enabled) c.onAccent.copy(alpha = 0.7f) else c.disabledText)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (vm.energy >= 1) "能量每 10 秒回 1 点" else "能量恢复中…",
                    color = c.textDim, fontSize = 11.sp,
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        // 每根轴落位:轻微滴答触感
        LaunchedEffect(vm.litIndices.size, vm.spinId) {
            if (vm.spinId > 0 && vm.litIndices.isNotEmpty()) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        }
    }
}

/** LED 计数胶囊:机壳底 + 七段管数字(实体老虎机的 CREDIT 显示窗) */
@Composable
private fun LedChip(emoji: String, value: String, tint: Color) {
    val c = LocalAppColors.current
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(c.cabinet)
            .border(1.dp, c.stroke, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 12.sp)
        Spacer(Modifier.width(5.dp))
        Text(value, fontFamily = FontLed, color = tint, fontSize = 13.sp)
    }
}

/**
 * 跑马灯泡带:机器的招牌灯。
 * 转动时灯点向右追逐;出词后全体呼吸庆祝;待机时缓慢微光。
 */
@Composable
private fun BulbStrip(chasing: Boolean, celebrate: Boolean, phaseOffset: Int = 0) {
    val c = LocalAppColors.current
    val infinite = rememberInfiniteTransition(label = "marquee")
    val phase by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "phase",
    )
    val pulse by infinite.animateFloat(
        0.55f, 1f,
        infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    Canvas(Modifier.fillMaxWidth().height(14.dp)) {
        val n = 18
        val r = 2.4.dp.toPx()
        val cy = size.height / 2
        for (i in 0 until n) {
            val cx = (i + 0.5f) * size.width / n
            val bright: Float = when {
                chasing -> {
                    val u = (((i + phaseOffset) % n) / n.toFloat() + phase) % 1f
                    val d = minOf(u, 1f - u)
                    (1f - d / 0.12f).coerceIn(0f, 1f)
                }
                celebrate -> 0.55f + 0.45f * pulse
                else -> 0.25f + 0.22f * pulse
            }
            val core = lerp(c.bulbDim, c.accent, bright)
            if (bright > 0.4f) {
                drawCircle(c.accent.copy(alpha = 0.28f * bright), r * 2.6f, Offset(cx, cy))
            }
            drawCircle(core, r, Offset(cx, cy))
        }
    }
}
