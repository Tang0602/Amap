package com.example.amap_sim.ui.screen.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amap_sim.domain.model.LatLng
import com.example.amap_sim.domain.model.MarkerData
import com.example.amap_sim.domain.model.MarkerType
import com.example.amap_sim.domain.model.RouteResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 地图页面 ViewModel
 */
class MapViewModel : ViewModel() {
    
    // UI 状态
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()
    
    // 地图命令通道
    private val _mapCommands = Channel<MapCommand>(Channel.BUFFERED)
    val mapCommands = _mapCommands.receiveAsFlow()
    
    // ============== 地图操作 ==============
    
    /**
     * 地图准备就绪
     */
    fun onMapReady() {
        _uiState.update { 
            it.copy(
                isMapReady = true,
                // 地图就绪后，如果还没有当前位置，设置默认位置（武汉市中心）
                currentLocation = it.currentLocation ?: LatLng.WUHAN_CENTER,
                showCurrentLocation = true
            ) 
        }
    }
    
    /**
     * 地图点击
     */
    fun onMapClick(position: LatLng) {
        // 可以在这里处理地图点击事件
        // 例如：清除选中状态、添加标记等
    }
    
    /**
     * 地图长按
     */
    fun onMapLongPress(position: LatLng) {
        // 长按添加标记点
        val marker = MarkerData(
            id = UUID.randomUUID().toString(),
            position = position,
            title = "自定义位置",
            type = MarkerType.DEFAULT
        )
        addMarker(marker)
    }
    
    /**
     * 标记点点击
     */
    fun onMarkerClick(marker: MarkerData) {
        // 处理标记点点击
        // 例如：显示详情、设置为起点/终点等
    }
    
    /**
     * 地图移动结束
     */
    fun onMapMoveEnd(center: LatLng, zoomLevel: Int) {
        _uiState.update { 
            it.copy(center = center, zoomLevel = zoomLevel) 
        }
    }
    
    // ============== 缩放控制 ==============
    
    /**
     * 放大
     */
    fun zoomIn() {
        viewModelScope.launch {
            _mapCommands.send(MapCommand.ZoomIn)
        }
    }
    
    /**
     * 缩小
     */
    fun zoomOut() {
        viewModelScope.launch {
            _mapCommands.send(MapCommand.ZoomOut)
        }
    }
    
    /**
     * 设置缩放级别
     */
    fun setZoomLevel(level: Int) {
        viewModelScope.launch {
            _mapCommands.send(MapCommand.ZoomTo(level))
        }
    }
    
    // ============== 位置控制 ==============
    
    /**
     * 移动到指定位置
     */
    fun moveTo(position: LatLng, zoomLevel: Int? = null) {
        viewModelScope.launch {
            _mapCommands.send(MapCommand.MoveTo(position, zoomLevel))
        }
    }
    
    /**
     * 移动到当前位置
     */
    fun moveToCurrentLocation() {
        // 在离线模式下，我们使用默认位置（武汉市中心）
        // 如果有模拟位置服务，可以从那里获取
        val currentLocation = _uiState.value.currentLocation ?: LatLng.WUHAN_CENTER
        moveTo(currentLocation, 16)
    }
    
    /**
     * 设置当前位置
     */
    fun setCurrentLocation(location: LatLng) {
        _uiState.update { 
            it.copy(
                currentLocation = location,
                showCurrentLocation = true
            ) 
        }
    }
    
    /**
     * 设置是否显示当前位置标记
     */
    fun setShowCurrentLocation(show: Boolean) {
        _uiState.update { 
            it.copy(showCurrentLocation = show) 
        }
    }
    
    /**
     * 适配边界框
     */
    fun fitBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        padding: Int = 50
    ) {
        viewModelScope.launch {
            _mapCommands.send(
                MapCommand.FitBounds(minLat, maxLat, minLon, maxLon, padding)
            )
        }
    }
    
    // ============== 标记点管理 ==============
    
    /**
     * 添加标记点
     */
    fun addMarker(marker: MarkerData) {
        _uiState.update { state ->
            state.copy(markers = state.markers + marker)
        }
    }
    
    /**
     * 移除标记点
     */
    fun removeMarker(markerId: String) {
        _uiState.update { state ->
            state.copy(markers = state.markers.filter { it.id != markerId })
        }
    }
    
    /**
     * 清除所有标记点
     */
    fun clearMarkers() {
        _uiState.update { it.copy(markers = emptyList()) }
    }
    
    /**
     * 设置标记点列表
     */
    fun setMarkers(markers: List<MarkerData>) {
        _uiState.update { it.copy(markers = markers) }
    }
    
    // ============== 路线管理 ==============
    
    /**
     * 显示路线
     */
    fun showRoute(route: RouteResult) {
        _uiState.update { it.copy(routeResult = route) }
        
        // 适配路线边界
        route.getBoundingBox()?.let { bounds ->
            fitBounds(
                minLat = bounds.minLat,
                maxLat = bounds.maxLat,
                minLon = bounds.minLon,
                maxLon = bounds.maxLon
            )
        }
    }
    
    /**
     * 清除路线
     */
    fun clearRoute() {
        _uiState.update { it.copy(routeResult = null) }
    }
    
    // ============== 加载状态 ==============
    
    /**
     * 设置加载状态
     */
    fun setLoading(isLoading: Boolean, message: String? = null) {
        _uiState.update { 
            it.copy(isLoading = isLoading, loadingMessage = message) 
        }
    }
    
    /**
     * 设置错误信息
     */
    fun setError(error: String) {
        _uiState.update { it.copy(error = error) }
    }
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    // ============== 便捷方法 ==============
    
    /**
     * 设置起点和终点标记
     */
    fun setStartAndEndMarkers(start: LatLng, end: LatLng) {
        val startMarker = MarkerData(
            id = "route_start",
            position = start,
            title = "起点",
            type = MarkerType.START
        )
        
        val endMarker = MarkerData(
            id = "route_end",
            position = end,
            title = "终点",
            type = MarkerType.END
        )
        
        // 移除旧的起点终点标记
        val filteredMarkers = _uiState.value.markers.filter { 
            it.id != "route_start" && it.id != "route_end" 
        }
        
        _uiState.update { 
            it.copy(markers = filteredMarkers + startMarker + endMarker) 
        }
    }
    
    /**
     * 添加 POI 标记
     */
    fun addPoiMarker(
        id: String,
        position: LatLng,
        title: String,
        snippet: String? = null
    ) {
        val marker = MarkerData(
            id = id,
            position = position,
            title = title,
            snippet = snippet,
            type = MarkerType.POI
        )
        addMarker(marker)
    }
}
