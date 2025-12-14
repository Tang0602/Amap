package com.example.amap_sim.ui.screen.map

import com.example.amap_sim.domain.model.LatLng
import com.example.amap_sim.domain.model.MarkerData
import com.example.amap_sim.domain.model.RouteResult

/**
 * 地图页面 UI 状态
 */
data class MapUiState(
    /** 地图中心点 */
    val center: LatLng = LatLng.WUHAN_CENTER,
    /** 缩放级别 (0-22) */
    val zoomLevel: Int = 14,
    /** 标记点列表 */
    val markers: List<MarkerData> = emptyList(),
    /** 当前路线 */
    val routeResult: RouteResult? = null,
    /** 是否显示当前位置 */
    val showCurrentLocation: Boolean = false,
    /** 当前位置 */
    val currentLocation: LatLng? = null,
    /** 是否正在加载 */
    val isLoading: Boolean = false,
    /** 加载提示信息 */
    val loadingMessage: String? = null,
    /** 错误信息 */
    val error: String? = null,
    /** 地图是否已准备就绪 */
    val isMapReady: Boolean = false
)

/**
 * 地图命令 - 用于 ViewModel 向 MapView 发送命令式操作
 */
sealed class MapCommand {
    /** 移动到指定位置 */
    data class MoveTo(
        val position: LatLng,
        val zoomLevel: Int? = null,
        val animate: Boolean = true
    ) : MapCommand()
    
    /** 缩放到指定级别 */
    data class ZoomTo(
        val zoomLevel: Int,
        val animate: Boolean = true
    ) : MapCommand()
    
    /** 放大一级 */
    data object ZoomIn : MapCommand()
    
    /** 缩小一级 */
    data object ZoomOut : MapCommand()
    
    /** 适配边界框 */
    data class FitBounds(
        val minLat: Double,
        val maxLat: Double,
        val minLon: Double,
        val maxLon: Double,
        val padding: Int = 50
    ) : MapCommand()
    
    /** 添加标记点 */
    data class AddMarker(val marker: MarkerData) : MapCommand()
    
    /** 移除标记点 */
    data class RemoveMarker(val markerId: String) : MapCommand()
    
    /** 清除所有标记点 */
    data object ClearMarkers : MapCommand()
    
    /** 显示路线 */
    data class ShowRoute(val route: RouteResult) : MapCommand()
    
    /** 清除路线 */
    data object ClearRoute : MapCommand()
    
    /** 重绘地图 */
    data object Redraw : MapCommand()
}

/**
 * 地图事件 - MapView 向外部发送的事件
 */
sealed class MapEvent {
    /** 地图准备就绪 */
    data object MapReady : MapEvent()
    
    /** 地图点击 */
    data class MapClick(val position: LatLng) : MapEvent()
    
    /** 地图长按 */
    data class MapLongPress(val position: LatLng) : MapEvent()
    
    /** 标记点点击 */
    data class MarkerClick(val marker: MarkerData) : MapEvent()
    
    /** 标记点长按 */
    data class MarkerLongPress(val marker: MarkerData) : MapEvent()
    
    /** 标记点拖拽结束 */
    data class MarkerDragEnd(val marker: MarkerData, val newPosition: LatLng) : MapEvent()
    
    /** 地图移动结束 */
    data class MapMoveEnd(val center: LatLng, val zoomLevel: Int) : MapEvent()
    
    /** 缩放级别改变 */
    data class ZoomChanged(val zoomLevel: Int) : MapEvent()
}
