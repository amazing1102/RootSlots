package com.cet4.rootslots

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import com.cet4.rootslots.ui.duel.DuelScreen
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
            val skin by vm.skin.collectAsState()
            RootSlotsTheme(themeMode, skin) {
                AppNav(vm)
            }
        }
    }
}

/** 全屏推入页:盖在 Tab 内容上,无底部栏,←/系统返回键关闭 */
private sealed class Overlay {
    data class Detail(val word: String) : Overlay()
    data object Review : Overlay()
    data object Duel : Overlay()
    data class Nemesis(val word: String) : Overlay()
    data object Wish : Overlay()
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
            // 推入页转场:推入时从底部上滑盖住旧页,关闭时下滑退场
            AnimatedContent(
                targetState = overlay,
                transitionSpec = {
                    if (targetState != null) {
                        (slideInVertically(tween(300, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(220)))
                            .togetherWith(ExitTransition.None)
                    } else {
                        fadeIn(tween(240))
                            .togetherWith(
                                slideOutVertically(tween(300, easing = FastOutSlowInEasing)) { it } +
                                    fadeOut(tween(240))
                            )
                    }
                },
                label = "overlay",
            ) { o ->
                when (o) {
                    is Overlay.Detail -> DetailScreen(
                        word = o.word, vm = vm,
                        onBack = { overlay = null },
                        onOpenWord = { overlay = Overlay.Detail(it) },
                    )
                    Overlay.Review -> ReviewScreen(vm = vm, onBack = { overlay = null })
                    Overlay.Duel -> DuelScreen(vm = vm, onBack = { overlay = null })
                    Overlay.Wish -> com.cet4.rootslots.ui.slots.WishScreen(vm = vm, onBack = { overlay = null })
                    is Overlay.Nemesis -> {
                        // 宿敌从通缉令点入;加载期间显示空白(不能立即关推入页,
                        // 否则首帧 null 会把推入秒关,表现为"点击无反应")
                        var n by remember(o.word) { mutableStateOf<com.cet4.rootslots.data.Repository.Nemesis?>(null) }
                        var missing by remember(o.word) { mutableStateOf(false) }
                        LaunchedEffect(o.word) {
                            val loaded = vm.nemesisOf(o.word)
                            if (loaded == null) missing = true else n = loaded
                        }
                        LaunchedEffect(missing) { if (missing) overlay = null }
                        val nem = n
                        if (nem != null) {
                            com.cet4.rootslots.ui.duel.NemesisScreen(vm = vm, nemesis = nem, onBack = { overlay = null })
                        }
                    }
                    null -> TabContent(vm, tab,
                        onOpenDetail = { overlay = Overlay.Detail(it) },
                        onStartReview = { overlay = Overlay.Review },
                        onOpenDuel = { overlay = Overlay.Duel },
                        onOpenNemesis = { overlay = Overlay.Nemesis(it) },
                        onOpenWish = { overlay = Overlay.Wish },
                    )
                }
            }
        }
        // Tab 栏随推入页收起/展开
        AnimatedVisibility(
            visible = overlay == null,
            enter = slideInVertically(tween(260, easing = FastOutSlowInEasing)) { it } + fadeIn(),
            exit = slideOutVertically(tween(200, easing = FastOutSlowInEasing)) { it } + fadeOut(),
        ) {
            RootTabBar(selected = tab, dueCount = dueCount, onTab = { tab = it })
        }
    }
}

/** Tab 内容切换:淡入 + 轻微上滑,替代生硬的瞬切 */
@Composable
private fun TabContent(
    vm: SlotViewModel,
    tab: RootTab,
    onOpenDetail: (String) -> Unit,
    onStartReview: () -> Unit,
    onOpenDuel: () -> Unit,
    onOpenNemesis: (String) -> Unit,
    onOpenWish: () -> Unit,
) {
    AnimatedContent(
        targetState = tab,
        transitionSpec = {
            (fadeIn(tween(220)) + slideInVertically(tween(240)) { it / 30 })
                .togetherWith(fadeOut(tween(120)))
        },
        label = "tabContent",
    ) { t ->
        when (t) {
            RootTab.Codex -> CodexScreen(vm = vm, onOpenWord = onOpenDetail)
            RootTab.Favorites -> FavoritesScreen(vm = vm, onOpenWord = onOpenDetail, onStartReview = onStartReview, onOpenNemesis = onOpenNemesis)
            RootTab.Mine -> MineScreen(vm = vm)
            RootTab.Quiz -> QuizScreen(vm = vm, onOpenDuel = onOpenDuel)
            RootTab.Slots -> SlotScreen(onOpenDetail = onOpenDetail, vm = vm, onOpenWish = onOpenWish)
        }
    }
}
