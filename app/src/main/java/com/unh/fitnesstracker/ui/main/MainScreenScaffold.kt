package com.unh.fitnesstracker.ui.main

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** How the top-left button of a screen behaves. */
sealed interface TopBarNav {
    /** ☰ on the bottom-tab screens; opens the drawer (S6). */
    data class Menu(val onClick: () -> Unit) : TopBarNav

    /** ← on screens opened from the drawer or a button. */
    data class Back(val onClick: () -> Unit) : TopBarNav
}

/**
 * Scaffold for every screen inside [MainScreen]. The outer MainScreen scaffold already handles the
 * bottom bar and system-bar insets, so this one sets contentWindowInsets to zero to avoid double padding.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenScaffold(
    title: String,
    nav: TopBarNav,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    when (nav) {
                        is TopBarNav.Menu -> IconButton(onClick = nav.onClick) {
                            Icon(Icons.Filled.Menu, contentDescription = "Open menu")
                        }
                        is TopBarNav.Back -> IconButton(onClick = nav.onClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = actions,
            )
        },
        floatingActionButton = floatingActionButton,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0),
        content = content,
    )
}
