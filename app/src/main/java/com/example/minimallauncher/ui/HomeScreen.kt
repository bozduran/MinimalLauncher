package com.example.minimallauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minimallauncher.R
import com.example.minimallauncher.ui.theme.AppTextStyles
import com.example.minimallauncher.ui.theme.Bg
import com.example.minimallauncher.ui.theme.Dimens
import com.example.minimallauncher.ui.theme.JetBrainsMono
import com.example.minimallauncher.ui.theme.TextPrimary
import com.example.minimallauncher.ui.theme.TextSecondary
import com.example.minimallauncher.ui.theme.TextTertiary
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.util.Locale

@Composable
private fun rememberCurrentTime(): State<LocalDateTime> {
    // Bound to RESUMED: a HOME activity spends most of its life stopped, and an
    // always-running minute ticker kept a coroutine and a recomposition alive for a
    // clock nobody was looking at.
    val lifecycleOwner = LocalLifecycleOwner.current
    val time = remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                time.value = LocalDateTime.now()
                // tick on the minute boundary (we only render HH:mm)
                val delayMs = 60_000L - (System.currentTimeMillis() % 60_000L)
                delay(delayMs)
            }
        }
    }
    return time
}

@Composable
fun HomeScreen(
    vm: LauncherViewModel,
    onOpenSettings: () -> Unit,
    onOpenDrawer: () -> Unit,
) {
    // One snapshot for the whole screen (ARCH-2): favorites, clock mode and the
    // load state can never disagree with each other.
    val state by vm.uiState.collectAsStateCompat()
    val favorites = state.favorites
    val use24h = state.use24h
    val isLoadingApps = state.isLoadingApps
    val appListError = state.appListError
    val now by rememberCurrentTime()

    // Compiled once per locale rather than reallocated on each (per-minute) recomposition.
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val clock = remember(locale) { ClockFormatter(locale) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg),
    ) {
        // The column cannot scroll — the only vertical gesture belongs to the pager
        // that opens the drawer — so the row count is derived from the real height
        // instead of being allowed to overflow off both edges of the screen.
        val visibleFavorites = remember(favorites, maxHeight) {
            HomeFavoritesLayout.visible(favorites, maxHeight)
        }
        val overflow = HomeFavoritesLayout.overflowCount(favorites, maxHeight)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { onOpenSettings() })
                }
                .padding(horizontal = Dimens.ScreenPadding),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = clock.time(now, use24h),
                style = AppTextStyles.Clock,
                color = TextPrimary,
                modifier = Modifier.clickable { vm.openClock() },
            )
            Spacer(Modifier.height(Dimens.SpaceXs))
            Text(
                text = clock.date(now),
                style = AppTextStyles.Date,
                color = TextSecondary,
                modifier = Modifier.clickable { vm.openCalendar() },
            )

            Spacer(Modifier.height(54.dp))

            // Only tell the user they have no favorites once we actually know: the
            // onboarding copy must not appear while the first load is still running.
            if (favorites.isEmpty()) {
                when {
                    isLoadingApps -> Unit
                    appListError != null -> HomeHint(stringResource(R.string.home_load_failed))
                    else -> HomeHint(stringResource(R.string.home_no_favorites))
                }
            } else {
                visibleFavorites.forEach { app ->
                    Text(
                        text = app.label,
                        style = AppTextStyles.Favorite,
                        color = TextPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Dimens.RowCorner))
                            .clickable { vm.launchApp(app) }
                            .padding(vertical = Dimens.RowVertical),
                    )
                }

                // Everything that did not fit stays reachable: the drawer lists every
                // app, so this row is a route to the rest rather than a dead end.
                if (overflow > 0) {
                    Text(
                        text = pluralStringResource(R.plurals.home_more_favorites, overflow, overflow),
                        style = AppTextStyles.ListRow,
                        color = TextSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Dimens.RowCorner))
                            .clickable { onOpenDrawer() }
                            .padding(vertical = Dimens.RowVertical),
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
            Text(
                text = stringResource(R.string.home_open_drawer),
                style = AppTextStyles.Footer,
                color = TextTertiary,
                textAlign = TextAlign.Start,
            )
        }
    }
}

@Composable
private fun HomeHint(text: String) {
    Text(
        text = text,
        style = AppTextStyles.Hint,
        color = TextTertiary,
    )
}
