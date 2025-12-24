package com.example.amap_sim.ui.screen.mapcontainer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amap_sim.domain.model.LatLng
import com.example.amap_sim.domain.model.MarkerData
import com.example.amap_sim.domain.model.MarkerType
import com.example.amap_sim.domain.model.RouteResult
import com.example.amap_sim.ui.screen.mapcontainer.overlay.route.TravelProfile
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 地图容器 ViewModel
 * 
 * 管理：
 * - 地图视图状态（中心点、缩放级别、标记点、路线）
 * - Overlay 状态切换
 * - 实现 MapStateController 接口供业务 ViewModel 调用
 */
class MapContainerViewModel : ViewModel(), MapStateController {
    
    // UI 状态
    private val _uiState = MutableStateFlow(MapContainerUiState())
    val uiState: StateFlow<MapContainerUiState> = _uiState.asStateFlow()
    
    // 地图命令通道
    private val _mapCommands = Channel<MapContainerCommand>(Channel.BUFFERED)
    val mapCommands = _mapCommands.receiveAsFlow()
    
    // ============== Overlay 状态管理 ==============
    
    /**
     * 切换到指定 Overlay
     * 
     * @param newState 新的 Overlay 状态
     * @param addToHistory 是否添加到历史栈（默认 true）
     */
    fun navigateToOverlay(newState: MapOverlayState, addToHistory: Boolean = true) {
        android.util.Log.d("MapContainerViewModel", "navigateToOverlay called: newState=$newState, addToHistory=$addToHistory")
        if (newState is MapOverlayState.RoutePlanning) {
            android.util.Log.d("MapContainerViewModel", "RoutePlanning params: startLocation=${newState.startLocation}, waypoints.size=${newState.waypoints?.size ?: "null"}, endLocation=${newState.endLocation}")
        }
        _uiState.update { current ->
            val newHistory = if (addToHistory && current.overlayState != newState) {
                current.overlayHistory + current.overlayState
            } else {
                current.overlayHistory
            }
            val updated = current.copy(
                overlayState = newState,
                overlayHistory = newHistory
            )
            android.util.Log.d("MapContainerViewModel", "State updated: overlayState=${updated.overlayState}")
            if (updated.overlayState is MapOverlayState.RoutePlanning) {
                android.util.Log.d("MapContainerViewModel", "Updated RoutePlanning params: startLocation=${updated.overlayState.startLocation}, waypoints.size=${updated.overlayState.waypoints?.size ?: "null"}, endLocation=${updated.overlayState.endLocation}")
            }
            updated
        }
    }
    
    /**
     * 返回上一个 Overlay
     * 
     * @return true 如果成功返回，false 如果已经在根 Overlay
     */
    fun navigateBack(): Boolean {
        val currentState = _uiState.value
        if (currentState.overlayHistory.isEmpty()) {
            return false
        }
        
        _uiState.update { current ->
            val previousState = current.overlayHistory.lastOrNull() ?: MapOverlayState.Home
            val newHistory = current.overlayHistory.dropLast(1)
            current.copy(
                overlayState = previousState,
                overlayHistory = newHistory
            )
        }
        return true
    }
    
    /**
     * 返回到主页
     */
    fun navigateToHome() {
        _uiState.update { 
            it.copy(
                overlayState = MapOverlayState.Home,
                overlayHistory = emptyList()
            ) 
        }
        // 清除路线和非必要标记
        clearRoute()
        clearMarkers()
    }
    
    /**
     * 打开搜索 Overlay
     */
    fun openSearch() {
        navigateToOverlay(MapOverlayState.Search)
    }
    
    /**
     * 打开 POI 详情 Overlay
     */
    fun openPoiDetail(poiId: String) {
        navigateToOverlay(MapOverlayState.Detail(poiId))
    }
    
    /**
     * 打开路线规划 Overlay
     * 
     * @param destLat 目的地纬度（可选）
     * @param destLon 目的地经度（可选）
     * @param destName 目的地名称（可选）
     * @param initialProfile 初始交通方式（可选）
     */
    fun openRoutePlanning(
        destLat: Double? = null, 
        destLon: Double? = null, 
        destName: String? = null,
        initialProfile: TravelProfile? = null
    ) {
        navigateToOverlay(MapOverlayState.RoutePlanning(destLat, destLon, destName, initialProfile))
    }
    
    /**
     * 打开添加途径点 Overlay
     * 
     * @param startLocation 起点位置
     * @param waypoints 已有途径点列表
     * @param endLocation 终点位置
     */
    fun openAddWaypoint(
        startLocation: com.example.amap_sim.ui.screen.mapcontainer.overlay.route.LocationInput = com.example.amap_sim.ui.screen.mapcontainer.overlay.route.LocationInput.CurrentLocation,
        waypoints: List<com.example.amap_sim.ui.screen.mapcontainer.overlay.route.LocationInput> = emptyList(),
        endLocation: com.example.amap_sim.ui.screen.mapcontainer.overlay.route.LocationInput? = null
    ) {
        navigateToOverlay(MapOverlayState.AddWaypoint(startLocation, waypoints, endLocation))
    }
    
    // ============== 地图生命周期 ==============
    
    /**
     * 地图准备就绪
     */
    fun onMapReady() {
        _uiState.update { 
            it.copy(
                isMapReady = true,
                currentLocation = it.currentLocation ?: LatLng.WUHAN_CENTER,
                showCurrentLocation = true
            ) 
        }
    }
    
    /**
     * 地图点击
     */
    fun onMapClick(position: LatLng) {
        // 可在此处理地图点击事件
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
    }
    
    /**
     * 地图移动结束
     */
    fun onMapMoveEnd(center: LatLng, zoomLevel: Int) {
        _uiState.update { 
            it.copy(center = center, zoomLevel = zoomLevel) 
        }
    }
    
    // ============== MapStateController 实现 ==============
    
    override fun moveTo(position: LatLng, zoomLevel: Int?, animate: Boolean) {
        viewModelScope.launch {
            _mapCommands.send(MapContainerCommand.MoveTo(position, zoomLevel, animate))
        }
        // 同步更新状态
        _uiState.update { current ->
            current.copy(
                center = position,
                zoomLevel = zoomLevel ?: current.zoomLevel
            )
        }
    }
    
    override fun setMarkers(markers: List<MarkerData>) {
        _uiState.update { it.copy(markers = markers) }
    }
    
    override fun addMarker(marker: MarkerData) {
        _uiState.update { state ->
            state.copy(markers = state.markers + marker)
        }
    }
    
    override fun removeMarker(markerId: String) {
        _uiState.update { state ->
            state.copy(markers = state.markers.filter { it.id != markerId })
        }
    }
    
    override fun clearMarkers() {
        _uiState.update { it.copy(markers = emptyList()) }
    }
    
    override fun setRoute(route: RouteResult) {
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
    
    override fun clearRoute() {
        _uiState.update { it.copy(routeResult = null) }
    }
    
    override fun fitBounds(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double, padding: Int) {
        viewModelScope.launch {
            _mapCommands.send(
                MapContainerCommand.FitBounds(minLat, maxLat, minLon, maxLon, padding)
            )
        }
    }
    
    override fun zoomIn() {
        viewModelScope.launch {
            _mapCommands.send(MapContainerCommand.ZoomIn)
        }
    }
    
    override fun zoomOut() {
        viewModelScope.launch {
            _mapCommands.send(MapContainerCommand.ZoomOut)
        }
    }
    
    override fun moveToCurrentLocation() {
        val currentLocation = _uiState.value.currentLocation ?: LatLng.WUHAN_CENTER
        moveTo(currentLocation, 16)
    }
    
    override fun setCurrentLocation(location: LatLng) {
        _uiState.update { 
            it.copy(
                currentLocation = location,
                showCurrentLocation = true
            ) 
        }
    }
    
    override fun setShowCurrentLocation(show: Boolean) {
        _uiState.update { 
            it.copy(showCurrentLocation = show) 
        }
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
    
    /**
     * 高亮显示单个 POI
     */
    fun highlightPoi(id: String, position: LatLng, title: String) {
        // 清除其他 POI 标记，只保留这一个
        val marker = MarkerData(
            id = "highlighted_poi",
            position = position,
            title = title,
            type = MarkerType.POI
        )
        setMarkers(listOf(marker))
        moveTo(position, 16)
    }
}

