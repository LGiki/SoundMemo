package net.lgiki.soundmemo.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal enum class SettingsItemPosition { Alone, Top, Middle, Bottom }

internal fun settingsItemPosition(index: Int, total: Int): SettingsItemPosition = when {
    total <= 1 -> SettingsItemPosition.Alone
    index == 0 -> SettingsItemPosition.Top
    index == total - 1 -> SettingsItemPosition.Bottom
    else -> SettingsItemPosition.Middle
}

@Composable
internal fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 10.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        content()
    }
}

@Composable
internal fun SettingsItemCard(
    modifier: Modifier = Modifier,
    position: SettingsItemPosition = SettingsItemPosition.Alone,
    content: @Composable ColumnScope.() -> Unit,
) {
    val outerRadius = 16.dp
    val innerRadius = 4.dp
    val shape = when (position) {
        SettingsItemPosition.Alone -> RoundedCornerShape(outerRadius)
        SettingsItemPosition.Top -> RoundedCornerShape(
            topStart = outerRadius,
            topEnd = outerRadius,
            bottomStart = innerRadius,
            bottomEnd = innerRadius,
        )
        SettingsItemPosition.Middle -> RoundedCornerShape(innerRadius)
        SettingsItemPosition.Bottom -> RoundedCornerShape(
            topStart = innerRadius,
            topEnd = innerRadius,
            bottomStart = outerRadius,
            bottomEnd = outerRadius,
        )
    }
    val itemSpacing = when (position) {
        SettingsItemPosition.Alone -> Modifier
        SettingsItemPosition.Top -> Modifier.padding(bottom = 1.dp)
        SettingsItemPosition.Middle -> Modifier.padding(vertical = 1.dp)
        SettingsItemPosition.Bottom -> Modifier.padding(top = 1.dp)
    }
    Card(
        modifier = modifier.then(itemSpacing).fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(content = content)
    }
}
