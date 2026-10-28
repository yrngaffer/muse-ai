package com.muse.companion.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.muse.companion.di.AppContainer
import com.muse.companion.ui.chat.ChatScreen
import com.muse.companion.ui.memory.MemoryScreen
import com.muse.companion.ui.security.SecurityScreen
import com.muse.companion.ui.settings.SettingsScreen

private enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    CHAT("chat", "Chat", Icons.Filled.Chat),
    MEMORY("memory", "Memory", Icons.Filled.Psychology),
    SETTINGS("settings", "Settings", Icons.Filled.Settings),
    SECURITY("security", "Security", Icons.Filled.Shield),
}

@Composable
fun MuseApp(container: AppContainer) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                Dest.entries.forEach { d ->
                    NavigationBarItem(
                        selected = current?.hierarchy?.any { it.route == d.route } == true,
                        onClick = {
                            nav.navigate(d.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(d.icon, contentDescription = d.label) },
                        label = { Text(d.label) },
                    )
                }
            }
        },
    ) { pad ->
        NavHost(
            navController = nav,
            startDestination = Dest.CHAT.route,
            modifier = Modifier.padding(pad),
        ) {
            composable(Dest.CHAT.route) { ChatScreen(container) }
            composable(Dest.MEMORY.route) { MemoryScreen(container) }
            composable(Dest.SETTINGS.route) { SettingsScreen(container) }
            composable(Dest.SECURITY.route) { SecurityScreen(container) }
        }
    }
}
