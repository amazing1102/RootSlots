package com.cet4.rootslots.ui.quiz

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cet4.rootslots.data.Q
import com.cet4.rootslots.ui.slots.SlotViewModel
import com.cet4.rootslots.ui.theme.LocalAppColors
import com.cet4.rootslots.ui.theme.pressScale
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val ROUND = 10
private const val REWARD_PER_CORRECT = 2

/** 测验 Tab:释义选词 / 听音选词 / 拼写补全,每轮 10 题,答对 +2 金币;顶部战书入口 */
@Composable
fun QuizScreen(vm: SlotViewModel, onOpenDuel: () -> Unit) {
    val c = LocalAppColors.current
    var mode by remember { mutableStateOf<String?>(null) }
    var qs by remember { mutableStateOf<List<Q>>(emptyList()) }
    var idx by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<String?>(null) }
    var correctCount by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    if (mode == null) {
        MenuPane(vm, onOpenDuel) { m ->
            qs = vm.buildQuiz(m, ROUND)
            mode = m; idx = 0; picked = null; correctCount = 0; finished = false
        }
        return
    }

    // 出题后自动发音(听音题)
    LaunchedEffect(idx) {
        if (!finished && idx < qs.size && qs[idx].kind == "sound") {
            delay(350)
            vm.pronounce(qs[idx].answer)
        }
    }
    LaunchedEffect(picked) {
        if (picked != null && !finished) {
            if (picked == qs[idx].answer) {
                correctCount++
                vm.addCoins(REWARD_PER_CORRECT)
            }
            delay(950)
            if (idx + 1 >= qs.size) finished = true else { idx++; picked = null }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("← 退出", color = c.textMid, fontSize = 15.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { mode = null }
                    .padding(horizontal = 6.dp, vertical = 4.dp))
            Spacer(Modifier.weight(1f))
            Text(modeTitle(mode!!), color = c.accent, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            Text("${(idx + 1).coerceAtMost(ROUND)}/$ROUND · 🪙${vm.coins}",
                color = c.textDim, fontSize = 13.sp)
        }

        if (finished) {
            Spacer(Modifier.weight(1f))
            Text("测验完成", color = c.accent, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            Text("答对 $correctCount/$ROUND", color = c.text, fontSize = 18.sp)
            Text("获得 +${correctCount * REWARD_PER_CORRECT} 金币", color = c.accent, fontSize = 15.sp)
            Spacer(Modifier.height(18.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .weight(1f, fill = false)
            ) {
                qs.forEach { q ->
                    val ipa = vm.ipaOf(q.word)
                    Text(
                        "${q.word}" + (ipa?.let { "  /$it/" } ?: "") + "  ${q.aux}",
                        color = c.textDim, fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { mode = null },
                colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
            ) { Text("再来一轮", fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 18.dp)) }
            Spacer(Modifier.weight(1f))
            return@Column
        }

        val q = qs[idx]
        Spacer(Modifier.height(26.dp))

        if (q.kind == "spell") {
            Text(q.prompt, color = c.prefix, fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            Text(q.aux, color = c.textMid, fontSize = 15.sp)
        } else {
            Text(
                q.prompt,
                color = c.text,
                fontSize = if (q.kind == "ipa") 26.sp else 22.sp,
                fontWeight = FontWeight.Bold,
            )
            if (q.kind == "sound") {
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.chip)
                        .clickable { vm.pronounce(q.answer) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "重播",
                        tint = c.prefix,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("重播", fontSize = 15.sp, color = c.prefix)
                }
            }
        }
        // 答题反馈:音标/听音题揭示单词与释义
        AnimatedVisibility(
            visible = picked != null && (q.kind == "ipa" || q.kind == "sound"),
            enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 3 },
        ) {
            val hit = picked == q.answer
            Text(
                "${q.answer}  ${q.aux}",
                color = if (hit) c.success else c.danger,
                fontSize = 15.sp, fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.weight(1f))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            q.options.forEach { opt ->
                val isAnswer = opt == q.answer
                val isPicked = opt == picked
                val ia = remember(opt) { MutableInteractionSource() }
                // 颜色渐变入场:对/错状态不再硬切
                val bg by animateColorAsState(
                    when {
                        picked == null -> c.surface
                        isAnswer -> c.successBg
                        isPicked -> c.dangerBg
                        else -> c.surface
                    },
                    tween(280), label = "optBg",
                )
                val fg by animateColorAsState(
                    when {
                        picked == null -> c.text
                        isAnswer -> c.success
                        isPicked -> c.danger
                        else -> c.textFaint
                    },
                    tween(280), label = "optFg",
                )
                val borderColor by animateColorAsState(
                    if (picked != null && isAnswer) c.success else Color.Transparent,
                    tween(280), label = "optBorder",
                )
                Text(
                    opt,
                    color = fg,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressScale(ia, pressedScale = 0.97f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bg)
                        .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = ia,
                            indication = LocalIndication.current,
                            enabled = picked == null,
                        ) { picked = opt }
                        .padding(vertical = 14.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MenuPane(vm: SlotViewModel, onOpenDuel: () -> Unit, onStart: (String) -> Unit) {
    val c = LocalAppColors.current
    val ia = remember { MutableInteractionSource() }
    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Text("词 汇 测 验", color = c.accent, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Text("每轮 10 题 · 答对 +2 金币", color = c.textDim, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))
        // 战书入口(M19a):离线好友 PK
        Row(
            Modifier
                .fillMaxWidth()
                .pressScale(ia, pressedScale = 0.97f)
                .clip(RoundedCornerShape(16.dp))
                .background(c.chip)
                .border(1.dp, c.accent, RoundedCornerShape(16.dp))
                .clickable(interactionSource = ia, indication = LocalIndication.current, onClick = onOpenDuel)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⚔", fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("好友 PK · 战书", color = c.accent, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(2.dp))
                Text("离线异步对战:发码 → 同题应战 → 结算押注", color = c.textDim, fontSize = 12.sp)
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = c.accent,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        MenuCard("释义选词", "看中文释义,选出对应单词", Icons.Filled.Translate) { onStart("gloss") }
        Spacer(Modifier.height(12.dp))
        MenuCard("看音标选词", "看美式音标,选出对应单词", Icons.Filled.Abc) { onStart("ipa") }
        Spacer(Modifier.height(12.dp))
        MenuCard("听音选词", "听发音选单词(需设备语音引擎)", Icons.AutoMirrored.Filled.VolumeUp) { onStart("sound") }
        Spacer(Modifier.height(12.dp))
        MenuCard("拼写补全", "补全单词缺失的字母", Icons.Filled.Spellcheck) { onStart("spell") }
        Spacer(Modifier.weight(1f))
    }
}

/** 测验入口:统一浅卡 + 图标 + 左对齐文字,不再四种高饱和色块 */
@Composable
private fun MenuCard(
    title: String,
    sub: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val c = LocalAppColors.current
    val ia = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .pressScale(ia, pressedScale = 0.97f)
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .border(1.dp, c.stroke, RoundedCornerShape(16.dp))
            .clickable(interactionSource = ia, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(c.chip),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = title, tint = c.accent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = c.text, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(2.dp))
            Text(sub, color = c.textDim, fontSize = 12.sp)
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = c.textFaint,
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun modeTitle(m: String) = when (m) {
    "gloss" -> "释义选词"; "ipa" -> "看音标选词"; "sound" -> "听音选词"; else -> "拼写补全"
}
