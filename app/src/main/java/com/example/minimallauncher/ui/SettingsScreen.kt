package com.example.minimallauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minimallauncher.R
import com.example.minimallauncher.ui.theme.Accent
import com.example.minimallauncher.ui.theme.AccentRing
import com.example.minimallauncher.ui.theme.AppThemes
import com.example.minimallauncher.ui.theme.Bg
import com.example.minimallauncher.ui.theme.BorderCol
import com.example.minimallauncher.ui.theme.BorderMid
import com.example.minimallauncher.ui.theme.JetBrainsMono
import com.example.minimallauncher.ui.theme.TextPrimary
import com.example.minimallauncher.ui.theme.TextSecondary
import com.example.minimallauncher.ui.theme.TextTertiary

@Composable
fun SettingsScreen(vm: LauncherViewModel, onBack: () -> Unit) {
    val apps by vm.allApps.collectAsStateCompat()
    val favorites by vm.favoriteSet.collectAsStateCompat()
    val hidden by vm.hiddenSet.collectAsStateCompat()
    val use24h by vm.use24h.collectAsStateCompat()
    val themeKey by vm.themeKey.collectAsStateCompat()

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
                text = stringResource(R.string.back_symbol),
                fontFamily = JetBrainsMono,
                fontSize = 30.sp,
                color = TextPrimary,
                modifier = Modifier.clickable { onBack() },
            )
            Spacer(Modifier.width(18.dp))
            Text(
                text = stringResource(R.string.settings_title),
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
                color = TextPrimary,
            )
        }
        HorizontalDivider(color = BorderCol, thickness = 1.dp)

        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            item {
                ToggleRow(stringResource(R.string.settings_24h), use24h) { vm.setUse24h(it) }
                ActionRow(stringResource(R.string.settings_set_default_launcher)) { vm.openHomeSettings() }
                SectionLabel(stringResource(R.string.settings_section_theme))
            }
            item {
                ThemePicker(current = themeKey, onSelect = { vm.setTheme(it) })
            }
            item {
                SectionLabel(stringResource(R.string.settings_section_favorites))
            }
            items(apps, key = { it.packageName }) { app ->
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
                        text = if (isFav) stringResource(R.string.favorite_on_symbol)
                        else stringResource(R.string.favorite_off_symbol),
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
                            text = stringResource(R.string.settings_unhide),
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
private fun ThemePicker(current: String, onSelect: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(AppThemes, key = { it.key }) { opt ->
            val selected = opt.key == current
            Column(
                modifier = Modifier
                    .width(98.dp)
                    .clickable { onSelect(opt.key) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(opt.palette.bg)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) opt.palette.accent else opt.palette.borderMid,
                            shape = RoundedCornerShape(10.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Dot(opt.palette.accent)
                        Dot(opt.palette.accentSoft)
                        Dot(opt.palette.text)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = opt.label,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = if (selected) Accent else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        modifier = Modifier
            .size(11.dp)
            .clip(CircleShape)
            .background(color),
    )
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