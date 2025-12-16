package com.example.amap_sim.ui.screen.mapcontainer

import com.example.amap_sim.domain.model.LatLng
import com.example.amap_sim.domain.model.MarkerData
import com.example.amap_sim.domain.model.RouteResult

/**
 * 地图状态控制器接口
 * 
 * 各业务 ViewModel 通过此接口控制地图
 * 实现依赖倒置，业务逻辑与地图实现解耦
 */
interface MapStateController {
    
    /**
     * 移动地图到指定位置
     * 
     * @param position 目标位置
     * @param zoomLevel 缩放级别（可选，不指定则保持当前级别）
     * @param animate 是否使用动画
     */
    fun moveTo(position: LatLng, zoomLevel: Int? = null, animate: Boolean = true)
    
    /**
     * 设置标记点列表
     * 
     * @param markers 标记点列表
     */
    fun setMarkers(markers: List<MarkerData>)
    
    /**
     * 添加单个标记点
     * 
     * @param marker 标记点
     */
    fun addMarker(marker: MarkerData)
    
    /**
     * 移除标记点
     * 
     * @param markerId 标记点 ID
     */
    fun removeMarker(markerId: String)
    
    /**
     * 清除所有标记点
     */
    fun clearMarkers()
    
    /**
     * 显示路线
     * 
     * @param route 路线结果
     */
    fun setRoute(route: RouteResult)
    
    /**
     * 清除路线
     */
    fun clearRoute()
    
    /**
     * 适配边界框
     * 
     * @param minLat 最小纬度
     * @param maxLat 最大纬度
     * @param minLon 最小经度
     * @param maxLon 最大经度
     * @param padding 边距（像素）
     */
    fun fitBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        padding: Int = 50
    )
    
    /**
     * 放大
     */
    fun zoomIn()
    
    /**
     * 缩小
     */
    fun zoomOut()
    
    /**
     * 移动到当前位置
     */
    fun moveToCurrentLocation()
    
    /**
     * 设置当前位置
     * 
     * @param location 当前位置
     */
    fun setCurrentLocation(location: LatLng)
    
    /**
     * 设置是否显示当前位置标记
     * 
     * @param show 是否显示
     */
    fun setShowCurrentLocation(show: Boolean)
}

