package com.cet4.rootslots.ui.review

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cet4.rootslots.data.Repository
import com.cet4.rootslots.data.parseSegs
import com.cet4.rootslots.ui.slots.SlotViewModel
import com.cet4.rootslots.ui.theme.LocalAppColors
import kotlinx.coroutines.launch

/** 生词本 Tab:收藏列表 + SRS 排期 + 进入复习(复习为全屏推入页) */
@Composable
fun FavoritesScreen(
    vm: SlotViewModel,
    onOpenWord: (String) -> Unit,
    onStartReview: () -> Unit,
) {
    val c = LocalAppColors.current
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf<List<Repository.FavRow>>(emptyList()) }
    var dueCount by remember { mutableStateOf(0) }

    fun reload() {
        scope.launch {
            rows = vm.favorites()
            dueCount = vm.dueCount()
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
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rows, key = { it.w }) { r ->
                val stageTxt = when {
                    r.srs == null -> "未排期"
                    else -> Repository.stageLabel(r.srs.stage)
                }
                val dueTxt = when {
                    r.srs == null -> ""
                    else -> Repository.dueLabel(r.srs.dueAt)
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.surface)
                        .clickable { onOpenWord(r.w) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(r.w, color = c.suffix, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
        Text("🔊", fontSize = 26.sp, modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { vm.pronounce(w) }
            .padding(6.dp))

        if (!revealed) {
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { revealed = true },
                colors = ButtonDefaults.buttonColors(containerColor = c.prefix, contentColor = c.bg),
            ) { Text("显示答案", fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) }
            Spacer(Modifier.weight(1f))
        } else {
            Spacer(Modifier.height(16.dp))
            Text(vm.glossOf(w) ?: "(释义待补)", color = c.text, fontSize = 18.sp)
            val combo = vm.comboFor(w)
            if (combo != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    parseSegs(combo.segsJson).joinToString(" | ") { it.s } + "   「${combo.family}」",
                    color = c.root, fontSize = 14.sp,
                )
            }
            Spacer(Modifier.weight(1f))
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
            Spacer(Modifier.height(20.dp))
        }
    }
}
