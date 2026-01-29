package com.example.amap_sim.ui.navigation

import com.example.amap_sim.domain.model.RouteResult
import com.example.amap_sim.ui.screen.mapcontainer.overlay.route.LocationInput

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
     * 是否应该打开收藏夹
     */
    @Volatile
    private var shouldOpenFavorites: Boolean = false

    /**
     * 待保存的路线历史信息
     */
    @Volatile
    private var pendingRouteHistoryInfo: RouteHistoryInfo? = null

    /**
     * 路线历史信息
     */
    data class RouteHistoryInfo(
        val startLocation: LocationInput,
        val endLocation: LocationInput,
        val routeResult: RouteResult
    )

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

    /**
     * 设置是否应该打开收藏夹
     */
    fun setShouldOpenFavorites(value: Boolean) {
        shouldOpenFavorites = value
    }

    /**
     * 获取并消费是否应该打开收藏夹的标志
     */
    fun consumeShouldOpenFavorites(): Boolean {
        val result = shouldOpenFavorites
        shouldOpenFavorites = false
        return result
    }

    /**
     * 设置路线历史信息
     */
    fun setRouteHistoryInfo(
        startLocation: LocationInput,
        endLocation: LocationInput,
        routeResult: RouteResult
    ) {
        pendingRouteHistoryInfo = RouteHistoryInfo(startLocation, endLocation, routeResult)
    }

    /**
     * 获取并消费路线历史信息
     */
    fun consumeRouteHistoryInfo(): RouteHistoryInfo? {
        val result = pendingRouteHistoryInfo
        pendingRouteHistoryInfo = null
        return result
    }
}

