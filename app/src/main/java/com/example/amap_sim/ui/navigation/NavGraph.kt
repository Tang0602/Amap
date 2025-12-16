package com.example.amap_sim.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Navigation
import com.example.amap_sim.ui.screen.common.UnderConstructionScreen
import com.example.amap_sim.ui.screen.mapcontainer.MapContainerScreen
import com.example.amap_sim.ui.screen.navigation.NavigationScreen
import com.example.amap_sim.ui.screen.splash.SplashScreen

/**
 * 应用导航图
 * 
 * 采用 Overlay 架构：
 * - Splash -> MapContainer (主入口，承载所有 Overlay)
 * - Navigation (独立页面，实时导航)
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
                    // 初始化完成，导航到地图容器页（清除启动页）
                    navController.navigate(Screen.MapContainer.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onInitError = { error ->
                    // 初始化失败，仍然进入地图容器页（会显示错误状态）
                    navController.navigate(Screen.MapContainer.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        
        // 地图容器页（承载所有 Overlay：Home、Search、Detail、RoutePlanning）
        composable(
            route = Screen.MapContainer.route,
            enterTransition = {
                fadeIn(animationSpec = tween(300))
            }
        ) {
            MapContainerScreen(
                onNavigateToNavigation = { routeResult ->
                    // 将路线结果存储到 NavigationStateHolder
                    NavigationStateHolder.setRouteForNavigation(routeResult)
                    // 跳转到导航页
                    navController.navigate(Screen.Navigation.createRoute("route_${System.currentTimeMillis()}"))
                },
                onNavigateToDrive = { navController.navigate(Screen.Drive.route) },
                onNavigateToBike = { navController.navigate(Screen.Bike.route) },
                onNavigateToWalk = { navController.navigate(Screen.Walk.route) },
                onNavigateToMore = { navController.navigate(Screen.More.route) }
            )
        }
        
        // 导航页（独立页面，实时导航）
        composable(
            route = Screen.Navigation.route,
            arguments = listOf(
                navArgument(Screen.ARG_ROUTE_ID) { type = NavType.StringType }
            ),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(400)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(400)
                )
            }
        ) { backStackEntry ->
            // 从 NavigationStateHolder 获取路线
            val routeResult = remember { NavigationStateHolder.consumeRouteForNavigation() }
            
            if (routeResult != null) {
                NavigationScreen(
                    routeResult = routeResult,
                    onNavigateBack = { 
                        navController.popBackStack() 
                    },
                    onNavigationFinished = {
                        // 导航结束，返回到地图容器页
                        navController.navigate(Screen.MapContainer.route) {
                            popUpTo(Screen.MapContainer.route) { inclusive = true }
                        }
                    }
                )
            } else {
                // 如果没有路线数据，显示错误或返回
                UnderConstructionScreen(
                    title = "导航",
                    description = "未找到路线数据，请先进行路线规划",
                    icon = Icons.Default.Navigation,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        
        // 驾车入口页（开发中）
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
        
        // 骑行入口页（开发中）
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
        
        // 步行入口页（开发中）
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
        
        // 更多功能页（开发中）
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
