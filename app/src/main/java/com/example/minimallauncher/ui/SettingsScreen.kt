package com.example.minimallauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minimallauncher.data.AppLauncher
import com.example.minimallauncher.ui.theme.Accent
import com.example.minimallauncher.ui.theme.AccentRing
import com.example.minimallauncher.ui.theme.Bg
import com.example.minimallauncher.ui.theme.BorderCol
import com.example.minimallauncher.ui.theme.BorderMid
import com.example.minimallauncher.ui.theme.JetBrainsMono
import com.example.minimallauncher.ui.theme.TextPrimary
import com.example.minimallauncher.ui.theme.TextSecondary
import com.example.minimallauncher.ui.theme.TextTertiary

@Composable
fun SettingsScreen(vm: LauncherViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val apps by vm.allApps.collectAsStateCompat()
    val favorites by vm.favoriteSet.collectAsStateCompat()
    val hidden by vm.hiddenSet.collectAsStateCompat()
    val use24h by vm.use24h.collectAsStateCompat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .systemBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "‹",
                fontFamily = JetBrainsMono,
                fontSize = 30.sp,
                color = TextPrimary,
                modifier = Modifier.clickable { onBack() },
            )
            Spacer(Modifier.width(18.dp))
            Text(
                text = "settings",
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
                color = TextPrimary,
            )
        }
        HorizontalDivider(color = BorderCol, thickness = 1.dp)

        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            item {
                ToggleRow("24-hour clock", use24h) { vm.setUse24h(it) }
                ActionRow("set as default launcher") { AppLauncher.openHomeSettings(context) }
                SectionLabel("favorites — tap to toggle")
            }
            items(apps, key = { it.key }) { app ->
                val isFav = app.packageName in favorites
                val isHidden = app.packageName in hidden
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.toggleFavorite(app.packageName) }
                        .padding(horizontal = 28.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (isFav) "★" else "☆",
                        fontFamily = JetBrainsMono,
                        fontSize = 18.sp,
                        color = if (isFav) Accent else TextTertiary,
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = app.label,
                        fontFamily = JetBrainsMono,
                        fontSize = 18.sp,
                        color = if (isHidden) TextTertiary else TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    if (isHidden) {
                        Text(
                            text = "unhide",
                            fontFamily = JetBrainsMono,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            modifier = Modifier.clickable { vm.toggleHidden(app.packageName) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontFamily = JetBrainsMono,
        fontSize = 13.sp,
        color = TextTertiary,
        modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 22.dp, bottom = 6.dp),
    )
}

@Composable
private fun ActionRow(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        fontFamily = JetBrainsMono,
        fontSize = 18.sp,
        color = TextPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 28.dp, vertical = 16.dp),
    )
}

@Composable
private fun ToggleRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 28.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            fontFamily = JetBrainsMono,
            fontSize = 18.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Bg,
                checkedTrackColor = Accent,
                uncheckedThumbColor = TextTertiary,
                uncheckedTrackColor = AccentRing,
                uncheckedBorderColor = BorderMid,
            ),
        )
    }
}
