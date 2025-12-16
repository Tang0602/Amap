package com.example.amap_sim.ui.screen.mapcontainer

import com.example.amap_sim.ui.screen.mapcontainer.overlay.route.TravelProfile

/**
 * Overlay 状态定义
 * 
 * 定义地图容器中所有可能的 Overlay 状态
 */
sealed class MapOverlayState {
    
    /**
     * 主页 Overlay - 搜索框 + 快捷入口
     */
    data object Home : MapOverlayState()
    
    /**
     * 搜索 Overlay - 搜索框 + 结果列表
     */
    data object Search : MapOverlayState()
    
    /**
     * POI 详情 Overlay - 详情卡片
     * 
     * @param poiId POI ID
     */
    data class Detail(
        val poiId: String
    ) : MapOverlayState()
    
    /**
     * 路线规划 Overlay - 起终点 + 路线信息
     * 
     * @param destLat 目的地纬度（可选）
     * @param destLon 目的地经度（可选）
     * @param destName 目的地名称（可选）
     * @param initialProfile 初始交通方式（可选）
     */
    data class RoutePlanning(
        val destLat: Double? = null,
        val destLon: Double? = null,
        val destName: String? = null,
        val initialProfile: TravelProfile? = null
    ) : MapOverlayState()
    
    companion object {
        /**
         * 默认状态
         */
        val Default: MapOverlayState = Home
    }
}

