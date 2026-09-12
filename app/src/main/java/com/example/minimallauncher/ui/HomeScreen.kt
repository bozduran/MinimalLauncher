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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minimallauncher.R
import com.example.minimallauncher.ui.theme.Bg
import com.example.minimallauncher.ui.theme.JetBrainsMono
import com.example.minimallauncher.ui.theme.TextPrimary
import com.example.minimallauncher.ui.theme.TextSecondary
import com.example.minimallauncher.ui.theme.TextTertiary
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.util.Locale

@Composable
private fun rememberCurrentTime(): State<LocalDateTime> =
    produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            // tick on the minute boundary (we only render HH:mm)
            val delayMs = 60_000L - (System.currentTimeMillis() % 60_000L)
            delay(delayMs)
        }
    }

@Composable
fun HomeScreen(
    vm: LauncherViewModel,
    onOpenSettings: () -> Unit,
    onOpenDrawer: () -> Unit,
) {
    val favorites by vm.favorites.collectAsStateCompat()
    val use24h by vm.use24h.collectAsStateCompat()
    val isLoadingApps by vm.isLoadingApps.collectAsStateCompat()
    val appListError by vm.appListError.collectAsStateCompat()
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
                .padding(horizontal = 30.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = clock.time(now, use24h),
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Light,
                fontSize = 66.sp,
                lineHeight = 70.sp,
                color = TextPrimary,
                modifier = Modifier.clickable { vm.openClock() },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = clock.date(now),
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
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
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Normal,
                        fontSize = 23.sp,
                        color = TextPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { vm.launchApp(app) }
                            .padding(vertical = 11.dp),
                    )
                }

                // Everything that did not fit stays reachable: the drawer lists every
                // app, so this row is a route to the rest rather than a dead end.
                if (overflow > 0) {
                    Text(
                        text = pluralStringResource(R.plurals.home_more_favorites, overflow, overflow),
                        fontFamily = JetBrainsMono,
                        fontSize = 18.sp,
                        color = TextSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenDrawer() }
                            .padding(vertical = 11.dp),
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
            Text(
                text = stringResource(R.string.home_open_drawer),
                fontFamily = JetBrainsMono,
                fontSize = 12.sp,
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
        fontFamily = JetBrainsMono,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        color = TextTertiary,
    )
}
