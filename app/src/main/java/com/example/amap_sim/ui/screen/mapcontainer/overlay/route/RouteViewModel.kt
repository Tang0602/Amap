package com.example.amap_sim.ui.screen.mapcontainer.overlay.route

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amap_sim.data.local.AgentDataManager
import com.example.amap_sim.data.local.BRouterService
import com.example.amap_sim.data.local.OfflineSearchService
import com.example.amap_sim.di.ServiceLocator
import com.example.amap_sim.domain.model.LatLng
import com.example.amap_sim.domain.model.MarkerData
import com.example.amap_sim.domain.model.MarkerType
import com.example.amap_sim.domain.model.RouteResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 路线规划 Overlay ViewModel
 * 
 * 使用 BRouter 进行离线路由计算
 */
class RouteViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "RouteViewModel"
    }
    
    // 使用 BRouter 路由服务
    private val routingService: BRouterService = ServiceLocator.brouterService
    private val searchService: OfflineSearchService = ServiceLocator.searchService
    private val agentDataManager: AgentDataManager = ServiceLocator.agentDataManager
    
    private val _uiState = MutableStateFlow(RouteUiState())
    val uiState: StateFlow<RouteUiState> = _uiState.asStateFlow()
    
    // 导航事件
    private val _navigationEvent = MutableSharedFlow<RouteNavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()
    
    init {
        observeRouteChanges()
    }
    
    /**
     * 设置初始交通方式（供 Overlay 初始化使用）
     */
    fun setInitialProfile(profile: TravelProfile) {
        _uiState.update { it.copy(selectedProfile = profile) }
    }
    
    /**
     * 监听路线相关状态变化，计算地图更新信息
     * 
     * 业务逻辑：根据起点、终点和路线结果创建地图标记和路线显示
     */
    private fun observeRouteChanges() {
        viewModelScope.launch {
            combine(
                uiState.map { it.startLocation },
                uiState.map { it.waypoints },
                uiState.map { it.endLocation },
                uiState.map { it.routeResult }
            ) { start: LocationInput, waypoints: List<LocationInput>, end: LocationInput?, route: RouteResult? ->
                Quadruple(start, waypoints, end, route)
            }
                .distinctUntilChanged()
                .collect { (start, waypoints, end, route) ->
                    updateMapState(start, waypoints, end, route)
                }
        }
    }
    
    // 辅助数据类
    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
    
    /**
     * 更新地图状态
     * 
     * 根据起点、途径点、终点和路线结果计算需要的地图更新操作
     */
    private fun updateMapState(
        startLocation: LocationInput,
        waypoints: List<LocationInput>,
        endLocation: LocationInput?,
        routeResult: RouteResult?
    ) {
        val start = startLocation.getLatLng()
        val end = endLocation?.getLatLng()
        
        if (end == null) {
            // 没有终点，清除地图
            _uiState.update { it.copy(mapUpdate = RouteMapUpdate.Clear) }
            return
        }
        
        // 创建起点标记
        val startMarker = MarkerData(
            id = "route_start",
            position = start,
            title = "起点",
            type = MarkerType.START
        )
        
        // 创建途径点标记
        val waypointMarkers = waypoints.mapIndexed { index, waypoint ->
            MarkerData(
                id = "route_waypoint_$index",
                position = waypoint.getLatLng(),
                title = "途经点 ${index + 1}",
                type = MarkerType.WAYPOINT
            )
        }
        
        // 创建终点标记
        val endMarker = MarkerData(
            id = "route_end",
            position = end,
            title = "终点",
            type = MarkerType.END
        )
        
        val mapUpdate = if (routeResult != null) {
            // 有路线结果：显示标记和路线
            RouteMapUpdate.ShowRoute(
                startMarker = startMarker,
                waypointMarkers = waypointMarkers,
                endMarker = endMarker,
                routeResult = routeResult
            )
        } else {
            // 无路线结果：只显示标记
            RouteMapUpdate.ShowMarkersOnly(
                startMarker = startMarker,
                waypointMarkers = waypointMarkers,
                endMarker = endMarker
            )
        }
        
        _uiState.update { it.copy(mapUpdate = mapUpdate) }
    }
    
    /**
     * 设置目的地（供 Overlay 初始化使用）
     */
    fun setDestination(lat: Double, lon: Double, name: String?) {
        val location = LocationInput.SpecificLocation(
            name = name ?: "目的地",
            coordinates = LatLng(lat, lon)
        )
        _uiState.update { 
            it.copy(
                endLocation = location, 
                routeResult = null,
                isEndInputFocused = false
            ) 
        }
        // 自动计算路线
        calculateRoute()
    }
    
    /**
     * 处理事件
     */
    fun onEvent(event: RouteEvent) {
        when (event) {
            RouteEvent.NavigateBack -> navigateBack()
            is RouteEvent.SelectProfile -> selectProfile(event.profile)
            RouteEvent.SwapLocations -> swapLocations()
            is RouteEvent.SetStartLocation -> setStartLocation(event.location)
            is RouteEvent.SetEndLocation -> setEndLocation(event.location)
            is RouteEvent.UpdateWaypoints -> updateWaypoints(event.startLocation, event.waypoints, event.endLocation)
            RouteEvent.CalculateRoute -> calculateRoute()
            RouteEvent.ToggleInstructions -> toggleInstructions()
            RouteEvent.StartNavigation -> startNavigation()
            RouteEvent.ClickStartInput -> clickStartInput()
            RouteEvent.ClickEndInput -> clickEndInput()
            RouteEvent.ClearError -> clearError()
        }
    }
    
    /**
     * 返回
     */
    private fun navigateBack() {
        viewModelScope.launch {
            _navigationEvent.emit(RouteNavigationEvent.Back)
        }
    }
    
    /**
     * 选择交通方式
     */
    private fun selectProfile(profile: TravelProfile) {
        if (_uiState.value.selectedProfile != profile) {
            _uiState.update { it.copy(selectedProfile = profile, routeResult = null) }
            // 重新计算路线
            if (_uiState.value.endLocation != null) {
                calculateRoute()
            }
        }
    }
    
    /**
     * 倒序排列所有地点（起点 + 途径点 + 终点）
     */
    private fun swapLocations() {
        val currentState = _uiState.value

        // 构建完整的地点列表
        val allLocations = mutableListOf<LocationInput>()
        allLocations.add(currentState.startLocation)
        allLocations.addAll(currentState.waypoints)
        currentState.endLocation?.let { allLocations.add(it) }

        // 如果列表少于2个地点，无法倒序
        if (allLocations.size < 2) return

        // 倒序排列
        val reversedLocations = allLocations.reversed()

        // 提取新的起点、途径点、终点
        val newStart = reversedLocations.first()
        val newEnd = if (reversedLocations.size > 1) reversedLocations.last() else null
        val newWaypoints = if (reversedLocations.size > 2) {
            reversedLocations.subList(1, reversedLocations.size - 1)
        } else {
            emptyList()
        }

        _uiState.update {
            it.copy(
                startLocation = newStart,
                waypoints = newWaypoints,
                endLocation = newEnd,
                routeResult = null
            )
        }

        // 重新计算路线
        calculateRoute()
    }
    
    /**
     * 设置起点
     */
    private fun setStartLocation(location: LocationInput) {
        _uiState.update { 
            it.copy(
                startLocation = location, 
                routeResult = null,
                isStartInputFocused = false
            ) 
        }
        // 如果终点已设置，自动计算路线
        if (_uiState.value.endLocation != null) {
            calculateRoute()
        }
    }
    
    /**
     * 设置终点
     */
    private fun setEndLocation(location: LocationInput) {
        _uiState.update { 
            it.copy(
                endLocation = location, 
                routeResult = null,
                isEndInputFocused = false
            ) 
        }
        // 自动计算路线
        calculateRoute()
    }
    
    /**
     * 更新终点（从搜索页返回后调用）
     */
    fun updateEndLocation(name: String, lat: Double, lon: Double) {
        val location = LocationInput.SpecificLocation(
            name = name,
            coordinates = LatLng(lat, lon)
        )
        setEndLocation(location)
    }
    
    /**
     * 更新起点（从搜索页返回后调用）
     */
    fun updateStartLocation(name: String, lat: Double, lon: Double) {
        val location = LocationInput.SpecificLocation(
            name = name,
            coordinates = LatLng(lat, lon)
        )
        setStartLocation(location)
    }
    
    /**
     * 更新途径点（从添加途径点页面返回后调用）
     */
    private fun updateWaypoints(
        startLocation: LocationInput,
        waypoints: List<LocationInput>,
        endLocation: LocationInput?
    ) {
        Log.d(TAG, "updateWaypoints: startLocation=$startLocation, waypoints.size=${waypoints.size}, endLocation=$endLocation")
        _uiState.update {
            it.copy(
                startLocation = startLocation,
                waypoints = waypoints,
                endLocation = endLocation,
                routeResult = null
            )
        }
        Log.d(TAG, "updateWaypoints: 状态已更新，waypoints.size=${_uiState.value.waypoints.size}")
        // 如果终点已设置，自动计算路线
        if (endLocation != null) {
            calculateRoute()
        }
    }
    
    /**
     * 计算路线
     */
    private fun calculateRoute() {
        val currentState = _uiState.value
        val start = currentState.startLocation
        val end = currentState.endLocation ?: return
        val waypoints = currentState.waypoints
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // 确保路由服务已初始化
                if (!routingService.isReady()) {
                    Log.d(TAG, "路由服务未初始化，正在初始化...")
                    val initResult = routingService.initialize()
                    if (initResult.isFailure) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "路由服务初始化失败: ${initResult.exceptionOrNull()?.message}"
                            )
                        }
                        return@launch
                    }
                }
                
                val startLatLng = start.getLatLng()
                val endLatLng = end.getLatLng()
                val profile = currentState.selectedProfile.profileId
                
                val result = if (waypoints.isNotEmpty()) {
                    // 有途径点，使用 calculateRouteWithWaypoints
                    val allPoints = listOf(startLatLng) + waypoints.map { it.getLatLng() } + endLatLng
                    Log.d(TAG, "开始计算路线（含途径点）: ${allPoints.size} 个点, profile=$profile")
                    routingService.calculateRouteWithWaypoints(
                        points = allPoints,
                        profile = profile
                    )
                } else {
                    // 无途径点，使用普通 calculateRoute
                    Log.d(TAG, "开始计算路线: $startLatLng -> $endLatLng, profile=$profile")
                    routingService.calculateRoute(
                        start = startLatLng,
                        end = endLatLng,
                        profile = profile
                    )
                }
                
                if (result.isSuccess) {
                    val routeResult = result.getOrNull()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            routeResult = routeResult,
                            error = null
                        )
                    }
                    Log.i(TAG, "路线计算成功: ${routeResult?.getFormattedDistance()}, ${routeResult?.getFormattedTime()}")
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "路线计算失败"
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = errorMsg
                        )
                    }
                    Log.e(TAG, "路线计算失败: $errorMsg")
                }
            } catch (e: Exception) {
                Log.e(TAG, "路线计算异常", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "路线计算失败"
                    )
                }
            }
        }
    }
    
    /**
     * 切换显示指令详情
     */
    private fun toggleInstructions() {
        _uiState.update { it.copy(showInstructions = !it.showInstructions) }
    }
    
    /**
     * 开始导航
     *
     * 同时更新 AgentDataManager 的文件7（指令7：步行导航去M+购物中心）
     * 文件16（指令16：步行导航去肖记公安牛肉鱼杂馆）
     * 文件17（指令17：从M+购物中心导航到我的位置）
     */
    private fun startNavigation() {
        val routeResult = _uiState.value.routeResult ?: return
        val startLocation = _uiState.value.startLocation
        val endLocation = _uiState.value.endLocation ?: return
        val selectedProfile = _uiState.value.selectedProfile

        // 获取起点和目的地名称
        val startName = when (startLocation) {
            is LocationInput.CurrentLocation -> "我的位置"
            is LocationInput.SpecificLocation -> startLocation.name
        }
        val destinationName = when (endLocation) {
            is LocationInput.CurrentLocation -> "我的位置"
            is LocationInput.SpecificLocation -> endLocation.name
        }
        val transportMode = selectedProfile.displayName  // "步行", "骑行", or "驾车"
        val fullDestination = "$transportMode 到 $destinationName"

        // 更新 Agent 数据文件7（指令7检测用）
        agentDataManager.updateFile7(fullDestination, true)
        Log.d(TAG, "已更新 Agent 文件7: destination=$fullDestination, started=true")

        // 更新 Agent 数据文件16（指令16检测用：步行导航去肖记公安牛肉鱼杂馆）
        if (destinationName == "肖记公安牛肉鱼杂馆" && selectedProfile == TravelProfile.FOOT) {
            agentDataManager.updateFile16(destinationName, transportMode, true)
            Log.d(TAG, "已更新 Agent 文件16: destination=$destinationName, mode=$transportMode, started=true")
        }

        // 更新 Agent 数据文件17（指令17检测用：从M+购物中心导航到我的位置）
        if (startName == "M+购物中心" && endLocation is LocationInput.CurrentLocation) {
            agentDataManager.updateFile17(startName, destinationName, true)
            Log.d(TAG, "已更新 Agent 文件17: from=$startName, to=$destinationName, started=true")
        }

        viewModelScope.launch {
            _navigationEvent.emit(RouteNavigationEvent.StartNavigation(routeResult, startLocation, endLocation))
        }
    }
    
    /**
     * 点击起点输入框
     */
    private fun clickStartInput() {
        viewModelScope.launch {
            _navigationEvent.emit(RouteNavigationEvent.SelectStartFromSearch)
        }
    }
    
    /**
     * 点击终点输入框
     */
    private fun clickEndInput() {
        viewModelScope.launch {
            _navigationEvent.emit(RouteNavigationEvent.SelectEndFromSearch)
        }
    }
    
    /**
     * 清除错误
     */
    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

