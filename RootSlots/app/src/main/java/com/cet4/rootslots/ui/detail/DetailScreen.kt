package com.cet4.rootslots.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cet4.rootslots.data.ComboEntity
import com.cet4.rootslots.data.Repository
import com.cet4.rootslots.data.SrsEntity
import com.cet4.rootslots.data.parseSegs
import com.cet4.rootslots.ui.slots.SlotViewModel
import com.cet4.rootslots.ui.theme.LocalAppColors
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.ln

/** 单词详情:彩色分段 + 各段词法释义 + 词根族 + 艾宾浩斯记忆计划 + 收藏 + 发音 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    word: String,
    vm: SlotViewModel,
    onBack: () -> Unit,
    onOpenWord: (String) -> Unit,
) {
    val c = LocalAppColors.current
    BackHandler(onBack = onBack)
    val scope = rememberCoroutineScope()
    val combo = remember(word) { vm.comboFor(word) }
    val segs = remember(word) { combo?.let { parseSegs(it.segsJson) } ?: emptyList() }
    var fav by remember(word) { mutableStateOf(false) }
    var srs by remember(word) { mutableStateOf<SrsEntity?>(null) }
    LaunchedEffect(word) {
        fav = vm.isFavorite(word)
        srs = vm.srsOf(word)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(6.dp))
        // ---- 顶栏 ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("← 返回", color = c.textMid, fontSize = 15.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onBack() }
                    .padding(horizontal = 6.dp, vertical = 4.dp))
            Spacer(Modifier.weight(1f))
            Text("🔊", fontSize = 22.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.chip)
                    .clickable { vm.pronounce(word) }
                    .padding(horizontal = 12.dp, vertical = 6.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (fav) "♥ 已收藏" else "♡ 收藏",
                color = if (fav) c.accent else c.textMid,
                fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.chip)
                    .clickable {
                        scope.launch {
                            fav = vm.toggleFavorite(word)
                            srs = vm.srsOf(word)   // 收藏/取消后立即刷新记忆计划卡
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp))
        }

        Spacer(Modifier.height(18.dp))

        if (combo == null) {
            // ---- 整词模式(无构词拆解) ----
            Text(word, color = c.text, fontSize = 34.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Text(vm.glossOf(word) ?: "", color = c.textMid, fontSize = 16.sp)
            Text("(该词暂无构词拆解)", color = c.textDim, fontSize = 13.sp)
        } else {
            // ---- 大词 + 分段 ----
            Row(verticalAlignment = Alignment.Bottom) {
                segs.forEach { seg ->
                    Text(
                        seg.s,
                        color = c.typeColor(seg.t),
                        fontSize = if (word.length > 10) 38.sp else 44.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(vm.glossOf(word) ?: "", color = c.textMid, fontSize = 16.sp)

            Spacer(Modifier.height(20.dp))

            // ---- 各段释义卡 ----
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                segs.forEach { seg ->
                    val color = c.typeColor(seg.t)
                    Column(
                        Modifier
                            .width(150.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(c.surface)
                            .padding(12.dp)
                    ) {
                        Text(seg.s, color = color, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Text("${c.typeName(seg.t)} · ${seg.k}",
                            color = color.copy(alpha = 0.75f), fontSize = 11.sp)
                        Spacer(Modifier.height(6.dp))
                        val segMeaning = vm.meaningOf(seg.t, seg.k)
                            .takeIf { it.isNotBlank() && it != "?" } ?: "(释义待补)"
                        Text(
                            segMeaning,
                            color = c.textMid, fontSize = 13.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ---- 词根族卡片 ----
            val fam = combo.family
            val famMeaning = vm.familyMeaning(fam).takeIf { it.isNotBlank() && it != "?" } ?: "(含义待补)"
            Text("词根族", color = c.textDim, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text("「$fam $famMeaning」", color = c.root, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("共 ${vm.familyWordList(fam).size} 个表内词,点击跳转:", color = c.textDim, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            vm.familyWordList(fam).forEach { w ->
                val isCurrent = w == word
                Text(
                    w,
                    color = if (isCurrent) c.bg else c.suffix,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isCurrent) c.suffix else c.chip)
                        .clickable(enabled = !isCurrent) { onOpenWord(w) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }
        }

        Spacer(Modifier.height(20.dp))

        // ---- 艾宾浩斯记忆计划(收藏即排期;曲线=复习把遗忘曲线一次次拉回 100%) ----
        MemoryCard(vm, word, srs)

        Spacer(Modifier.height(28.dp))
    }
}

/** 艾宾浩斯卡:进度行 + 保持率锯齿曲线 + 9 节点时间轴 */
@Composable
private fun MemoryCard(vm: SlotViewModel, word: String, srs: SrsEntity?) {
    val c = LocalAppColors.current
    val stage = srs?.stage ?: 0
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("艾宾浩斯记忆计划", color = c.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(
                when {
                    srs == null -> "未加入"
                    else -> Repository.stageLabel(stage)
                },
                color = if (srs == null) c.textFaint else c.accent,
                fontSize = 13.sp, fontWeight = FontWeight.Black,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            when {
                srs == null -> "点「收藏」自动加入,共 9 个记忆节点"
                stage >= Repository.STAGE_GRADUATED ->
                    "已通过全部节点,进入长期记忆 🎉 复习 ${srs.reviews} 次 · 忘记 ${srs.lapses} 次"
                else ->
                    "节点 ${stage.coerceIn(1, 9)}/${Repository.NODE_LABELS.size} · " +
                        "下次复习 ${Repository.dueLabel(srs.dueAt)} · " +
                        "复习 ${srs.reviews} 次 · 忘记 ${srs.lapses} 次"
            },
            color = c.textDim, fontSize = 12.sp,
        )

        Spacer(Modifier.height(8.dp))
        MemoryCurve(stage = stage, height = 150.dp)
        Spacer(Modifier.height(2.dp))
        Text(
            "认识 → 顺延到下一节点 · 忘记 → 回到 5 分钟档重新爬曲线",
            color = c.textFaint, fontSize = 10.sp,
        )
    }
}

/**
 * 保持率锯齿曲线:横轴为 9 个记忆节点(等距),纵轴为记忆保持率。
 * 每段按指数衰减掉到 ~32%,复习节点把曲线拉回 100%;
 * 已通过的节点段用金色实线,未来段用灰虚线,下一节点用蓝色圆点强调。
 */
@Composable
private fun MemoryCurve(stage: Int, height: androidx.compose.ui.unit.Dp) {
    val c = LocalAppColors.current
    val labels = Repository.NODE_SHORT
    val n = labels.size
    Canvas(Modifier.fillMaxWidth().height(height)) {
        val padL = 6.dp.toPx(); val padR = 6.dp.toPx()
        val padT = 8.dp.toPx(); val padB = 22.dp.toPx()
        val w = size.width - padL - padR
        val h = size.height - padT - padB
        val floor = 0.32f

        fun x(i: Int) = padL + w * i / (n - 1)
        fun y(r: Float) = padT + h * (1f - r)

        // 网格:100% / 66% / 33%
        listOf(1f, 0.66f, floor).forEach { r ->
            drawLine(c.stroke, Offset(padL, y(r)), Offset(padL + w, y(r)), 1.dp.toPx())
        }

        // 节点间衰减曲线
        fun decayPath(i: Int): Path {
            val p = Path()
            val x0 = x(i); val x1 = x(i + 1)
            p.moveTo(x0, y(1f))
            val steps = 24
            for (k in 1..steps) {
                val t = k.toFloat() / steps
                p.lineTo(x0 + (x1 - x0) * t, y(exp(ln(floor.toDouble()) * t).toFloat()))
            }
            return p
        }
        for (i in 0 until n - 1) {
            val passed = stage >= i + 2          // 已跨过节点 i+1
            val path = decayPath(i)
            if (passed) {
                drawPath(path, c.accent, style = Stroke(width = 2.5.dp.toPx()))
            } else {
                drawPath(
                    path, c.textFaint, alpha = 0.55f,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 9f)),
                    ),
                )
            }
            // 复习拉回 100% 的竖向跳升(虚线)
            if (i > 0) {
                drawLine(
                    c.stroke, Offset(x(i), y(floor)), Offset(x(i), y(1f)),
                    1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                )
            }
        }

        // 节点圆点 + 标签
        val labelPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 9.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            color = c.textFaint.toArgb()
        }
        val labelPaintNext = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 10.sp.toPx()
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.CENTER
            color = c.prefix.toArgb()
        }
        for (j in 0 until n) {
            val passed = stage >= j + 2 || stage >= Repository.STAGE_GRADUATED
            val isNext = stage == j + 1 || (stage == 0 && j == 0)
            val center = Offset(x(j), y(1f))
            if (isNext) {
                drawCircle(c.prefix, 5.dp.toPx(), center)
                drawCircle(c.surface, 2.dp.toPx(), center)
            } else if (passed) {
                drawCircle(c.accent, 4.5.dp.toPx(), center)
            } else {
                drawCircle(c.stroke, 4.dp.toPx(), center)
                drawCircle(c.bg, 2.dp.toPx(), center)
            }
            drawContext.canvas.nativeCanvas.drawText(
                labels[j], x(j), size.height - 5.dp.toPx(),
                if (isNext) labelPaintNext else labelPaint,
            )
        }
    }
}
