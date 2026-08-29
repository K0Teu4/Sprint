package ru.sprint.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ru.sprint.app.ui.screens.day.DayScreen
import ru.sprint.app.ui.screens.month.MonthScreen
import ru.sprint.app.ui.screens.settings.SettingsScreen
import ru.sprint.app.ui.screens.stats.StatsScreen

sealed class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Month : Tab("month", "Месяц", Icons.Filled.DateRange)
    data object Day : Tab("day", "День", Icons.AutoMirrored.Filled.List)
    data object Stats : Tab("stats", "Итоги", Icons.Filled.Star)
    data object Settings : Tab("settings", "Настройки", Icons.Filled.Settings)
}

@Composable
fun SprintNavGraph() {
    val navController = rememberNavController()
    val tabs = listOf(Tab.Month, Tab.Day, Tab.Stats, Tab.Settings)

    Scaffold(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                tonalElevation = 0.dp,
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
            ) {
                val entry by navController.currentBackStackEntryAsState()
                val current = entry?.destination?.route
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Month.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Tab.Month.route) { MonthScreen() }
            composable(Tab.Day.route) { DayScreen() }
            composable(Tab.Stats.route) { StatsScreen() }
            composable(Tab.Settings.route) { SettingsScreen() }
        }
    }
}
