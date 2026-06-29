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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.minimallauncher.data.AppInfo
import com.example.minimallauncher.data.AppLauncher
import com.example.minimallauncher.ui.theme.Accent
import com.example.minimallauncher.ui.theme.Bg
import com.example.minimallauncher.ui.theme.BorderCol
import com.example.minimallauncher.ui.theme.JetBrainsMono
import com.example.minimallauncher.ui.theme.SurfaceCol
import com.example.minimallauncher.ui.theme.TextPrimary
import com.example.minimallauncher.ui.theme.TextSecondary
import com.example.minimallauncher.ui.theme.TextTertiary
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DrawerScreen(vm: LauncherViewModel) {
    val context = LocalContext.current
    val apps by vm.drawerApps.collectAsStateCompat()
    val query by vm.query.collectAsStateCompat()
    val favorites by vm.favoriteSet.collectAsStateCompat()
    var menuApp by remember { mutableStateOf<AppInfo?>(null) }

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
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp),
        )
        HorizontalDivider(color = BorderCol, thickness = 1.dp)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 6.dp),
        ) {
            items(apps, key = { it.key }) { app ->
                Text(
                    text = app.label,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Normal,
                    fontSize = 20.sp,
                    color = TextPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { AppLauncher.launch(context, app) },
                            onLongClick = { menuApp = app },
                        )
                        .padding(horizontal = 30.dp, vertical = 13.dp),
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
            onInfo = { AppLauncher.openAppInfo(context, app.packageName); menuApp = null },
            onUninstall = { AppLauncher.uninstall(context, app.packageName); menuApp = null },
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
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceCol)
                .border(1.dp, BorderCol, RoundedCornerShape(16.dp))
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = app.label,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
            )
            HorizontalDivider(color = BorderCol, thickness = 1.dp)
            MenuRow(if (isFavorite) "remove from favorites" else "add to favorites", onToggleFavorite)
            MenuRow("hide app", onHide)
            MenuRow("app info", onInfo)
            MenuRow("uninstall", onUninstall, color = Accent)
        }
    }
}

@Composable
private fun MenuRow(text: String, onClick: () -> Unit, color: androidx.compose.ui.graphics.Color = TextPrimary) {
    Text(
        text = text,
        fontFamily = JetBrainsMono,
        fontSize = 16.sp,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 22.dp, vertical = 14.dp),
    )
}
