package com.example.amap_sim.ui.navigation

/**
 * 应用页面路由定义
 * 
 * Overlay 架构下，只保留必要的顶级页面路由：
 * - Splash: 启动页
 * - MapContainer: 地图容器（承载所有 Overlay）
 * - Navigation: 实时导航页（独立页面）
 */
sealed class Screen(val route: String) {
    
    /**
     * 启动页 - 数据初始化
     */
    data object Splash : Screen("splash")
    
    /**
     * 地图容器页 - 承载所有 Overlay（Home、Search、Detail、RoutePlanning）
     */
    data object MapContainer : Screen("map_container")
    
    /**
     * 导航页 - 实时导航（独立页面）
     */
    data object Navigation : Screen("navigation/{routeId}") {
        fun createRoute(routeId: String): String {
            return "navigation/$routeId"
        }
    }
    
    /**
     * 更多功能页（开发中）
     */
    data object More : Screen("more")
    
    companion object {
        // 路由参数名
        const val ARG_ROUTE_ID = "routeId"
    }
}
