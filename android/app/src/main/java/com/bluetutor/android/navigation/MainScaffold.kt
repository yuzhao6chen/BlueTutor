package com.bluetutor.android.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bluetutor.android.ui.theme.BluetutorElevation

@Composable
fun MainScaffold() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colorScheme.background,
        bottomBar = {
            NavigationBar(
                modifier = Modifier.height(60.dp),
                containerColor = colorScheme.surface,
                tonalElevation = BluetutorElevation.low,
                windowInsets = WindowInsets(0, 0, 0, 0),
            ) {
                MainTab.entries.forEach { tab ->
                    val selected = currentRoute == tab.destination.route

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            androidx.compose.material3.Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = colorScheme.primary,
                            selectedTextColor = colorScheme.primary,
                            unselectedIconColor = colorScheme.onSurfaceVariant,
                            unselectedTextColor = colorScheme.onSurfaceVariant,
                            indicatorColor = colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        BluetutorNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}