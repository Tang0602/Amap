package com.example.amap_sim.ui.navigation

import com.example.amap_sim.domain.model.RouteResult

/**
 * 导航状态持有器
 * 
 * 用于在页面间传递复杂对象（如 RouteResult）
 * 由于 Navigation Compose 不支持直接传递复杂对象，
 * 使用此单例来临时存储导航所需的数据
 */
object NavigationStateHolder {
    
    /**
     * 待导航的路线结果
     */
    @Volatile
    private var pendingRouteResult: RouteResult? = null
    
    /**
     * 设置待导航的路线
     */
    fun setRouteForNavigation(routeResult: RouteResult) {
        pendingRouteResult = routeResult
    }
    
    /**
     * 获取并消费待导航的路线
     * 
     * 调用后会清除存储的路线，防止重复使用
     */
    fun consumeRouteForNavigation(): RouteResult? {
        val result = pendingRouteResult
        pendingRouteResult = null
        return result
    }
    
    /**
     * 查看待导航的路线（不消费）
     */
    fun peekRouteForNavigation(): RouteResult? {
        return pendingRouteResult
    }
    
    /**
     * 清除待导航的路线
     */
    fun clearRoute() {
        pendingRouteResult = null
    }
}

