package com.example.minimallauncher.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import com.example.minimallauncher.ui.theme.Bg
import com.example.minimallauncher.ui.theme.MinimalLauncherTheme
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.luminance
import com.example.minimallauncher.ui.theme.paletteFor
class MainActivity : ComponentActivity() {

    private val vm: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeKey by vm.themeKey.collectAsState()
            val palette = remember(themeKey) { paletteFor(themeKey) }

            // Keep system-bar icons readable on both light and dark themes.
            LaunchedEffect(palette) {
                val transparent = android.graphics.Color.TRANSPARENT
                val style = if (palette.bg.luminance() < 0.5f) {
                    SystemBarStyle.dark(transparent)
                } else {
                    SystemBarStyle.light(transparent, transparent)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            MinimalLauncherTheme(palette = palette) {
                LauncherRoot(vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Catch installs/uninstalls that happened while we were away.
        vm.refresh()
    }

    // Pressing Home while the launcher is foreground re-delivers the intent here.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        vm.onHomePressed()
    }
}

@Composable
private fun LauncherRoot(vm: LauncherViewModel) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    val focusManager = LocalFocusManager.current

    // Home button -> back to page 0 and close settings.
    LaunchedEffect(Unit) {
        vm.goHome.collect {
            showSettings = false
            pagerState.animateScrollToPage(0)
        }
    }

    // Back gesture handling.
    BackHandler(enabled = showSettings) { showSettings = false }
    BackHandler(enabled = !showSettings && pagerState.currentPage == 1) {
        scope.launch { pagerState.animateScrollToPage(0) }
    }

    if (showSettings) {
        SettingsScreen(vm = vm, onBack = { showSettings = false })
    } else {
        VerticalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .background(Bg),
        ) { page ->
            when (page) {
                0 -> HomeScreen(vm = vm, onOpenSettings = { showSettings = true })
                else -> DrawerScreen(vm = vm)
            }
        }
    }
}
