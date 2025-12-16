package com.example.amap_sim.ui.screen.mapcontainer

import com.example.amap_sim.domain.model.LatLng
import com.example.amap_sim.domain.model.MarkerData
import com.example.amap_sim.domain.model.RouteResult

/**
 * 地图容器 UI 状态
 * 
 * 管理地图相关的共享状态，包括：
 * - 地图视图状态（中心点、缩放级别）
 * - 覆盖物状态（标记点、路线）
 * - Overlay 状态
 */
data class MapContainerUiState(
    /** 地图中心点 */
    val center: LatLng = LatLng.WUHAN_CENTER,
    
    /** 缩放级别 (0-22) */
    val zoomLevel: Int = 14,
    
    /** 标记点列表 */
    val markers: List<MarkerData> = emptyList(),
    
    /** 当前路线 */
    val routeResult: RouteResult? = null,
    
    /** 当前位置 */
    val currentLocation: LatLng? = null,
    
    /** 是否显示当前位置标记 */
    val showCurrentLocation: Boolean = true,
    
    /** 地图是否已准备就绪 */
    val isMapReady: Boolean = false,
    
    /** 当前 Overlay 状态 */
    val overlayState: MapOverlayState = MapOverlayState.Home,
    
    /** Overlay 历史栈 (用于返回操作) */
    val overlayHistory: List<MapOverlayState> = emptyList(),
    
    /** 是否正在加载 */
    val isLoading: Boolean = false,
    
    /** 加载提示信息 */
    val loadingMessage: String? = null,
    
    /** 错误信息 */
    val error: String? = null
) {
    /**
     * 是否可以返回上一个 Overlay
     */
    val canGoBack: Boolean
        get() = overlayHistory.isNotEmpty()
    
    /**
     * 是否在主页
     */
    val isHome: Boolean
        get() = overlayState is MapOverlayState.Home
}

/**
 * 地图命令
 * 
 * 用于 ViewModel 向 MapView 发送命令式操作
 */
sealed class MapContainerCommand {
    /** 移动到指定位置 */
    data class MoveTo(
        val position: LatLng,
        val zoomLevel: Int? = null,
        val animate: Boolean = true
    ) : MapContainerCommand()
    
    /** 缩放到指定级别 */
    data class ZoomTo(
        val zoomLevel: Int,
        val animate: Boolean = true
    ) : MapContainerCommand()
    
    /** 放大一级 */
    data object ZoomIn : MapContainerCommand()
    
    /** 缩小一级 */
    data object ZoomOut : MapContainerCommand()
    
    /** 适配边界框 */
    data class FitBounds(
        val minLat: Double,
        val maxLat: Double,
        val minLon: Double,
        val maxLon: Double,
        val padding: Int = 50
    ) : MapContainerCommand()
    
    /** 重绘地图 */
    data object Redraw : MapContainerCommand()
}

