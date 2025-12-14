package com.example.amap_sim.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.amap_sim.domain.model.LatLng
import com.example.amap_sim.ui.screen.home.HomeScreen
import com.example.amap_sim.ui.screen.splash.SplashScreen

/**
 * 应用导航图
 */
@Composable
fun AmapNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // 启动页
        composable(
            route = Screen.Splash.route,
            exitTransition = {
                fadeOut(animationSpec = tween(300))
            }
        ) {
            SplashScreen(
                onInitComplete = {
                    // 初始化完成，导航到主页（清除启动页）
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onInitError = { error ->
                    // 初始化失败，仍然进入主页（会显示错误状态）
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        
        // 主页
        composable(
            route = Screen.Home.route,
            enterTransition = {
                fadeIn(animationSpec = tween(300))
            }
        ) {
            HomeScreen(
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                },
                onNavigateToRoute = { start, end ->
                    navController.navigate(
                        Screen.Route.createRoute(start.lat, start.lon, end.lat, end.lon)
                    )
                },
                onNavigateToPoiDetail = { poiId ->
                    navController.navigate(Screen.PoiDetail.createRoute(poiId))
                }
            )
        }
        
        // 搜索页
        composable(
            route = Screen.Search.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(300)
                )
            }
        ) {
            // TODO: SearchScreen
            // SearchScreen(
            //     onNavigateBack = { navController.popBackStack() },
            //     onPoiSelected = { poi ->
            //         navController.previousBackStackEntry?.savedStateHandle?.set("selectedPoi", poi)
            //         navController.popBackStack()
            //     }
            // )
        }
        
        // 路线规划页
        composable(
            route = Screen.Route.route,
            arguments = listOf(
                navArgument(Screen.ARG_START_LAT) { type = NavType.FloatType },
                navArgument(Screen.ARG_START_LON) { type = NavType.FloatType },
                navArgument(Screen.ARG_END_LAT) { type = NavType.FloatType },
                navArgument(Screen.ARG_END_LON) { type = NavType.FloatType }
            ),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) { backStackEntry ->
            val startLat = backStackEntry.arguments?.getFloat(Screen.ARG_START_LAT)?.toDouble() ?: 0.0
            val startLon = backStackEntry.arguments?.getFloat(Screen.ARG_START_LON)?.toDouble() ?: 0.0
            val endLat = backStackEntry.arguments?.getFloat(Screen.ARG_END_LAT)?.toDouble() ?: 0.0
            val endLon = backStackEntry.arguments?.getFloat(Screen.ARG_END_LON)?.toDouble() ?: 0.0
            
            // TODO: RouteScreen
            // RouteScreen(
            //     start = LatLng(startLat, startLon),
            //     end = LatLng(endLat, endLon),
            //     onNavigateBack = { navController.popBackStack() },
            //     onStartNavigation = { routeId ->
            //         navController.navigate(Screen.Navigation.createRoute(routeId))
            //     }
            // )
        }
        
        // 导航页
        composable(
            route = Screen.Navigation.route,
            arguments = listOf(
                navArgument(Screen.ARG_ROUTE_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val routeId = backStackEntry.arguments?.getString(Screen.ARG_ROUTE_ID) ?: ""
            
            // TODO: NavigationScreen
            // NavigationScreen(
            //     routeId = routeId,
            //     onNavigateBack = { navController.popBackStack() },
            //     onNavigationComplete = {
            //         navController.popBackStack(Screen.Home.route, inclusive = false)
            //     }
            // )
        }
        
        // POI 详情页
        composable(
            route = Screen.PoiDetail.route,
            arguments = listOf(
                navArgument(Screen.ARG_POI_ID) { type = NavType.StringType }
            ),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(300)
                )
            }
        ) { backStackEntry ->
            val poiId = backStackEntry.arguments?.getString(Screen.ARG_POI_ID) ?: ""
            
            // TODO: PoiDetailScreen
            // PoiDetailScreen(
            //     poiId = poiId,
            //     onNavigateBack = { navController.popBackStack() },
            //     onNavigateTo = { poi ->
            //         // 导航到该 POI
            //     }
            // )
        }
    }
}
