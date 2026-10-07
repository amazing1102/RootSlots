package com.cet4.rootslots.ui.duel

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cet4.rootslots.data.Repository
import com.cet4.rootslots.ui.slots.SlotViewModel
import com.cet4.rootslots.ui.theme.LocalAppColors
import com.cet4.rootslots.ui.theme.pressScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 宿敌对决(M18a):三局 Boss 战,全围绕该词 —— 释义选词 → 例句挖空(或看词选释义)
 * → 拼写补全(再临加听音局)。三局全胜 = 降服(筹码翻倍返还 + 勋章 + SRS 升 3 档);
 * 任一局败 / 中途弃战 = 败(筹码没收,词回 5 分钟档)。
 */
@Composable
fun NemesisScreen(vm: SlotViewModel, nemesis: Repository.Nemesis, onBack: () -> Unit) {
    val c = LocalAppColors.current
    val scope = rememberCoroutineScope()
    var pane by remember { mutableStateOf("intro") }   // intro / battle / win / lose / broke
    var rounds by remember { mutableStateOf<List<Repository.NemesisRound>>(emptyList()) }
    var idx by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<String?>(null) }

    BackHandler {
        when (pane) {
            "intro" -> onBack()
            "battle" -> scope.launch {   // 弃战 = 败
                vm.loseNemesis(nemesis)
                onBack()
            }
            else -> onBack()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        when (pane) {
            "intro" -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(28.dp))
                Text("☠ 通缉令", color = c.danger, fontSize = 26.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(18.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.surface)
                        .border(1.5.dp, c.danger, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(nemesis.word, color = c.text, fontSize = 26.sp, fontWeight = FontWeight.Black)
                        if (nemesis.defeatedBadge) {
                            Spacer(Modifier.width(8.dp))
                            Text("🏅", fontSize = 18.sp)
                        }
                    }
                    nemesis.gloss?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, color = c.textDim, fontSize = 13.sp, maxLines = 2)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(vm.nemesisTaunt(nemesis.word, nemesis.lapses), color = c.danger, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        (if (nemesis.revisit) "⚔ 降服过的宿敌卷土重来 · 筹码×2 · 多一局听音\n" else "") +
                            "入场筹码 ${nemesis.entryFee}🪙 · 三局全胜 = 降服:翻倍返还 + SRS 直升 3 档\n任一局失败:筹码没收,词回 5 分钟档",
                        color = c.textDim, fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        scope.launch {
                            if (!vm.payNemesisFee(nemesis.entryFee)) {
                                pane = "broke"
                                return@launch
                            }
                            rounds = vm.buildNemesisRounds(nemesis)
                            idx = 0; picked = null
                            pane = if (rounds.isEmpty()) "lose" else "battle"
                        }
                    },
                    enabled = vm.coins >= nemesis.entryFee,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = c.danger,
                        contentColor = c.bg,
                        disabledContainerColor = c.chip,
                        disabledContentColor = c.textFaint,
                    ),
                ) {
                    Text(
                        if (vm.coins < nemesis.entryFee) "金币不足(需 ${nemesis.entryFee}🪙)" else "发起对决(−${nemesis.entryFee}🪙)",
                        fontWeight = FontWeight.Black, fontSize = 16.sp,
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("余额 ${vm.coins}🪙 · 弃战视为败", color = c.textFaint, fontSize = 11.sp)
                Spacer(Modifier.height(24.dp))
            }

            "battle" -> {
                val r = rounds.getOrNull(idx)
                if (r == null) {
                    LaunchedEffect(Unit) { pane = "lose" }
                } else {
                    NemesisRoundPane(vm, r, idx + 1, rounds.size, picked,
                        onPick = { p -> picked = p },
                        onNext = { hit ->
                            scope.launch {
                                delay(950)
                                if (hit && idx + 1 >= rounds.size) {
                                    vm.defeatNemesis(nemesis, nemesis.entryFee)
                                    pane = "win"
                                } else if (hit) {
                                    idx++; picked = null
                                } else {
                                    vm.loseNemesis(nemesis)
                                    pane = "lose"
                                }
                            }
                        },
                        onPronounce = { vm.pronounce(it) },
                    )
                }
            }

            "win" -> NemesisResultPane(
                title = "🏅 降服成功!",
                body = "「${nemesis.word}」被你拿下\n+${nemesis.entryFee * 2}🪙(翻倍返还)· SRS 直升 3 档 · 降服勋章已镌刻",
                accent = c.success,
                onBack = onBack,
            )
            "lose" -> NemesisResultPane(
                title = "💀 对决失败",
                body = "「${nemesis.word}」第 ${nemesis.lapses} 次逃脱\n筹码已没收 · 词已回 5 分钟档 —— 它需要,正好",
                accent = c.danger,
                onBack = onBack,
            )
            "broke" -> NemesisResultPane(
                title = "🪙 筹码不足",
                body = "入场需 ${nemesis.entryFee}🪙,先去老虎机攒筹码\n宿敌会一直等着你",
                accent = c.textMid,
                onBack = onBack,
            )
        }
    }
}

@Composable
private fun NemesisRoundPane(
    vm: SlotViewModel,
    r: Repository.NemesisRound,
    no: Int,
    total: Int,
    picked: String?,
    onPick: (String) -> Unit,
    onNext: (Boolean) -> Unit,
    onPronounce: (String) -> Unit,
) {
    val c = LocalAppColors.current
    LaunchedEffect(picked) {
        if (picked != null) onNext(picked == r.answer)
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("第 $no/$total 局", color = c.danger, fontSize = 15.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            Text(
                when (r.kind) {
                    "gloss" -> "释义选词"; "cloze" -> "例句挖空"; "glossRev" -> "看词选义"
                    "spell" -> "拼写补全"; else -> "听音选词"
                },
                color = c.textDim, fontSize = 12.sp,
            )
        }
        // 局数进度点
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
            repeat(total) { i ->
                Box(
                    Modifier
                        .width(18.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(when {
                            i < no - 1 -> c.success
                            i == no - 1 -> c.danger
                            else -> c.surfaceAlt
                        }),
                )
            }
        }

        Spacer(Modifier.height(30.dp))
        if (r.kind == "spell") {
            Text(r.prompt, color = c.prefix, fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
            Spacer(Modifier.height(8.dp))
            Text(r.aux, color = c.textMid, fontSize = 15.sp)
        } else if (r.kind == "cloze") {
            Text("补全例句", color = c.textFaint, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Text(r.prompt, color = c.text, fontSize = 19.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (r.aux.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(r.aux, color = c.textDim, fontSize = 12.sp)
            }
        } else if (r.kind == "glossRev") {
            Text(r.prompt, color = c.suffix, fontSize = 32.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text(r.aux, color = c.textFaint, fontSize = 12.sp)
        } else if (r.kind == "sound") {
            Text(r.prompt, color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.chip)
                    .clickable { onPronounce(r.answer) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) { Text("🔊 重播发音", color = c.prefix, fontSize = 14.sp) }
        } else {
            Text("选出对应单词", color = c.textFaint, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Text(r.prompt, color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            r.options.forEach { opt ->
                val isAnswer = opt == r.answer
                val isPicked = opt == picked
                val ia = remember(opt) { MutableInteractionSource() }
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
                    color = fg, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressScale(ia, pressedScale = 0.97f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bg)
                        .border(1.5.dp, if (picked != null && isAnswer) c.success else c.stroke, RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = ia,
                            indication = LocalIndication.current,
                            enabled = picked == null,
                        ) { onPick(opt) }
                        .padding(vertical = 13.dp),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun NemesisResultPane(title: String, body: String, accent: androidx.compose.ui.graphics.Color, onBack: () -> Unit) {
    val c = LocalAppColors.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Text(title, color = accent, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(14.dp))
        Text(body, color = c.textMid, fontSize = 15.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
        ) { Text("离开", fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 24.dp)) }
        Spacer(Modifier.weight(1f))
    }
}
