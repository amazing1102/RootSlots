package com.cet4.rootslots.ui.review

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.cet4.rootslots.data.parseSegs
import com.cet4.rootslots.ui.slots.SlotViewModel
import com.cet4.rootslots.ui.theme.LocalAppColors
import com.cet4.rootslots.ui.theme.pressScale
import kotlinx.coroutines.launch

/** 生词本 Tab:通缉令(宿敌)+ 收藏列表 + SRS 排期 + 进入复习(复习为全屏推入页) */
@Composable
fun FavoritesScreen(
    vm: SlotViewModel,
    onOpenWord: (String) -> Unit,
    onStartReview: () -> Unit,
    onOpenNemesis: (String) -> Unit,
) {
    val c = LocalAppColors.current
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf<List<Repository.FavRow>>(emptyList()) }
    var dueCount by remember { mutableStateOf(0) }
    var nemeses by remember { mutableStateOf<List<Repository.Nemesis>>(emptyList()) }
    var badges by remember { mutableStateOf<Set<String>>(emptySet()) }

    fun reload() {
        scope.launch {
            rows = vm.favorites()
            dueCount = vm.dueCount()
            nemeses = vm.nemeses()
            badges = rows.map { it.w }.filter { vm.isDefeatedBadge(it) }.toSet()
        }
    }
    LaunchedEffect(Unit) { reload() }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
            .padding(horizontal = 14.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("生词本", color = c.accent, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            Text("${rows.size} 词", color = c.textDim, fontSize = 13.sp)
        }

        // ---- 通缉令(宿敌对决,M18a):最紧迫宿敌置顶 ----
        nemeses.firstOrNull()?.let { n ->
            Spacer(Modifier.height(10.dp))
            val ia = remember { MutableInteractionSource() }
            Row(
                Modifier
                    .fillMaxWidth()
                    .pressScale(ia, pressedScale = 0.98f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.surface)
                    .border(1.5.dp, c.danger, RoundedCornerShape(12.dp))
                    .clickable(interactionSource = ia, indication = LocalIndication.current) { onOpenNemesis(n.word) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("☠", fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "通缉:${n.word}" + if (nemeses.size > 1) " 等 ${nemeses.size} 名宿敌" else "",
                        color = c.danger, fontSize = 15.sp, fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(vm.nemesisTaunt(n.word, n.lapses), color = c.textDim, fontSize = 11.sp, maxLines = 1)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("−${n.entryFee}🪙 入场", color = c.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(if (n.revisit) "再临 · 筹码×2" else "悬赏 ${n.lapses} 级", color = c.textFaint, fontSize = 10.sp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onStartReview,
            enabled = rows.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
        ) {
            Text(
                if (dueCount > 0) "开始复习 · $dueCount 个到期" else "开始复习 · 暂无到期,可自由过一遍",
                fontWeight = FontWeight.Black, fontSize = 15.sp,
                modifier = Modifier.padding(vertical = 6.dp),
            )
        }

        Spacer(Modifier.height(10.dp))

        if (rows.isEmpty()) {
            Text("在详情页点「♡ 收藏」把生词收进来", color = c.textFaint, fontSize = 13.sp)
        }
        // 按遗忘紧迫度排序:到期/逾期置顶,已毕业与未排期垫底
        val sorted = rows.sortedByDescending { r ->
            when {
                r.srs == null -> -2f
                r.srs.stage >= Repository.STAGE_GRADUATED -> -1f
                else -> Repository.forgetProgress(r.srs) ?: -2f
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sorted, key = { it.w }) { r ->
                val stageTxt = when {
                    r.srs == null -> "未排期"
                    else -> Repository.stageLabel(r.srs.stage)
                }
                val dueTxt = when {
                    r.srs == null -> ""
                    else -> Repository.dueLabel(r.srs.dueAt)
                }
                val ia = remember { MutableInteractionSource() }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .pressScale(ia, pressedScale = 0.98f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.surface)
                        .clickable(
                            interactionSource = ia,
                            indication = LocalIndication.current,
                        ) { onOpenWord(r.w) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(r.w, color = c.suffix, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            if (r.w in badges) {
                                Spacer(Modifier.width(5.dp))
                                Text("🏅", fontSize = 12.sp)
                            }
                            r.ipa?.let {
                                Spacer(Modifier.width(6.dp))
                                Text("/$it/", color = c.textFaint, fontSize = 11.sp)
                            }
                        }
                        Text(r.g ?: "(释义待补)", color = c.textDim, fontSize = 12.sp, maxLines = 1)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stageTxt, color = c.prefix, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(dueTxt, color = if (r.srs != null && r.srs.dueAt <= System.currentTimeMillis()) c.accent else c.textFaint, fontSize = 11.sp)
                    }
                }
                // 遗忘进度:上次复习后档位间隔的消耗度(毕业 = 满条金色)
                Repository.forgetProgress(r.srs)?.let { p ->
                    Spacer(Modifier.height(8.dp))
                    val isGraduated = p < 0f
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(c.surfaceAlt)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(if (isGraduated) 1f else p)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    when {
                                        isGraduated -> c.accent
                                        p >= 1f -> c.danger
                                        p >= 0.5f -> c.accent
                                        else -> c.success
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}
}

/** 复习卡流:词 → 显示答案 → 认识/没记住 */
@Composable
fun ReviewScreen(
    vm: SlotViewModel,
    onBack: () -> Unit,
) {
    val c = LocalAppColors.current
    val scope = rememberCoroutineScope()
    var queue by remember { mutableStateOf<List<String>>(emptyList()) }
    var idx by remember { mutableStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var knownCount by remember { mutableStateOf(0) }
    var loaded by remember { mutableStateOf(false) }
    var freeMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val due = vm.dueWords()
        if (due.isEmpty()) {
            freeMode = true
            queue = vm.favorites().map { it.w }
        } else {
            queue = due
        }
        loaded = true
    }

    BackHandler { onBack() }

    if (!loaded) return

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("← 结束", color = c.textMid, fontSize = 15.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onBack() }
                    .padding(horizontal = 6.dp, vertical = 4.dp))
            Spacer(Modifier.weight(1f))
            Text("复习 ${(idx + 1).coerceAtMost(queue.size)}/${queue.size}" + if (freeMode) " · 自由模式" else "",
                color = c.textDim, fontSize = 13.sp)
        }

        if (queue.isEmpty()) {
            Spacer(Modifier.weight(1f))
            Text("收藏为空,先去收几个生词吧", color = c.textFaint, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            return@Column
        }

        if (idx >= queue.size) {
            // ---- 总结 ----
            Spacer(Modifier.weight(1f))
            Text("本轮复习完成", color = c.accent, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp))
            Text("认识 $knownCount · 没记住 ${queue.size - knownCount}", color = c.textMid, fontSize = 16.sp)
            Spacer(Modifier.height(6.dp))
            Text("认识=顺延到下一记忆节点 · 忘记=回到 5 分钟档重新爬曲线", color = c.textFaint, fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
            ) { Text("完成", fontWeight = FontWeight.Black) }
            Spacer(Modifier.weight(1f))
            return@Column
        }

        val w = queue[idx]
        Spacer(Modifier.height(48.dp))
        Text(w, color = c.suffix, fontSize = 40.sp, fontWeight = FontWeight.Black)
        vm.ipaOf(w)?.let {
            Spacer(Modifier.height(4.dp))
            Text("/$it/", color = c.textDim, fontSize = 15.sp)
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(c.chip)
                .clickable { vm.pronounce(w) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "发音",
                tint = c.prefix,
                modifier = Modifier.size(22.dp),
            )
        }

        // 答案揭示:淡入上滑(常驻组合,保证入场动画能播)
        Spacer(Modifier.weight(1f))
        AnimatedVisibility(
            visible = revealed,
            enter = fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 8 },
            exit = androidx.compose.animation.ExitTransition.None,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(vm.glossOf(w) ?: "(释义待补)", color = c.text, fontSize = 18.sp)
                val combo = vm.comboFor(w)
                if (combo != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        parseSegs(combo.segsJson).joinToString(" | ") { it.s } + "   「${combo.family}」",
                        color = c.root, fontSize = 14.sp,
                    )
                }
                // 例句:复习场景下强化记忆
                val senEn = vm.senEnOf(w)
                if (!senEn.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(senEn, color = c.text, fontSize = 15.sp, textAlign = TextAlign.Center)
                    vm.senZhOf(w)?.let {
                        Spacer(Modifier.height(2.dp))
                        Text(it, color = c.textDim, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        if (!revealed) {
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { revealed = true },
                colors = ButtonDefaults.buttonColors(containerColor = c.prefix, contentColor = c.bg),
            ) { Text("显示答案", fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) }
        } else {
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            vm.answerReview(w, false)
                            idx++; revealed = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = c.surfaceAlt, contentColor = c.text),
                ) { Text("没记住", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp)) }
                Button(
                    onClick = {
                        scope.launch {
                            vm.answerReview(w, true)
                            vm.addCoins(1)
                            knownCount++
                            idx++; revealed = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
                ) { Text("认识 +1🪙", fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 14.dp)) }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}
