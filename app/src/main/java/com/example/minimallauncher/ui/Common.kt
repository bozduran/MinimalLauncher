package com.example.minimallauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.example.minimallauncher.R
import com.example.minimallauncher.ui.theme.Accent
import com.example.minimallauncher.ui.theme.AppTextStyles
import com.example.minimallauncher.ui.theme.JetBrainsMono
import com.example.minimallauncher.ui.theme.TextPrimary
import com.example.minimallauncher.ui.theme.TextSecondary
import com.example.minimallauncher.ui.theme.TextTertiary
import kotlinx.coroutines.flow.StateFlow

/** Convenience wrapper so screens can collect a StateFlow without extra imports. */
@Composable
fun <T> StateFlow<T>.collectAsStateCompat(): State<T> = collectAsState()

@Composable
fun SearchField(
    query: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onSearch: () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = AppTextStyles.DrawerItem.copy(color = TextPrimary),
                cursorBrush = SolidColor(Accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
            )
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.search_hint),
                    style = AppTextStyles.DrawerItem,
                    color = TextTertiary,
                )
            }
        }
        if (query.isNotEmpty()) {
            Text(
                text = stringResource(R.string.search_clear_symbol),
                style = AppTextStyles.SearchClear,
                color = TextSecondary,
                modifier = Modifier
                    .clickable { onChange("") }
                    .padding(start = 10.dp),
            )
        }
    }
}