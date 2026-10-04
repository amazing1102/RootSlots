package com.cet4.rootslots.ui.slots

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cet4.rootslots.ui.theme.LocalAppColors

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
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(4.dp))
        // ---- HUD(导航交由底部 Tab,只留品牌+资源条) ----
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("ROOT SLOTS", fontSize = 16.sp, fontWeight = FontWeight.Black, color = c.accent)
            Row(verticalAlignment = Alignment.CenterVertically) {
                HudChip("🪙 ${vm.coins}", tint = c.accent)
                Spacer(Modifier.size(6.dp))
                HudChip("⚡ ${vm.energy}", tint = c.prefix)
            }
        }

        Spacer(Modifier.height(18.dp))

        // ---- 动态段轴:轴数=词段数,左→右依次停轴,逐段点亮 ----
        val n = vm.reels.size
        val specs = if (n == 0) {
            // 待机:三根空轴示意(机器静止态,不对应任何词)
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

        Spacer(Modifier.height(14.dp))

        // ---- 结果卡:完整单词(分段着色,随停轴逐段点亮)+ 词族 + 释义 ----
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(if (vm.current != null) c.surface else Color.Transparent)
                .clickable(enabled = !vm.spinning && vm.current != null) {
                    vm.current?.let { onOpenDetail(it.w) }
                }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (vm.current == null) {
                Text("转动老虎机,拼出真词", color = c.textFaint, fontSize = 14.sp)
            } else {
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
                )
                if (!vm.spinning) {
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
                    Spacer(Modifier.height(4.dp))
                    Text(
                        vm.gloss ?: "",
                        color = c.textMid, fontSize = 15.sp, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text("点击单词查看构词详情 →", color = c.textFaint, fontSize = 10.sp)
                }
            }
        }

        // 结算浮字
        AnimatedVisibility(
            visible = vm.lastReward > 0 && !vm.spinning,
            enter = slideInVertically { it / 2 } + fadeIn(),
            exit = fadeOut(),
        ) {
            Text(
                "+${vm.lastReward} 金币",
                color = c.accent,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
            )
        }
        if (vm.familyStreak >= 2 && !vm.spinning) {
            Text("🎰 词根家族连击 ×${vm.familyStreak}", color = c.suffix, fontSize = 13.sp)
        }

        Spacer(Modifier.weight(1f))

        // ---- SPIN 按钮 ----
        val enabled = !vm.spinning && vm.energy >= 1
        Box(
            modifier = Modifier
                .size(108.dp)
                .clip(CircleShape)
                .background(if (enabled) c.accent else c.disabledBg)
                .clickable(enabled = enabled) { vm.spin() },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SPIN", fontSize = 30.sp, fontWeight = FontWeight.Black,
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
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun HudChip(text: String, tint: androidx.compose.ui.graphics.Color? = null, onClick: () -> Unit = {}) {
    val c = LocalAppColors.current
    Text(
        text,
        color = tint ?: c.textMid,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(c.chip)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}
