package com.cet4.rootslots.ui.nav

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cet4.rootslots.ui.theme.LocalAppColors

/**
 * 底部 Tab(信息架构定稿:5 Tab,详情/复习为无 Tab 栏的全屏推入页)。
 * 用 enum 而非 sealed class + companion 列表:companion 在 <clinit> 期回读
 * data object 单例会因类初始化循环读到 null(已在真机崩过一次)。
 */
enum class RootTab(val label: String, val icon: ImageVector) {
    Slots("老虎机", Icons.Filled.Casino),
    Codex("图鉴", Icons.Filled.MenuBook),
    Favorites("生词", Icons.Filled.FavoriteBorder),
    Quiz("测验", Icons.Filled.Quiz),
    Mine("我的", Icons.Filled.Person),
}

@Composable
fun RootTabBar(selected: RootTab, dueCount: Int, onTab: (RootTab) -> Unit) {
    val c = LocalAppColors.current
    NavigationBar(containerColor = c.surface) {
        RootTab.entries.forEach { tab ->
            val isSel = tab == selected
            // 选中图标弹簧放大一小格,落点有"咔哒"感
            val iconScale by animateFloatAsState(
                targetValue = if (isSel) 1.14f else 1f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
                label = "tabIconScale",
            )
            NavigationBarItem(
                selected = isSel,
                onClick = { onTab(tab) },
                icon = {
                    BadgedBox(badge = {
                        if (tab == RootTab.Favorites && dueCount > 0) {
                            Badge(containerColor = c.danger) {
                                Text("$dueCount", fontSize = 10.sp, color = c.onAccent)
                            }
                        }
                    }) {
                        Icon(
                            imageVector = if (tab == RootTab.Favorites && isSel) Icons.Filled.Favorite else tab.icon,
                            contentDescription = tab.label,
                            modifier = Modifier.graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            },
                        )
                    }
                },
                label = { Text(tab.label, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = c.accent,
                    selectedTextColor = c.accent,
                    unselectedIconColor = c.textDim,
                    unselectedTextColor = c.textDim,
                    indicatorColor = c.accent.copy(alpha = 0.16f),
                ),
            )
        }
    }
}
