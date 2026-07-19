package net.lgiki.soundmemo.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundMemoScaffold(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets = WindowInsets.safeDrawing,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val useCompactAppBar = maxHeight < 600.dp
        val scrollBehavior = if (useCompactAppBar) {
            TopAppBarDefaults.pinnedScrollBehavior()
        } else {
            TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        }
        val appBarColors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        )
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = contentWindowInsets,
            topBar = {
                if (useCompactAppBar) {
                    TopAppBar(
                        title = title,
                        navigationIcon = navigationIcon,
                        actions = actions,
                        colors = appBarColors,
                        scrollBehavior = scrollBehavior,
                    )
                } else {
                    LargeTopAppBar(
                        title = title,
                        navigationIcon = navigationIcon,
                        actions = actions,
                        colors = appBarColors,
                        scrollBehavior = scrollBehavior,
                    )
                }
            },
            bottomBar = bottomBar,
            snackbarHost = snackbarHost,
            content = content,
        )
    }
}
