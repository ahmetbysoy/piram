package com.example.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.theme.BgDark
import com.example.core.theme.BorderDark
import com.example.core.theme.PinkPastel
import com.example.core.theme.PurplePastel
import com.example.core.theme.SurfaceDark
import com.example.core.theme.TextMuted
import com.example.core.theme.TextPrimary
import com.example.presentation.screens.pyramid.PyramidScreen
import com.example.presentation.screens.pyramid.PyramidViewModel
import com.example.presentation.screens.radar.RadarScreen
import com.example.presentation.screens.radar.RadarViewModel
import com.example.presentation.screens.settings.SettingsScreen
import com.example.presentation.screens.settings.SettingsViewModel
import com.example.presentation.screens.strategies.StrategiesScreen
import com.example.presentation.screens.strategies.StrategiesViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Pyramid : Screen("pyramid", "Pyramid", Icons.Default.ChangeHistory)
    object Strategies : Screen("strategies", "Strategies", Icons.Default.Insights)
    object Radar : Screen("radar", "Radar", Icons.Default.Radar)
    object Settings : Screen("settings", "Settings", Icons.Default.Tune)
}

@Composable
fun AppNavigation(
    pyramidViewModel: PyramidViewModel,
    navController: NavHostController = rememberNavController()
) {
    val items = listOf(
        Screen.Pyramid,
        Screen.Strategies,
        Screen.Radar,
        Screen.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Pyramid.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                items.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selected = selected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BgDark,
                            selectedTextColor = PurplePastel,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = PurplePastel
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Pyramid.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Pyramid.route) {
                PyramidScreen(viewModel = pyramidViewModel)
            }
            composable(Screen.Strategies.route) {
                val strategiesViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
                    StrategiesViewModel(
                        application = pyramidViewModel.getApplication(),
                        pyramidViewModel = pyramidViewModel
                    )
                }
                StrategiesScreen(viewModel = strategiesViewModel)
            }
            composable(Screen.Radar.route) {
                val radarViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
                    RadarViewModel(
                        application = pyramidViewModel.getApplication(),
                        repository = pyramidViewModel.repository,
                        preferencesRepository = pyramidViewModel.preferencesRepository,
                        symbolRegistry = pyramidViewModel.symbolRegistry
                    )
                }
                RadarScreen(viewModel = radarViewModel)
            }
            composable(Screen.Settings.route) {
                val settingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
                    SettingsViewModel(
                        application = pyramidViewModel.getApplication(),
                        preferencesRepository = pyramidViewModel.preferencesRepository,
                        marketDataRepository = pyramidViewModel.repository,
                        symbolRegistry = pyramidViewModel.symbolRegistry
                    )
                }
                SettingsScreen(viewModel = settingsViewModel)
            }
        }
    }
}
