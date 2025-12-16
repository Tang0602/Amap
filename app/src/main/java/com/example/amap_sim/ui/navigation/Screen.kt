package com.example.amap_sim.ui.navigation

/**
 * 应用页面路由定义
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
     * 主页 - 地图 + 搜索入口（已废弃，使用 MapContainer）
     */
    @Deprecated("使用 MapContainer 替代")
    data object Home : Screen("home")
    
    /**
     * 搜索页 - POI 搜索
     */
    data object Search : Screen("search")
    
    /**
     * 路线规划页
     */
    data object Route : Screen("route/{startLat}/{startLon}/{endLat}/{endLon}") {
        fun createRoute(startLat: Double, startLon: Double, endLat: Double, endLon: Double): String {
            return "route/$startLat/$startLon/$endLat/$endLon"
        }
    }
    
    /**
     * 导航页 - 实时导航
     */
    data object Navigation : Screen("navigation/{routeId}") {
        fun createRoute(routeId: String): String {
            return "navigation/$routeId"
        }
    }
    
    /**
     * POI 详情页
     */
    data object PoiDetail : Screen("poi/{poiId}") {
        fun createRoute(poiId: String): String {
            return "poi/$poiId"
        }
    }
    
    /**
     * 驾车导航入口页
     */
    data object Drive : Screen("drive")
    
    /**
     * 骑行导航入口页
     */
    data object Bike : Screen("bike")
    
    /**
     * 步行导航入口页
     */
    data object Walk : Screen("walk")
    
    /**
     * 路线规划入口页（快捷入口，可选带目的地参数）
     */
    data object RoutePlanning : Screen("route_planning?destLat={destLat}&destLon={destLon}&destName={destName}") {
        const val BASE_ROUTE = "route_planning"
        
        /**
         * 创建路由（无参数）
         */
        fun createRoute(): String = BASE_ROUTE
        
        /**
         * 创建路由（带目的地参数）
         */
        fun createRoute(destLat: Double, destLon: Double, destName: String): String {
            return "$BASE_ROUTE?destLat=$destLat&destLon=$destLon&destName=${java.net.URLEncoder.encode(destName, "UTF-8")}"
        }
    }
    
    /**
     * 更多功能页
     */
    data object More : Screen("more")
    
    companion object {
        // 路由参数名
        const val ARG_START_LAT = "startLat"
        const val ARG_START_LON = "startLon"
        const val ARG_END_LAT = "endLat"
        const val ARG_END_LON = "endLon"
        const val ARG_ROUTE_ID = "routeId"
        const val ARG_POI_ID = "poiId"
        const val ARG_DEST_LAT = "destLat"
        const val ARG_DEST_LON = "destLon"
        const val ARG_DEST_NAME = "destName"
    }
}
