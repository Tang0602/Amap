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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Navigation
import com.example.amap_sim.ui.screen.common.UnderConstructionScreen
import com.example.amap_sim.ui.screen.detail.PoiDetailScreen
import com.example.amap_sim.ui.screen.home.HomeScreen
import com.example.amap_sim.ui.screen.search.SearchScreen
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
                },
                onNavigateToDrive = {
                    navController.navigate(Screen.Drive.route)
                },
                onNavigateToBike = {
                    navController.navigate(Screen.Bike.route)
                },
                onNavigateToWalk = {
                    navController.navigate(Screen.Walk.route)
                },
                onNavigateToRoutePlanning = {
                    navController.navigate(Screen.RoutePlanning.route)
                },
                onNavigateToMore = {
                    navController.navigate(Screen.More.route)
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
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onPoiSelected = { poi ->
                    // 导航到 POI 详情页
                    navController.navigate(Screen.PoiDetail.createRoute(poi.id.toString()))
                }
            )
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
            UnderConstructionScreen(
                title = "路线规划",
                description = "路线规划功能正在开发中，即将支持驾车、骑行、步行等多种出行方式！",
                icon = Icons.Default.Directions,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // 导航页
        composable(
            route = Screen.Navigation.route,
            arguments = listOf(
                navArgument(Screen.ARG_ROUTE_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            UnderConstructionScreen(
                title = "导航",
                description = "实时导航功能正在开发中，即将支持语音导航、路况提醒等功能！",
                icon = Icons.Default.Navigation,
                onNavigateBack = { navController.popBackStack() }
            )
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
            PoiDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRoute = { destLat, destLon ->
                    // TODO: 跳转到路线规划页
                    // 暂时跳转到路线规划占位页
                    navController.navigate(Screen.RoutePlanning.route)
                }
            )
        }
        
        // 驾车导航入口页
        composable(
            route = Screen.Drive.route,
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
            UnderConstructionScreen(
                title = "驾车",
                description = "驾车导航功能正在开发中，即将支持实时路况、躲避拥堵等功能！",
                icon = Icons.Default.DirectionsCar,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // 骑行导航入口页
        composable(
            route = Screen.Bike.route,
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
            UnderConstructionScreen(
                title = "骑行",
                description = "骑行导航功能正在开发中，即将支持骑行路线规划、卡路里计算等功能！",
                icon = Icons.Default.DirectionsBike,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // 步行导航入口页
        composable(
            route = Screen.Walk.route,
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
            UnderConstructionScreen(
                title = "步行",
                description = "步行导航功能正在开发中，即将支持步行路线、室内导航等功能！",
                icon = Icons.Default.DirectionsWalk,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // 路线规划入口页
        composable(
            route = Screen.RoutePlanning.route,
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
            UnderConstructionScreen(
                title = "路线",
                description = "路线规划功能正在开发中，即将支持多种出行方式的路线规划！",
                icon = Icons.Default.Directions,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // 更多功能页
        composable(
            route = Screen.More.route,
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
            UnderConstructionScreen(
                title = "更多",
                description = "更多精彩功能正在开发中，敬请期待！",
                icon = Icons.Default.MoreHoriz,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
