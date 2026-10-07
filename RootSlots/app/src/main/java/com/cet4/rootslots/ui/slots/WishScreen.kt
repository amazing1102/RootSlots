package com.cet4.rootslots.ui.slots

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cet4.rootslots.data.GamePrefs
import com.cet4.rootslots.ui.theme.LocalAppColors
import com.cet4.rootslots.ui.theme.pressScale
import kotlinx.coroutines.launch

/**
 * 定向转轴·许愿(M18b):30 币选定一个薄弱词族,下一转必出该族词(一次性,不可叠加,
 * 24h 未用失效退币)。金币买到"刻意练习"——学习者自己最清楚薄弱族在哪。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WishScreen(vm: SlotViewModel, onBack: () -> Unit) {
    val c = LocalAppColors.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf<String?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }
    val wish by vm.wish.collectAsState()

    BackHandler { onBack() }

    val families = remember(query) {
        vm.familyEntries().filter { it.key.contains(query.trim(), ignoreCase = true) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
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
            Text("定向转轴 · ${GamePrefs.WISH_COST}🪙", color = c.accent, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(40.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "许愿一个词根族,下一转必出该族词(一次性)。\n已有许愿时不可叠加 · 24 小时未用自动退币。",
            color = c.textDim, fontSize = 12.sp,
        )
        Spacer(Modifier.height(10.dp))
        BasicTextField(
            value = query,
            onValueChange = { query = it },
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
                    if (query.isEmpty()) Text("搜索词根族(如 spect / port)", color = c.textFaint, fontSize = 14.sp)
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))

        wish?.let { w ->
            Text(
                "当前许愿:$w(下一转生效)",
                color = c.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(families, key = { it.key }) { f ->
                val selected = picked == f.key
                val ia = remember(f.key) { MutableInteractionSource() }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .pressScale(ia, pressedScale = 0.98f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) c.chip else c.surface)
                        .border(1.dp, if (selected) c.accent else c.stroke, RoundedCornerShape(10.dp))
                        .clickable(interactionSource = ia, indication = LocalIndication.current) { picked = f.key }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(f.key, color = c.root, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text(f.meaning, color = c.textDim, fontSize = 11.sp, maxLines = 1, modifier = Modifier.weight(1f))
                    Text("${f.count} 词", color = c.textFaint, fontSize = 11.sp)
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        msg?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = if (it.startsWith("✓")) c.success else c.danger, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
        }
        val w = wish
        Button(
            onClick = {
                val target = picked
                if (target == null) {
                    msg = "先在上方选一个词根族"
                } else {
                    scope.launch {
                        if (vm.makeWish(target)) {
                            msg = "✓ 许愿成功:下一转必出「$target」族"
                            picked = null
                        } else if (w != null) {
                            msg = "已有许愿进行中($w),用掉或等失效后再许"
                        } else {
                            msg = "金币不足(需 ${GamePrefs.WISH_COST}🪙)"
                        }
                    }
                }
            },
            enabled = picked != null || w != null,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = c.accent, contentColor = c.onAccent,
                disabledContainerColor = c.chip, disabledContentColor = c.textFaint,
            ),
        ) {
            Text(
                when {
                    w != null -> "已有许愿($w)· 不可叠加"
                    picked != null -> "许愿「$picked」· −${GamePrefs.WISH_COST}🪙"
                    else -> "选一个词根族"
                },
                fontWeight = FontWeight.Black, fontSize = 15.sp,
                modifier = Modifier.padding(vertical = 5.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
    }
}
