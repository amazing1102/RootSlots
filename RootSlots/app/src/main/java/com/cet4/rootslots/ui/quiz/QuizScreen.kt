package com.cet4.rootslots.ui.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cet4.rootslots.data.Q
import com.cet4.rootslots.ui.slots.SlotViewModel
import com.cet4.rootslots.ui.theme.LocalAppColors
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val ROUND = 10
private const val REWARD_PER_CORRECT = 2

/** 测验 Tab:释义选词 / 听音选词 / 拼写补全,每轮 10 题,答对 +2 金币 */
@Composable
fun QuizScreen(vm: SlotViewModel) {
    val c = LocalAppColors.current
    var mode by remember { mutableStateOf<String?>(null) }
    var qs by remember { mutableStateOf<List<Q>>(emptyList()) }
    var idx by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<String?>(null) }
    var correctCount by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    if (mode == null) {
        MenuPane(vm) { m ->
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
                Text("🔊 重播", fontSize = 15.sp, color = c.prefix,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.chip)
                        .clickable { vm.pronounce(q.answer) }
                        .padding(horizontal = 14.dp, vertical = 8.dp))
            }
        }
        // 答题反馈:音标/听音题揭示单词与释义
        if (picked != null && (q.kind == "ipa" || q.kind == "sound")) {
            Spacer(Modifier.height(10.dp))
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
                val bg = when {
                    picked == null -> c.surface
                    isAnswer -> c.successBg
                    isPicked -> c.dangerBg
                    else -> c.surface
                }
                val fg = when {
                    picked == null -> c.text
                    isAnswer -> c.success
                    isPicked -> c.danger
                    else -> c.textFaint
                }
                Text(
                    opt,
                    color = fg,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(bg)
                        .clickable(enabled = picked == null) { picked = opt }
                        .padding(vertical = 14.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MenuPane(vm: SlotViewModel, onStart: (String) -> Unit) {
    val c = LocalAppColors.current
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
        Spacer(Modifier.height(26.dp))
        Button(
            onClick = { onStart("gloss") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                Text("释义选词", fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text("看中文释义,选出对应单词", fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { onStart("ipa") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = c.surfaceAlt, contentColor = c.text),
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                Text("看音标选词", fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text("看美式音标,选出对应单词", fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { onStart("sound") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = c.suffix, contentColor = c.bg),
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                Text("听音选词", fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text("听发音选单词(需设备语音引擎)", fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { onStart("spell") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = c.prefix, contentColor = c.bg),
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                Text("拼写补全", fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text("补全单词缺失的字母", fontSize = 12.sp)
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

private fun modeTitle(m: String) = when (m) {
    "gloss" -> "释义选词"; "ipa" -> "看音标选词"; "sound" -> "听音选词"; else -> "拼写补全"
}
