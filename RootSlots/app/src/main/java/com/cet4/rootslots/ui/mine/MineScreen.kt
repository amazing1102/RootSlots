package com.cet4.rootslots.ui.mine

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cet4.rootslots.data.GamePrefs
import com.cet4.rootslots.ui.slots.SlotViewModel
import com.cet4.rootslots.ui.theme.LocalAppColors
import java.time.LocalDate
import kotlin.math.roundToInt

/** 我的:收集统计(自图鉴迁来)+ 外观/发音/数据设置 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MineScreen(vm: SlotViewModel) {
    val c = LocalAppColors.current
    val themeMode by vm.themeMode.collectAsState()
    val rate by vm.speechRate.collectAsState()
    val dailyStats by vm.dailyStats.collectAsState()
    val exams by vm.exams.collectAsState()
    var spins by remember { mutableStateOf(0) }
    var distinct by remember { mutableStateOf(0) }
    var families by remember { mutableStateOf(0) }
    var covered by remember { mutableStateOf(0) }
    var vocab by remember { mutableStateOf(Triple(0, 0, 0)) }

    LaunchedEffect(exams) {
        spins = vm.spinTotal()
        distinct = vm.spunDistinct()
        val spun = vm.spunWordSet()
        val fs = vm.familyEntries()
        families = fs.size
        covered = fs.count { f -> vm.familyWordList(f.key).any { it in spun } }
        vocab = vm.vocabCounts()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Text("我的", color = c.accent, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(14.dp))

        // ---- 统计卡(数字滚动入场) ----
        val spinsA by animateIntAsState(spins, tween(750, easing = FastOutSlowInEasing), label = "spins")
        val distinctA by animateIntAsState(distinct, tween(750, easing = FastOutSlowInEasing), label = "distinct")
        val coveredA by animateIntAsState(covered, tween(750, easing = FastOutSlowInEasing), label = "covered")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("累计转动", "$spinsA", c.prefix, Modifier.weight(1f))
            StatCard("见过单词", "$distinctA", c.suffix, Modifier.weight(1f))
            StatCard("族覆盖", "$coveredA/$families", c.accent, Modifier.weight(1f))
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("复习统计")
        Spacer(Modifier.height(8.dp))
        Card {
            var weekly by remember { mutableStateOf(List(7) { 0 }) }
            var achievement by remember { mutableStateOf(0f) }
            LaunchedEffect(Unit) {
                weekly = vm.weeklyReviews()
                achievement = vm.memoryAchievement()
            }
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                    AchievementRing(achievement, Modifier.fillMaxSize())
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${(achievement * 100).roundToInt()}%",
                            color = c.accent, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Text("达成率", color = c.textFaint, fontSize = 9.sp)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    WeekBars(weekly)
                    Spacer(Modifier.height(4.dp))
                    Text("近 7 天复习量 · 达成率 = 未逾期生词占比",
                        color = c.textFaint, fontSize = 9.sp)
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("每日目标")
        Spacer(Modifier.height(8.dp))
        Card {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("今日已转", color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${vm.spinsToday} / ${vm.dailyGoal}",
                        color = if (vm.spinsToday >= vm.dailyGoal) c.success else c.accent,
                        fontSize = 15.sp, fontWeight = FontWeight.Black,
                    )
                }
                Spacer(Modifier.height(8.dp))
                // 进度条:今日已转 / 目标
                val fraction = if (vm.dailyGoal > 0) {
                    (vm.spinsToday.toFloat() / vm.dailyGoal).coerceIn(0f, 1f)
                } else 0f
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(c.surfaceAlt)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (vm.spinsToday >= vm.dailyGoal) c.success else c.accent)
                    )
                }
                Spacer(Modifier.height(12.dp))
                // 每日目标:自由输入(数字键盘,失焦/完成时提交),不再是固定档位
                var goalText by remember { mutableStateOf(vm.dailyGoal.toString()) }
                LaunchedEffect(vm.dailyGoal) { goalText = vm.dailyGoal.toString() }
                var goalFocused by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("每天学习", color = c.textMid, fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    OutlinedTextField(
                        value = goalText,
                        onValueChange = { s -> if (s.length <= 4 && s.all { it.isDigit() }) goalText = s },
                        modifier = Modifier
                            .width(96.dp)
                            .onFocusChanged {
                                if (goalFocused && !it.isFocused) {
                                    val n = goalText.toIntOrNull()
                                    if (n != null && n >= 1) vm.updateDailyGoal(n) else goalText = vm.dailyGoal.toString()
                                }
                                goalFocused = it.isFocused
                            },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 16.sp, fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center, color = c.text,
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            val n = goalText.toIntOrNull()
                            if (n != null && n >= 1) vm.updateDailyGoal(n) else goalText = vm.dailyGoal.toString()
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = c.accent,
                            unfocusedBorderColor = c.stroke,
                            cursorColor = c.accent,
                            focusedTextColor = c.text,
                            unfocusedTextColor = c.text,
                        ),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("词 / 天", color = c.textMid, fontSize = 14.sp)
                }
                Spacer(Modifier.height(6.dp))
                Text("目标为每天学习的词数(1–999),转一次 = 学一词,达成后进度条变绿",
                    color = c.textFaint, fontSize = 10.sp)
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("目标考试")
        Spacer(Modifier.height(8.dp))
        Card {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("出题范围", color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("可多选 · 并集生效", color = c.textFaint, fontSize = 11.sp)
                }
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GamePrefs.EXAM_CHOICES.forEach { (code, label) ->
                        val selected = code in exams
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) c.accent else c.chip)
                                .clickable {
                                    val next = if (selected) exams - code else exams + code
                                    vm.setExams(next)   // 空集由 GamePrefs 兜底回默认 cet4
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (selected) {
                                Text("✓ ", color = c.onAccent, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            }
                            Text(
                                label,
                                color = if (selected) c.onAccent else c.textMid,
                                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("老虎机 / 测验 / 词根图鉴只从所选考试的词池出题;生词本与复习计划不受影响",
                    color = c.textFaint, fontSize = 10.sp)
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("学习日历")
        Spacer(Modifier.height(8.dp))
        Card {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                val today = LocalDate.now()
                val first = today.withDayOfMonth(1)
                val stats = dailyStats
                // 连续达标天数:今天没达标就从昨天往前数(今天还没结束,不算断签)
                var streak = 0
                var cursor = if (stats[today]?.let { it.count >= it.goal } == true) today else today.minusDays(1)
                while (stats[cursor]?.let { it.count >= it.goal } == true) {
                    streak++
                    cursor = cursor.minusDays(1)
                }
                val monthStats = stats.filterKeys { it.month == first.month && it.year == first.year }
                val monthCount = monthStats.values.sumOf { it.count }
                val monthHit = monthStats.values.count { it.count >= it.goal }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${first.year} 年 ${first.monthValue} 月",
                        color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (streak > 0) "🔥 连续 $streak 天达标" else "今天学一点,点亮格子",
                        color = if (streak > 0) c.accent else c.textFaint,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth()) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                        Text(it, color = c.textFaint, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(4.dp))
                val lead = first.dayOfWeek.value - 1   // 周一 = 0
                val cells: List<Int?> = List(lead) { null } + (1..first.lengthOfMonth()).map { it }
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { d ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (d == null) {
                                    Spacer(Modifier.height(34.dp))
                                } else {
                                    val date = first.withDayOfMonth(d)
                                    val stat = stats[date]
                                    val isFuture = date.isAfter(today)
                                    val isToday = date == today
                                    val hit = stat != null && stat.count >= stat.goal
                                    val bg = when {
                                        isFuture -> Color.Transparent
                                        stat == null || stat.count == 0 -> c.surfaceAlt.copy(alpha = 0.45f)
                                        hit -> c.accent
                                        else -> c.accent.copy(
                                            alpha = 0.25f + 0.5f * (stat.count.toFloat() / stat.goal.coerceAtLeast(1)).coerceIn(0f, 1f),
                                        )
                                    }
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(bg)
                                            .then(
                                                if (isToday) Modifier.border(1.5.dp, c.accent, RoundedCornerShape(8.dp))
                                                else Modifier
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "$d",
                                            color = when {
                                                isFuture -> c.textFaint.copy(alpha = 0.5f)
                                                hit -> c.onAccent
                                                else -> c.textDim
                                            },
                                            fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("本月共学 $monthCount 词 · 达标 $monthHit 天", color = c.textFaint, fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("外观")
        Spacer(Modifier.height(8.dp))
        Card {
            ThemeOption("跟随系统", "白天浅色、夜晚深色", themeMode == 0) { vm.setThemeMode(0) }
            Divider()
            ThemeOption("深色 · 赌场夜场", "墨紫底 + 暖金灯管", themeMode == 1) { vm.setThemeMode(1) }
            Divider()
            ThemeOption("浅色 · 纸面暖光", "米白底 + 深色字,日间不刺眼", themeMode == 2) { vm.setThemeMode(2) }
        }

        Spacer(Modifier.height(10.dp))
        Card {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("构词三色", color = c.textMid, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                TypeSwatch("前", c.prefix)
                TypeSwatch("根", c.root)
                TypeSwatch("缀", c.suffix)
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("发音")
        Spacer(Modifier.height(8.dp))
        Card {
            Row(
                Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("引擎", color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(vm.engineStatus, color = c.textDim, fontSize = 12.sp)
            }
            Divider()
            Row(
                Modifier.fillMaxWidth().clickable { vm.pronounce("reduction") }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "试听",
                    tint = c.prefix,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("试听发音", color = c.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("example · reduction", color = c.textDim, fontSize = 12.sp)
            }
            Divider()
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("语速", color = c.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("×${"%.2f".format(rate)}", color = c.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = rate,
                    onValueChange = { vm.setSpeechRate(((it * 20).roundToInt()) / 20f) },
                    valueRange = 0.5f..1.2f,
                    steps = 12,
                    colors = SliderDefaults.colors(
                        thumbColor = c.accent,
                        activeTrackColor = c.accent,
                        inactiveTrackColor = c.surfaceAlt,
                    ),
                )
                Row(Modifier.fillMaxWidth()) {
                    Text("慢", color = c.textFaint, fontSize = 11.sp)
                    Spacer(Modifier.weight(1f))
                    Text("快", color = c.textFaint, fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("数据")
        Spacer(Modifier.height(8.dp))
        Card {
            InfoRow("词库", "${vocab.first} 词 · 组合词 ${vocab.second} · 词根族 ${vocab.third}")
            Divider()
            InfoRow("运行", "完全离线 · 无账号 · 数据在本机")
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(c.surface)
            .border(1.dp, c.stroke, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = color, fontSize = 17.sp, fontWeight = FontWeight.Black)
        Text(label, color = c.textDim, fontSize = 11.sp)
    }
}

@Composable
private fun AchievementRing(progress: Float, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    // 达成率圆环从 0 转起来
    val anim by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "ring",
    )
    Canvas(modifier) {
        val stroke = 8.dp.toPx()
        val inset = stroke / 2
        val arcSize = size.width - stroke
        drawArc(
            color = c.surfaceAlt,
            startAngle = 0f, sweepAngle = 360f, useCenter = false,
            topLeft = Offset(inset, inset), size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawArc(
            color = c.accent,
            startAngle = -90f, sweepAngle = 360f * anim, useCenter = false,
            topLeft = Offset(inset, inset), size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun WeekBars(weekly: List<Int>) {
    val c = LocalAppColors.current
    // 柱子从地面长出来
    val appear = remember { Animatable(0f) }
    LaunchedEffect(weekly) {
        appear.snapTo(0f)
        appear.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
    }
    val labels = (6 downTo 0).map { back ->
        val d = java.time.LocalDate.now().minusDays(back.toLong()).dayOfWeek.value
        "一二三四五六日"[d - 1].toString()
    }
    val max = (weekly.maxOrNull() ?: 0).coerceAtLeast(1)
    Row(
        Modifier.fillMaxWidth().height(76.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        weekly.forEachIndexed { i, count ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$count", color = if (count > 0) c.textMid else c.textFaint,
                    fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Box(
                    Modifier
                        .width(16.dp)
                        .height(((8 + 44f * count / max) * appear.value).dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (count > 0) c.accent else c.surfaceAlt)
                )
                Spacer(Modifier.height(2.dp))
                Text(if (i == weekly.size - 1) "今天" else labels[i],
                    color = c.textFaint, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    val c = LocalAppColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .border(1.dp, c.stroke, RoundedCornerShape(16.dp))
    ) { content() }
}

@Composable
private fun Divider() {
    val c = LocalAppColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(1.dp)
            .background(c.stroke)
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = LocalAppColors.current.textDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun ThemeOption(title: String, sub: String, selected: Boolean, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(sub, color = c.textDim, fontSize = 12.sp)
        }
        Box(
            Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(50))
                .border(
                    2.dp,
                    if (selected) c.accent else c.stroke,
                    RoundedCornerShape(50),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(c.accent))
        }
    }
}

@Composable
private fun TypeSwatch(label: String, color: Color) {
    Box(
        Modifier
            .padding(start = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val c = LocalAppColors.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
        Text(label, color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text(value, color = c.textDim, fontSize = 12.sp)
    }
}
