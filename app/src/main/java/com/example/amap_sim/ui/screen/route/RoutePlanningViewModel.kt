package com.example.amap_sim.ui.screen.route

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amap_sim.data.local.BRouterService
import com.example.amap_sim.data.local.OfflineSearchService
import com.example.amap_sim.di.ServiceLocator
import com.example.amap_sim.domain.model.LatLng
import com.example.amap_sim.domain.model.RouteResult
import com.example.amap_sim.ui.navigation.Screen
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 路线规划页 ViewModel
 * 
 * 使用 BRouter 进行离线路由计算
 */
class RoutePlanningViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    companion object {
        private const val TAG = "RoutePlanningViewModel"
    }
    
    // 使用 BRouter 路由服务（替代 GraphHopper）
    private val routingService: BRouterService = ServiceLocator.brouterService
    private val searchService: OfflineSearchService = ServiceLocator.searchService
    
    private val _uiState = MutableStateFlow(RoutePlanningUiState())
    val uiState: StateFlow<RoutePlanningUiState> = _uiState.asStateFlow()
    
    // 导航事件
    private val _navigationEvent = MutableSharedFlow<RoutePlanningNavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()
    
    // 从路由参数获取目的地坐标
    private val destLat: Float? = savedStateHandle.get<Float>(Screen.ARG_DEST_LAT)
    private val destLon: Float? = savedStateHandle.get<Float>(Screen.ARG_DEST_LON)
    private val destName: String? = savedStateHandle.get<String>(Screen.ARG_DEST_NAME)
    
    init {
        initializeDestination()
    }
    
    /**
     * 初始化目的地（如果从其他页面传入）
     */
    private fun initializeDestination() {
        if (destLat != null && destLon != null) {
            val location = LocationInput.SpecificLocation(
                name = destName ?: "目的地",
                coordinates = LatLng(destLat.toDouble(), destLon.toDouble())
            )
            _uiState.update { it.copy(endLocation = location) }
            
            // 自动计算路线
            calculateRoute()
        }
    }
    
    /**
     * 处理事件
     */
    fun onEvent(event: RoutePlanningEvent) {
        when (event) {
            RoutePlanningEvent.NavigateBack -> navigateBack()
            is RoutePlanningEvent.SelectProfile -> selectProfile(event.profile)
            RoutePlanningEvent.SwapLocations -> swapLocations()
            is RoutePlanningEvent.SetStartLocation -> setStartLocation(event.location)
            is RoutePlanningEvent.SetEndLocation -> setEndLocation(event.location)
            RoutePlanningEvent.CalculateRoute -> calculateRoute()
            RoutePlanningEvent.ToggleInstructions -> toggleInstructions()
            RoutePlanningEvent.StartNavigation -> startNavigation()
            RoutePlanningEvent.ClickStartInput -> clickStartInput()
            RoutePlanningEvent.ClickEndInput -> clickEndInput()
            RoutePlanningEvent.ClearError -> clearError()
        }
    }
    
    /**
     * 返回
     */
    private fun navigateBack() {
        viewModelScope.launch {
            _navigationEvent.emit(RoutePlanningNavigationEvent.Back)
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
     * 交换起点终点
     */
    private fun swapLocations() {
        val currentState = _uiState.value
        val newStart = currentState.endLocation ?: return
        val newEnd = currentState.startLocation
        
        _uiState.update {
            it.copy(
                startLocation = newStart,
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
     * 设置目的地（供 Overlay 使用）
     */
    fun setDestination(lat: Double, lon: Double, name: String?) {
        val location = LocationInput.SpecificLocation(
            name = name ?: "目的地",
            coordinates = LatLng(lat, lon)
        )
        setEndLocation(location)
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
     * 计算路线
     */
    private fun calculateRoute() {
        val currentState = _uiState.value
        val start = currentState.startLocation
        val end = currentState.endLocation ?: return
        
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
                
                Log.d(TAG, "开始计算路线: $startLatLng -> $endLatLng, profile=$profile")
                
                val result = routingService.calculateRoute(
                    start = startLatLng,
                    end = endLatLng,
                    profile = profile
                )
                
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
     */
    private fun startNavigation() {
        val routeResult = _uiState.value.routeResult ?: return
        viewModelScope.launch {
            _navigationEvent.emit(RoutePlanningNavigationEvent.StartNavigation(routeResult))
        }
    }
    
    /**
     * 点击起点输入框
     */
    private fun clickStartInput() {
        viewModelScope.launch {
            _navigationEvent.emit(RoutePlanningNavigationEvent.SelectStartFromSearch)
        }
    }
    
    /**
     * 点击终点输入框
     */
    private fun clickEndInput() {
        viewModelScope.launch {
            _navigationEvent.emit(RoutePlanningNavigationEvent.SelectEndFromSearch)
        }
    }
    
    /**
     * 清除错误
     */
    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
