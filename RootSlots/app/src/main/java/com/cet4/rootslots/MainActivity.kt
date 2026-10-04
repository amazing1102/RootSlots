package com.cet4.rootslots

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cet4.rootslots.ui.codex.CodexScreen
import com.cet4.rootslots.ui.detail.DetailScreen
import com.cet4.rootslots.ui.mine.MineScreen
import com.cet4.rootslots.ui.nav.RootTab
import com.cet4.rootslots.ui.nav.RootTabBar
import com.cet4.rootslots.ui.quiz.QuizScreen
import com.cet4.rootslots.ui.review.FavoritesScreen
import com.cet4.rootslots.ui.review.ReviewScreen
import com.cet4.rootslots.ui.slots.SlotScreen
import com.cet4.rootslots.ui.slots.SlotViewModel
import com.cet4.rootslots.ui.theme.RootSlotsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: SlotViewModel = viewModel()
            val themeMode by vm.themeMode.collectAsState()
            RootSlotsTheme(themeMode) {
                AppNav(vm)
            }
        }
    }
}

/** 全屏推入页:盖在 Tab 内容上,无底部栏,←/系统返回键关闭 */
private sealed class Overlay {
    data class Detail(val word: String) : Overlay()
    data object Review : Overlay()
}

@Composable
private fun AppNav(vm: SlotViewModel) {
    var tab by remember { mutableStateOf<RootTab>(RootTab.Slots) }
    var overlay by remember { mutableStateOf<Overlay?>(null) }
    var dueCount by remember { mutableIntStateOf(0) }

    // 生词 Tab 角标:每次切 Tab/关推入页后刷新
    LaunchedEffect(tab, overlay) { dueCount = vm.dueCount() }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (val o = overlay) {
                is Overlay.Detail -> DetailScreen(
                    word = o.word, vm = vm,
                    onBack = { overlay = null },
                    onOpenWord = { overlay = Overlay.Detail(it) },
                )
                Overlay.Review -> ReviewScreen(vm = vm, onBack = { overlay = null })
                null -> when (tab) {
                    RootTab.Codex -> CodexScreen(
                        vm = vm,
                        onOpenWord = { overlay = Overlay.Detail(it) },
                    )
                    RootTab.Favorites -> FavoritesScreen(
                        vm = vm,
                        onOpenWord = { overlay = Overlay.Detail(it) },
                        onStartReview = { overlay = Overlay.Review },
                    )
                    RootTab.Mine -> MineScreen(vm = vm)
                    RootTab.Quiz -> QuizScreen(vm = vm)
                    RootTab.Slots -> SlotScreen(
                        onOpenDetail = { overlay = Overlay.Detail(it) },
                        vm = vm,
                    )
                }
            }
        }
        if (overlay == null) {
            RootTabBar(selected = tab, dueCount = dueCount, onTab = { tab = it })
        }
    }
}
