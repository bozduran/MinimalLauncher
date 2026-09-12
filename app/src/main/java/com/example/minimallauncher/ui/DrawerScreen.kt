package com.example.minimallauncher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.minimallauncher.R
import com.example.minimallauncher.data.AppInfo
import com.example.minimallauncher.ui.theme.Accent
import com.example.minimallauncher.ui.theme.AppTextStyles
import com.example.minimallauncher.ui.theme.Bg
import com.example.minimallauncher.ui.theme.Dimens
import com.example.minimallauncher.ui.theme.BorderCol
import com.example.minimallauncher.ui.theme.JetBrainsMono
import com.example.minimallauncher.ui.theme.SurfaceCol
import com.example.minimallauncher.ui.theme.TextPrimary
import com.example.minimallauncher.ui.theme.TextSecondary

private const val FOCUS_ATTEMPTS = 2

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DrawerScreen(vm: LauncherViewModel, active: Boolean) {
    val apps by vm.drawerApps.collectAsStateCompat()
    val query by vm.query.collectAsStateCompat()
    val favorites by vm.favoriteSet.collectAsStateCompat()
    var menuApp by remember { mutableStateOf<AppInfo?>(null) }

    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // Returning to the home screen must not leave the app menu (a separate Dialog
    // window) on screen over it.
    LaunchedEffect(active) {
        if (!active) menuApp = null
    }

    // Auto-focus the search box when the drawer becomes the active page.
    //
    // Waits for a frame rather than sleeping: a fixed delay was a guess about frame
    // timing, so on a slow first composition the field might not be attached yet and
    // the failure was swallowed by runCatching (the keyboard opened over an
    // unfocused field, with no log and no retry).
    LaunchedEffect(active) {
        if (!active) {
            keyboard?.hide()
            return@LaunchedEffect
        }
        // Two attempts, each preceded by a frame, then report rather than swallow.
        repeat(FOCUS_ATTEMPTS) { attempt ->
            withFrameNanos { }
            val result = runCatching { focusRequester.requestFocus() }
            if (result.isSuccess) return@LaunchedEffect
            if (attempt == FOCUS_ATTEMPTS - 1) {
                vm.onSearchFocusFailed(result.exceptionOrNull() ?: IllegalStateException("no focus target"))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .systemBarsPadding()
            .imePadding(),
    ) {
        SearchField(
            query = query,
            onChange = vm::setQuery,
            modifier = Modifier.padding(horizontal = Dimens.RowPadding, vertical = Dimens.MenuRowVertical),
            focusRequester = focusRequester,
            onSearch = { vm.submitSearch() },
        )
        HorizontalDivider(color = BorderCol, thickness = 1.dp)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 6.dp),
        ) {
            items(apps, key = { it.packageName }) { app ->
                Text(
                    text = app.label,
                    style = AppTextStyles.DrawerItem,
                    color = TextPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { vm.launchApp(app) },
                            onLongClick = { menuApp = app },
                        )
                        .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.DrawerRowVertical),
                )
            }
        }
    }

    menuApp?.let { app ->
        AppMenu(
            app = app,
            isFavorite = app.packageName in favorites,
            onToggleFavorite = { vm.toggleFavorite(app.packageName); menuApp = null },
            onHide = { vm.toggleHidden(app.packageName); menuApp = null },
            onInfo = { vm.openAppInfo(app.packageName); menuApp = null },
            onUninstall = { vm.uninstall(app.packageName); menuApp = null },
            onDismiss = { menuApp = null },
        )
    }
}

@Composable
private fun AppMenu(
    app: AppInfo,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onHide: () -> Unit,
    onInfo: () -> Unit,
    onUninstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(min = 250.dp)
                .clip(RoundedCornerShape(Dimens.PanelCorner))
                .background(SurfaceCol)
                .border(1.dp, BorderCol, RoundedCornerShape(Dimens.PanelCorner))
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = app.label,
                style = AppTextStyles.MenuHeader,
                color = TextSecondary,
                modifier = Modifier.padding(
                    horizontal = Dimens.MenuPadding,
                    vertical = Dimens.MenuHeaderVertical,
                ),
            )
            HorizontalDivider(color = BorderCol, thickness = 1.dp)
            MenuRow(
                if (isFavorite) stringResource(R.string.menu_remove_favorite)
                else stringResource(R.string.menu_add_favorite),
                onToggleFavorite,
            )
            MenuRow(stringResource(R.string.menu_hide_app), onHide)
            MenuRow(stringResource(R.string.menu_app_info), onInfo)
            MenuRow(stringResource(R.string.menu_uninstall), onUninstall, color = Accent)
        }
    }
}

@Composable
private fun MenuRow(text: String, onClick: () -> Unit, color: androidx.compose.ui.graphics.Color = TextPrimary) {
    Text(
        text = text,
        style = AppTextStyles.MenuRow,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = Dimens.MenuPadding, vertical = Dimens.MenuRowVertical),
    )
}