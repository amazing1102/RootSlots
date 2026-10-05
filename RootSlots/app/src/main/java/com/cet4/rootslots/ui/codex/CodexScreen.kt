package com.cet4.rootslots.ui.codex

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cet4.rootslots.data.FamilyEntity
import com.cet4.rootslots.ui.slots.SlotViewModel
import com.cet4.rootslots.ui.theme.LocalAppColors
import com.cet4.rootslots.ui.theme.pressScale

/** 词根图鉴:354 词根族,搜索 + 转出进度;统计卡在「我的」页 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CodexScreen(
    vm: SlotViewModel,
    onOpenWord: (String) -> Unit,
) {
    val c = LocalAppColors.current
    var families by remember { mutableStateOf<List<FamilyEntity>>(emptyList()) }
    var famWords by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var spun by remember { mutableStateOf(setOf<String>()) }
    var expanded by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    val exams by vm.exams.collectAsState()

    // 考试集合变更时重载(族列表按目标考试过滤,空族不显示)
    LaunchedEffect(exams) {
        val fs = vm.familyEntries()
        families = fs
        famWords = fs.associate { it.key to vm.familyWordList(it.key) }
        spun = vm.spunWordSet()
        if (expanded != null && famWords[expanded].isNullOrEmpty()) expanded = null
    }

    val shown = if (query.isBlank()) families
        else families.filter {
            it.key.contains(query.trim(), ignoreCase = true) || it.meaning.contains(query.trim())
        }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
            .padding(horizontal = 14.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("词根图鉴", color = c.accent, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            Text("🪙 ${vm.coins}", color = c.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(10.dp))

        // ---- 搜索 ----
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = c.textDim) },
            placeholder = { Text("搜索词根或含义,如 vis / 看", color = c.textFaint, fontSize = 13.sp) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = c.accent,
                unfocusedBorderColor = c.stroke,
                focusedContainerColor = c.surface,
                unfocusedContainerColor = c.surface,
                cursorColor = c.accent,
                focusedTextColor = c.text,
                unfocusedTextColor = c.text,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(10.dp))

        // ---- 词根族列表(单行紧凑卡) ----
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (shown.isEmpty()) {
                item {
                    Text("没有匹配的词根", color = c.textFaint, fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 20.dp))
                }
            }
            items(shown, key = { it.key }) { fam ->
                val touched = (famWords[fam.key] ?: emptyList()).count { it in spun }
                val isExp = expanded == fam.key
                val ia = remember { MutableInteractionSource() }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .pressScale(ia, pressedScale = 0.98f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.surface)
                        .border(
                            1.dp,
                            if (touched > 0) c.accent.copy(alpha = 0.55f) else c.stroke,
                            RoundedCornerShape(12.dp),
                        )
                        .clickable(
                            interactionSource = ia,
                            indication = LocalIndication.current,
                        ) { expanded = if (isExp) null else fam.key }
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(fam.key, color = c.root, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(8.dp))
                        val m = fam.meaning.takeIf { it.isNotBlank() && it != "?" } ?: "(含义待补)"
                        Text(m, color = c.textMid, fontSize = 13.sp, maxLines = 1, modifier = Modifier.weight(1f))
                        if (touched > 0) {
                            Text("已转出 $touched/${fam.count}", color = c.prefix, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text("未遇到 · ${fam.count}词", color = c.textFaint, fontSize = 11.sp)
                        }
                    }
                    // 词表展开/收起:纵向展开 + 淡入淡出
                    AnimatedVisibility(
                        visible = isExp,
                        enter = expandVertically(tween(240)) + fadeIn(tween(240)),
                        exit = shrinkVertically(tween(180)) + fadeOut(tween(180)),
                    ) {
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            (famWords[fam.key] ?: emptyList()).forEach { w ->
                                val seen = w in spun
                                Text(
                                    w,
                                    color = if (seen) c.onAccent else c.suffix,
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(if (seen) c.accent else c.chip)
                                        .clickable { onOpenWord(w) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
