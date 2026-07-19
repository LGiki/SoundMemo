package net.lgiki.soundmemo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AdaptiveContent(
    padding: PaddingValues,
    modifier: Modifier = Modifier,
    maxContentWidth: Dp = 1_200.dp,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.TopCenter,
    ) {
        val horizontalPadding = if (maxWidth < 600.dp) 20.dp else 32.dp
        Box(
            modifier = Modifier
                .widthIn(max = maxContentWidth)
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = horizontalPadding, vertical = 12.dp),
        ) {
            content()
        }
    }
}
