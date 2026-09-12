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
import androidx.compose.foundation.layout.sizeIn
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
import com.example.minimallauncher.ui.theme.AppTextStyles
import com.example.minimallauncher.ui.theme.AppThemes
import com.example.minimallauncher.ui.theme.Dimens
import com.example.minimallauncher.ui.theme.Bg
import com.example.minimallauncher.ui.theme.BorderCol
import com.example.minimallauncher.ui.theme.BorderMid
import com.example.minimallauncher.ui.theme.JetBrainsMono
import com.example.minimallauncher.ui.theme.TextPrimary
import com.example.minimallauncher.ui.theme.TextSecondary
import com.example.minimallauncher.ui.theme.TextTertiary

@Composable
fun SettingsScreen(vm: LauncherViewModel, onBack: () -> Unit) {
    val state by vm.uiState.collectAsStateCompat()
    val apps = state.apps
    val favorites = state.favoritePackages
    val hidden = state.hiddenPackages
    val use24h = state.use24h
    val themeKey = state.themeKey

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .systemBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.TitlePadding, vertical = Dimens.TitleVertical),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.back_symbol),
                style = AppTextStyles.BackSymbol,
                color = TextPrimary,
                modifier = Modifier
                    .sizeIn(minWidth = Dimens.MinTouchTarget, minHeight = Dimens.MinTouchTarget)
                    .clickable { onBack() },
            )
            Spacer(Modifier.width(18.dp))
            Text(
                text = stringResource(R.string.settings_title),
                style = AppTextStyles.ScreenTitle,
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
                        .padding(horizontal = Dimens.RowPadding, vertical = Dimens.DrawerRowVertical),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (isFav) stringResource(R.string.favorite_on_symbol)
                        else stringResource(R.string.favorite_off_symbol),
                        style = AppTextStyles.ListRow,
                        color = if (isFav) Accent else TextTertiary,
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = app.label,
                        style = AppTextStyles.ListRow,
                        color = if (isHidden) TextTertiary else TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    if (isHidden) {
                        Text(
                            text = stringResource(R.string.settings_unhide),
                            style = AppTextStyles.SectionLabel,
                            color = TextSecondary,
                            modifier = Modifier
                                .sizeIn(
                                    minWidth = Dimens.MinTouchTarget,
                                    minHeight = Dimens.MinTouchTarget,
                                )
                                .clickable { vm.toggleHidden(app.packageName) },
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
        contentPadding = PaddingValues(horizontal = Dimens.RowPadding, vertical = 4.dp),
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
                        .clip(RoundedCornerShape(Dimens.SwatchCorner))
                        .background(opt.palette.bg)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) opt.palette.accent else opt.palette.borderMid,
                            shape = RoundedCornerShape(Dimens.SwatchCorner),
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
                    style = AppTextStyles.ThemeLabel,
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
        style = AppTextStyles.SectionLabel,
        color = TextTertiary,
        modifier = Modifier.padding(
            start = Dimens.RowPadding,
            end = Dimens.RowPadding,
            top = Dimens.SpaceLg,
            bottom = Dimens.SpaceXs,
        ),
    )
}

@Composable
private fun ActionRow(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = AppTextStyles.ListRow,
        color = TextPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = Dimens.RowPadding, vertical = Dimens.SpaceMd),
    )
}

@Composable
private fun ToggleRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = Dimens.RowPadding, vertical = Dimens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = AppTextStyles.ListRow,
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