package com.example.minimallauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.example.minimallauncher.ui.theme.Bg
import com.example.minimallauncher.ui.theme.JetBrainsMono
import com.example.minimallauncher.ui.theme.TextPrimary
import com.example.minimallauncher.ui.theme.TextSecondary
import com.example.minimallauncher.ui.theme.TextTertiary
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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
) {
    val favorites by vm.favorites.collectAsStateCompat()
    val use24h by vm.use24h.collectAsStateCompat()
    val isLoadingApps by vm.isLoadingApps.collectAsStateCompat()
    val appListError by vm.appListError.collectAsStateCompat()
    val now by rememberCurrentTime()

    val timePattern = if (use24h) "HH:mm" else "h:mm"
    val timeFmt = DateTimeFormatter.ofPattern(timePattern, Locale.getDefault())
    val dateFmt = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .systemBarsPadding()
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onOpenSettings() })
            }
            .padding(horizontal = 30.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = now.format(timeFmt),
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Light,
            fontSize = 66.sp,
            lineHeight = 70.sp,
            color = TextPrimary,
            modifier = Modifier.clickable { vm.openClock() },
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = now.format(dateFmt).replaceFirstChar { it.titlecase(Locale.getDefault()) },
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
                appListError != null -> Text(
                    text = "couldn't load your apps\nlong-press anywhere to open settings",
                    fontFamily = JetBrainsMono,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = TextTertiary,
                )
                else -> Text(
                    text = "no favorites yet\nlong-press anywhere to open settings",
                    fontFamily = JetBrainsMono,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = TextTertiary,
                )
            }
        } else {
            favorites.forEach { app ->
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
        }

        Spacer(Modifier.height(40.dp))
        Text(
            text = "↑ apps",
            fontFamily = JetBrainsMono,
            fontSize = 12.sp,
            color = TextTertiary,
            textAlign = TextAlign.Start,
        )
    }
}
