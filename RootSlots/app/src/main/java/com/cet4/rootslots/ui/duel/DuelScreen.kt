package com.cet4.rootslots.ui.duel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cet4.rootslots.data.ChallengeCodec
import com.cet4.rootslots.data.DuelEntity
import com.cet4.rootslots.data.Repository
import com.cet4.rootslots.data.WordEntity
import com.cet4.rootslots.ui.slots.SlotViewModel
import com.cet4.rootslots.ui.theme.LocalAppColors
import com.cet4.rootslots.ui.theme.pressScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** 战书推入页:列表 ↔ 下战书流(挑词→押注→基准战→生成码)↔ 打战书(导入) */
@Composable
fun DuelScreen(vm: SlotViewModel, onBack: () -> Unit) {
    val c = LocalAppColors.current
    val scope = rememberCoroutineScope()
    var view by remember { mutableStateOf<DuelView>(DuelView.List) }
    var duels by remember { mutableStateOf<List<DuelEntity>>(emptyList()) }
    var toast by remember { mutableStateOf<String?>(null) }

    // 跨视图流转数据:下战书流产物 / 导入解析结果
    var pendingQuestions by remember { mutableStateOf<List<ChallengeCodec.DuelQ>>(emptyList()) }
    var pendingChallenge by remember { mutableStateOf<ChallengeCodec.Challenge?>(null) }
    var imported by remember { mutableStateOf<ChallengeCodec.Challenge?>(null) }

    fun reload() {
        scope.launch {
            vm.expireStaleDuels()
            duels = vm.duels()
        }
    }
    LaunchedEffect(Unit) { reload() }
    LaunchedEffect(toast) {
        if (toast != null) { delay(2400); toast = null }
    }

    // 推入页返回:流内先逐层回列表,列表时关推入页
    BackHandler {
        when (view) {
            is DuelView.List -> onBack()
            else -> view = DuelView.List
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        when (view) {
            is DuelView.List -> ListPane(vm, duels,
                onCreate = { view = DuelView.Create },
                onImport = { imported = null; view = DuelView.Import },
                onToast = { toast = it },
                onChanged = { reload() },
            )
            is DuelView.Create -> CreatePane(vm,
                onStart = { qs, bet, nick ->
                    pendingQuestions = qs
                    pendingChallenge = ChallengeCodec.Challenge(
                        id = ChallengeCodec.newDuelId(), from = nick, bet = bet,
                        questions = qs, challengerScore = -1, challengerMs = 0,
                    )
                    view = DuelView.Battle
                },
                onBack = { view = DuelView.List },
            )
            is DuelView.Battle -> BattlePane(vm, pendingQuestions,
                onDone = { score, ms ->
                    val base = pendingChallenge ?: return@BattlePane
                    val finished = base.copy(challengerScore = score, challengerMs = ms)
                    scope.launch {
                        val e = vm.createDuel(finished)
                        pendingChallenge = finished
                        if (e == null) toast = "战书生成失败:金币不足或重复"
                        view = DuelView.Created
                        reload()
                    }
                },
                onQuit = { view = DuelView.List },
            )
            is DuelView.Created -> CreatedPane(vm, pendingChallenge,
                onDone = { view = DuelView.List },
            )
            is DuelView.Import -> ImportPane(vm, imported,
                onImported = { imported = it },
                onAccepted = {
                    toast = "应战成功,押注已托管"
                    view = DuelView.List
                    reload()
                },
                onBack = { view = DuelView.List },
            )
        }

        toast?.let { t ->
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(c.chip)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) { Text(t, color = c.text, fontSize = 13.sp) }
        }
    }
}

private sealed class DuelView {
    data object List : DuelView()
    data object Create : DuelView()
    data object Battle : DuelView()
    data object Created : DuelView()
    data object Import : DuelView()
}

// ---------------- 列表 ----------------

@Composable
private fun ListPane(
    vm: SlotViewModel,
    duels: List<DuelEntity>,
    onCreate: () -> Unit,
    onImport: () -> Unit,
    onToast: (String) -> Unit,
    onChanged: () -> Unit,
) {
    val c = LocalAppColors.current
    val scope = rememberCoroutineScope()
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⚔ 战书", color = c.accent, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            Text("离线好友 PK · 押注零和", color = c.textDim, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigAction("📨 发战书", "挑 7 词 · 押注 · 生成战书码", Modifier.weight(1f), onCreate)
            BigAction("📥 打战书", "粘贴好友的战书码应战", Modifier.weight(1f), onImport)
        }
        Spacer(Modifier.height(12.dp))
        if (duels.isEmpty()) {
            Spacer(Modifier.weight(1f))
            Text(
                "还没有战书。\n发一份战书挑战好友,或粘贴好友发来的战书码。",
                color = c.textFaint, fontSize = 14.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.weight(1f))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(duels, key = { it.id }) { d ->
                    DuelCard(vm, d, onToast, onChanged)
                }
                item { Spacer(Modifier.height(10.dp)) }
            }
        }
    }
}

@Composable
private fun BigAction(title: String, sub: String, modifier: Modifier, onClick: () -> Unit) {
    val c = LocalAppColors.current
    val ia = remember { MutableInteractionSource() }
    Column(
        modifier
            .pressScale(ia, pressedScale = 0.97f)
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .border(1.dp, c.stroke, RoundedCornerShape(16.dp))
            .clickable(interactionSource = ia, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(title, color = c.accent, fontSize = 17.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(3.dp))
        Text(sub, color = c.textDim, fontSize = 11.sp)
    }
}

@Composable
private fun DuelCard(vm: SlotViewModel, d: DuelEntity, onToast: (String) -> Unit, onChanged: () -> Unit) {
    val c = LocalAppColors.current
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val statusTxt = when (d.status) {
        "sent" -> "待应战"; "received" -> "待作答"; "finished" -> "待回执"
        "settled" -> "已结算"; else -> "已退回"
    }
    val statusColor = when (d.status) {
        "sent", "received" -> c.accent
        "finished" -> c.prefix
        "settled" -> c.success
        else -> c.textFaint
    }
    val myTxt = if (d.myScore >= 0) "${d.myScore}/7" else "—"
    val oppTxt = if (d.oppScore >= 0) "${d.oppScore}/7" else "—"
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (d.role == "challenger") "发" else "应",
                color = c.onAccent, fontSize = 12.sp, fontWeight = FontWeight.Black,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (d.role == "challenger") c.root else c.prefix)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (d.role == "challenger" && d.opponent.isBlank()) "等待对手应战" else "VS ${d.opponent}",
                color = c.text, fontSize = 15.sp, fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Text(statusTxt, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("押注 ${d.bet}🪙", color = c.accent, fontSize = 12.sp)
            Spacer(Modifier.width(10.dp))
            Text("我 $myTxt · 对方 $oppTxt", color = c.textDim, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            if (d.status == "sent") {
                CardAction("复制码") { resendDuelCode(context, vm, d) ; onToast("战书码已复制") }
                Spacer(Modifier.width(8.dp))
            }
            if (d.status == "sent" || d.status == "received") {
                CardAction("作废") {
                    scope.launch {
                        if (vm.voidDuel(d)) onToast("已作废,押注 ${d.bet}🪙 退回")
                        onChanged()
                    }
                }
            }
        }
    }
}

@Composable
private fun CardAction(label: String, onClick: () -> Unit) {
    val c = LocalAppColors.current
    val ia = remember { MutableInteractionSource() }
    Text(
        label,
        color = c.prefix, fontSize = 12.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(interactionSource = ia, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}

/** 已发出战书重发码:从落库 quiz_json 重组 Challenge 再编码(署名用当前昵称) */
private fun resendDuelCode(context: Context, vm: SlotViewModel, d: DuelEntity) {
    val ch = ChallengeCodec.Challenge.fromQuestionsJson(
        d.codeId, vm.nickname.value, d.bet, d.myScore, d.myMs, d.quizJson,
    )
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("RootSlots 战书", ChallengeCodec.encodeChallenge(ch)))
}

// ---------------- 下战书:挑词 + 押注 ----------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreatePane(
    vm: SlotViewModel,
    onStart: (List<ChallengeCodec.DuelQ>, Int, String) -> Unit,
    onBack: () -> Unit,
) {
    val c = LocalAppColors.current
    val scope = rememberCoroutineScope()
    var source by remember { mutableStateOf(Repository.DuelSource.WEAK) }
    var candidates by remember { mutableStateOf<List<WordEntity>>(emptyList()) }
    var familyQuery by remember { mutableStateOf("") }
    var pickedFamily by remember { mutableStateOf<String?>(null) }
    var bet by remember { mutableStateOf(30) }
    var nick by remember { mutableStateOf(vm.nickname.value) }

    val coins = vm.coins
    val maxBet = (coins.coerceAtLeast(10)).coerceAtMost(500)
    val picked = candidates.take(Repository.DUEL_QUESTIONS)
    val canStart = picked.size >= Repository.DUEL_QUESTIONS && bet <= coins && bet >= 10

    LaunchedEffect(Unit) { nick = vm.nickname.first() }
    LaunchedEffect(source, pickedFamily) {
        candidates = vm.duelCandidates(source, pickedFamily)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("← 返回", color = c.textMid, fontSize = 15.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onBack() }
                    .padding(horizontal = 6.dp, vertical = 4.dp))
            Spacer(Modifier.weight(1f))
            Text("发战书 · 第 1 步 挑词", color = c.accent, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(40.dp))
        }
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SourceChip("生词本·薄弱", source == Repository.DuelSource.WEAK) { source = Repository.DuelSource.WEAK }
            SourceChip("指定词根族", source == Repository.DuelSource.FAMILY) { source = Repository.DuelSource.FAMILY }
            SourceChip("随机", source == Repository.DuelSource.RANDOM) { source = Repository.DuelSource.RANDOM }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            when (source) {
                Repository.DuelSource.WEAK -> "把我生词本里最薄弱的 7 个词发给你——真的难。"
                Repository.DuelSource.FAMILY -> "词根主题战:选一个词根族,族内随机抽 7 词。"
                Repository.DuelSource.RANDOM -> "公平娱乐局:当前考试池随机 7 词。"
            },
            color = c.textDim, fontSize = 12.sp,
        )

        if (source == Repository.DuelSource.FAMILY) {
            Spacer(Modifier.height(10.dp))
            BasicTextField(
                value = familyQuery,
                onValueChange = { familyQuery = it },
                singleLine = true,
                textStyle = TextStyle(color = c.text, fontSize = 14.sp),
                cursorBrush = SolidColor(c.accent),
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(c.surfaceAlt)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        if (familyQuery.isEmpty()) Text("搜索词根族(如 spect / port)", color = c.textFaint, fontSize = 14.sp)
                        inner()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            val fams = vm.familyEntries()
                .filter { it.key.contains(familyQuery.trim(), ignoreCase = true) }
                .take(24)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                fams.forEach { f ->
                    SourceChip("${f.key}·${f.count}", pickedFamily == f.key) {
                        pickedFamily = if (pickedFamily == f.key) null else f.key
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("题目 7 词", color = c.textMid, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        picked.forEach { w ->
            Row(Modifier.padding(vertical = 3.dp)) {
                Text(w.w, color = c.suffix, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(w.g ?: "", color = c.textDim, fontSize = 12.sp, maxLines = 1, modifier = Modifier.weight(1f))
            }
        }
        if (picked.size < Repository.DUEL_QUESTIONS) {
            Spacer(Modifier.height(4.dp))
            Text(
                when {
                    source == Repository.DuelSource.FAMILY && pickedFamily == null -> "↑ 先选一个词根族"
                    source == Repository.DuelSource.WEAK -> "生词本可出题词不足 7 个,换个来源或先攒生词"
                    else -> "该来源可出题词不足 7 个,换个来源"
                },
                color = c.textFaint, fontSize = 12.sp,
            )
        }

        Spacer(Modifier.height(14.dp))
        Text("第 2 步 押注与署名", color = c.accent, fontSize = 16.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("押注", color = c.text, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            Text("$bet🪙", color = c.accent, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
        Slider(
            value = bet.toFloat(),
            onValueChange = { bet = (it / 5).roundToInt() * 5 },
            valueRange = 10f..maxBet.toFloat(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(10, 30, 50, 100).forEach { v ->
                SourceChip("$v", bet == v) { if (v <= maxBet) bet = v }
            }
            Text(
                "余额 ${coins}🪙 · 7 天未战自动退回",
                color = c.textFaint, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text("我的署名(战书上显示)", color = c.textMid, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = nick,
            onValueChange = { nick = it },
            singleLine = true,
            textStyle = TextStyle(color = c.text, fontSize = 15.sp, fontWeight = FontWeight.Bold),
            cursorBrush = SolidColor(c.accent),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.surfaceAlt)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    if (nick.isEmpty()) Text("无名氏", color = c.textFaint, fontSize = 15.sp)
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                scope.launch {
                    vm.setNickname(nick)
                    onStart(vm.buildDuelQuestions(picked), bet, nick.trim().ifBlank { "无名氏" })
                }
            },
            enabled = canStart,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
        ) {
            Text(
                if (bet > coins) "金币不足" else "开始基准战(自己先打一遍)",
                fontWeight = FontWeight.Black, fontSize = 15.sp,
                modifier = Modifier.padding(vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text("基准成绩将写进战书码,对手答题时追赶你的影子", color = c.textFaint, fontSize = 11.sp)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SourceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = LocalAppColors.current
    val ia = remember { MutableInteractionSource() }
    Text(
        label,
        color = if (selected) c.onAccent else c.textMid,
        fontSize = 12.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) c.accent else c.chip)
            .clickable(interactionSource = ia, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

// ---------------- 基准战(下战书方自己打一遍,无幽灵) ----------------

@Composable
private fun BattlePane(
    vm: SlotViewModel,
    questions: List<ChallengeCodec.DuelQ>,
    onDone: (Int, Long) -> Unit,
    onQuit: () -> Unit,
) {
    val c = LocalAppColors.current
    var idx by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<String?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var leftMs by remember { mutableLongStateOf(Repository.DUEL_TIME_PER_Q_MS) }
    val startAt = remember { System.currentTimeMillis() }

    LaunchedEffect(idx) {
        leftMs = Repository.DUEL_TIME_PER_Q_MS
        while (leftMs > 0 && picked == null) {
            delay(100)
            leftMs -= 100
        }
        if (picked == null && leftMs <= 0) picked = "·timeout·"   // 超时算错
    }
    LaunchedEffect(picked) {
        if (picked != null) {
            val q = questions.getOrNull(idx) ?: return@LaunchedEffect
            if (picked == q.o[q.a]) score++
            delay(900)
            if (idx + 1 >= questions.size) onDone(score, System.currentTimeMillis() - startAt)
            else { idx++; picked = null }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("× 退出", color = c.textMid, fontSize = 15.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onQuit() }
                    .padding(horizontal = 6.dp, vertical = 4.dp))
            Spacer(Modifier.weight(1f))
            Text("基准战 ${idx + 1}/7", color = c.accent, fontSize = 15.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            Text("${(leftMs / 1000).coerceAtLeast(0)}s", color = if (leftMs < 5000) c.danger else c.textDim, fontSize = 14.sp)
        }
        // 倒计时条
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(c.surfaceAlt),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(leftMs / Repository.DUEL_TIME_PER_Q_MS.toFloat())
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (leftMs < 5000) c.danger else c.accent),
            )
        }

        if (questions.isEmpty()) {
            Spacer(Modifier.weight(1f))
            Text("题目为空", color = c.textFaint, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            return@Column
        }
        val q = questions[idx]
        Spacer(Modifier.height(34.dp))
        if (q.k == "s") {
            Text(q.m ?: q.w, color = c.prefix, fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
            Spacer(Modifier.height(8.dp))
            Text(q.g, color = c.textMid, fontSize = 15.sp)
        } else {
            Text("选出对应单词", color = c.textFaint, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Text(q.g, color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            q.o.forEach { opt ->
                val isAnswer = opt == q.o[q.a]
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
                    color = fg, fontSize = 18.sp, fontWeight = FontWeight.Bold,
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
                        ) { picked = opt }
                        .padding(vertical = 14.dp),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ---------------- 战书码生成 ----------------

@Composable
private fun CreatedPane(vm: SlotViewModel, challenge: ChallengeCodec.Challenge?, onDone: () -> Unit) {
    val c = LocalAppColors.current
    val context = androidx.compose.ui.platform.LocalContext.current
    if (challenge == null) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Text("战书生成失败(金币不足或重复)", color = c.danger, fontSize = 15.sp)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onDone, colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent)) {
                Text("返回", fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.weight(1f))
        }
        return
    }
    val code = remember(challenge.id) { ChallengeCodec.encodeChallenge(challenge) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Text("⚔ 战书已生成", color = c.accent, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(6.dp))
        Text("基准成绩 ${challenge.challengerScore}/7 · ${(challenge.challengerMs / 1000.0).let { "%.1f".format(it) }}s", color = c.textMid, fontSize = 14.sp)
        Spacer(Modifier.height(14.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(c.surface)
                .border(1.dp, c.stroke, RoundedCornerShape(12.dp))
                .padding(12.dp),
        ) {
            Text("战书码(发给好友)", color = c.textDim, fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                code,
                color = c.text, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 15.sp,
            )
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("RootSlots 战书", code))
                Toast.makeText(context, "战书码已复制,去微信发给好友吧", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
        ) { Text("复制战书码", fontWeight = FontWeight.Black, fontSize = 15.sp, modifier = Modifier.padding(vertical = 6.dp)) }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = c.surfaceAlt, contentColor = c.text),
        ) { Text("完成", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
        Spacer(Modifier.height(8.dp))
        Text(
            "押注 ${challenge.bet}🪙 已托管 · 好友在「战书 → 打战书」粘贴应战\n回执码传回后结算划转 · 7 天未战自动退回",
            color = c.textFaint, fontSize = 11.sp, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
    }
}

// ---------------- 打战书(导入 + 预览 + 应战) ----------------

@Composable
private fun ImportPane(
    vm: SlotViewModel,
    imported: ChallengeCodec.Challenge?,
    onImported: (ChallengeCodec.Challenge?) -> Unit,
    onAccepted: () -> Unit,
    onBack: () -> Unit,
) {
    val c = LocalAppColors.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    val coins = vm.coins

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("← 返回", color = c.textMid, fontSize = 15.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onBack() }
                    .padding(horizontal = 6.dp, vertical = 4.dp))
            Spacer(Modifier.weight(1f))
            Text("打战书", color = c.accent, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(40.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text("把好友发来的战书码整段粘贴到下面", color = c.textDim, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = text,
            onValueChange = { text = it; err = null },
            textStyle = TextStyle(color = c.text, fontSize = 12.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp),
            cursorBrush = SolidColor(c.accent),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.surfaceAlt)
                        .padding(10.dp),
                ) {
                    if (text.isEmpty()) Text("RS1.……", color = c.textFaint, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                scope.launch {
                    val parsed = vm.parseChallenge(text)
                    if (parsed == null) {
                        err = "无法识别:码可能被截断、不是战书码,或已导入过"
                        onImported(null)
                    } else {
                        err = null
                        onImported(parsed)
                    }
                }
            },
            enabled = text.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
        ) { Text("解析战书码", fontWeight = FontWeight.Black) }
        err?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = c.danger, fontSize = 13.sp)
        }

        imported?.let { ch ->
            Spacer(Modifier.height(16.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.surface)
                    .border(1.dp, c.stroke, RoundedCornerShape(14.dp))
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("VS ${ch.from}", color = c.text, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.weight(1f))
                    Text("押注 ${ch.bet}🪙", color = c.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Text("对方基准 ${ch.challengerScore}/7 · ${(ch.challengerMs / 1000.0).let { "%.1f".format(it) }}s", color = c.textDim, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                ch.questions.forEach { q ->
                    Row(Modifier.padding(vertical = 2.dp)) {
                        Text(q.w, color = c.suffix, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Text(q.g, color = c.textDim, fontSize = 11.sp, maxLines = 1, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(10.dp))
                val insufficient = coins < ch.bet
                Button(
                    onClick = {
                        scope.launch {
                            if (vm.acceptChallenge(ch)) onAccepted()
                            else err = "应战失败:金币不足或已导入"
                        }
                    },
                    enabled = !insufficient,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = c.accent,
                        contentColor = c.onAccent,
                        disabledContainerColor = c.chip,
                        disabledContentColor = c.textFaint,
                    ),
                ) {
                    Text(
                        if (insufficient) "金币不足(需 ${ch.bet}🪙)" else "应战(托管 ${ch.bet}🪙)",
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}
