package com.tks.videophotobookv3

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.tks.videophotobookv3.ui.ar.ArViewScreen
import com.tks.videophotobookv3.ui.main.MainScreen

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Main)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Main> {
                MainScreen(
                    onItemClick = { navKey -> backStack.add(navKey) },
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                )
            }
            entry<ArView> {
                ArViewScreen(
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    )
}
