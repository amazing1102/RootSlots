package com.cet4.rootslots.ui.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cet4.rootslots.ui.slots.SlotViewModel
import com.cet4.rootslots.ui.theme.LocalAppColors
import kotlin.math.roundToInt

/** 我的:收集统计(自图鉴迁来)+ 外观/发音/数据设置 */
@Composable
fun MineScreen(vm: SlotViewModel) {
    val c = LocalAppColors.current
    val themeMode by vm.themeMode.collectAsState()
    val rate by vm.speechRate.collectAsState()
    var spins by remember { mutableStateOf(0) }
    var distinct by remember { mutableStateOf(0) }
    var families by remember { mutableStateOf(0) }
    var covered by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        spins = vm.spinTotal()
        distinct = vm.spunDistinct()
        val spun = vm.spunWordSet()
        val fs = vm.familyEntries()
        families = fs.size
        covered = fs.count { f -> vm.familyWordList(f.key).any { it in spun } }
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

        // ---- 统计卡 ----
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("累计转动", "$spins", c.prefix, Modifier.weight(1f))
            StatCard("见过单词", "$distinct", c.suffix, Modifier.weight(1f))
            StatCard("族覆盖", "$covered/$families", c.accent, Modifier.weight(1f))
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
                Text("🔊 试听发音", color = c.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
            InfoRow("词库", "6286 词 · 组合词 2297 · 词根族 354")
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
