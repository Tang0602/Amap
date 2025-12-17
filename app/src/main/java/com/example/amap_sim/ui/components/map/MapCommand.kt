package com.example.amap_sim.ui.components.map

import com.example.amap_sim.domain.model.LatLng
import com.example.amap_sim.domain.model.MarkerData
import com.example.amap_sim.domain.model.RouteResult

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
