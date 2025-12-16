package com.example.amap_sim.ui.screen.navigation

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amap_sim.domain.model.LatLng
import com.example.amap_sim.domain.model.RouteResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 导航页 ViewModel
 * 
 * 管理实时导航逻辑，包括：
 * - 位置跟踪
 * - 指令更新
 * - 剩余距离/时间计算
 * - 导航模拟（用于测试）
 */
class NavigationViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    companion object {
        private const val TAG = "NavigationViewModel"
        
        // 模拟导航的更新间隔（毫秒）
        private const val SIMULATION_INTERVAL = 1000L
        
        // 模拟速度（米/秒）
        private const val SIMULATION_SPEED_CAR = 11.0      // ~40 km/h
        private const val SIMULATION_SPEED_BIKE = 4.0     // ~15 km/h
        private const val SIMULATION_SPEED_FOOT = 1.4     // ~5 km/h
        
        // 到达下一指令的阈值（米）
        private const val INSTRUCTION_THRESHOLD = 30.0
        
        // 到达目的地的阈值（米）
        private const val ARRIVAL_THRESHOLD = 20.0
    }
    
    private val _uiState = MutableStateFlow(NavigationUiState())
    val uiState: StateFlow<NavigationUiState> = _uiState.asStateFlow()
    
    // 导航事件（页面跳转）
    private val _navigationEvent = MutableSharedFlow<NavigationNavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()
    
    // 模拟导航的 Job
    private var simulationJob: Job? = null
    
    // 当前路线点索引（用于模拟）
    private var currentPointIndex = 0
    
    // 路线总长度（米）
    private var totalRouteLength = 0.0
    
    /**
     * 设置路线并初始化导航
     */
    fun setRoute(routeResult: RouteResult) {
        Log.i(TAG, "设置导航路线: ${routeResult.getFormattedDistance()}, ${routeResult.instructions.size} 条指令")
        
        totalRouteLength = routeResult.distance
        currentPointIndex = 0
        
        val firstInstruction = routeResult.instructions.firstOrNull()
        val secondInstruction = routeResult.instructions.getOrNull(1)
        val startLocation = routeResult.points.firstOrNull() ?: LatLng.WUHAN_CENTER
        
        // 计算预计到达时间
        val arrivalTime = calculateEstimatedArrivalTime(routeResult.time)
        
        _uiState.update {
            it.copy(
                routeResult = routeResult,
                currentLocation = startLocation,
                currentInstructionIndex = 0,
                currentInstruction = firstInstruction,
                nextInstruction = secondInstruction,
                distanceToNextInstruction = firstInstruction?.distance ?: 0.0,
                remainingDistance = routeResult.distance,
                remainingTime = routeResult.time,
                navigationState = NavigationState.NOT_STARTED,
                completedPointIndex = 0,
                estimatedArrivalTime = arrivalTime,
                error = null
            )
        }
    }
    
    /**
     * 处理事件
     */
    fun onEvent(event: NavigationEvent) {
        when (event) {
            NavigationEvent.StartNavigation -> startNavigation()
            NavigationEvent.PauseNavigation -> pauseNavigation()
            NavigationEvent.ResumeNavigation -> resumeNavigation()
            NavigationEvent.StopNavigation -> stopNavigation()
            NavigationEvent.ToggleFollowMode -> toggleFollowMode()
            NavigationEvent.ToggleOverviewMode -> toggleOverviewMode()
            NavigationEvent.NavigateBack -> navigateBack()
            NavigationEvent.Reroute -> reroute()
            is NavigationEvent.MapClick -> handleMapClick(event.position)
            is NavigationEvent.SimulateLocationUpdate -> updateLocation(event.position)
            NavigationEvent.ClearError -> clearError()
        }
    }
    
    /**
     * 开始导航
     */
    private fun startNavigation() {
        Log.i(TAG, "开始导航")
        
        _uiState.update {
            it.copy(
                navigationState = NavigationState.NAVIGATING,
                isFollowingUser = true
            )
        }
        
        // 启动模拟导航
        startSimulation()
    }
    
    /**
     * 暂停导航
     */
    private fun pauseNavigation() {
        Log.i(TAG, "暂停导航")
        
        simulationJob?.cancel()
        simulationJob = null
        
        _uiState.update {
            it.copy(navigationState = NavigationState.PAUSED)
        }
    }
    
    /**
     * 恢复导航
     */
    private fun resumeNavigation() {
        Log.i(TAG, "恢复导航")
        
        _uiState.update {
            it.copy(navigationState = NavigationState.NAVIGATING)
        }
        
        startSimulation()
    }
    
    /**
     * 停止导航
     */
    private fun stopNavigation() {
        Log.i(TAG, "停止导航")
        
        simulationJob?.cancel()
        simulationJob = null
        
        viewModelScope.launch {
            _navigationEvent.emit(NavigationNavigationEvent.Back)
        }
    }
    
    /**
     * 切换跟随模式
     */
    private fun toggleFollowMode() {
        _uiState.update {
            it.copy(
                isFollowingUser = !it.isFollowingUser,
                isOverviewMode = if (!it.isFollowingUser) false else it.isOverviewMode
            )
        }
    }
    
    /**
     * 切换全览模式
     */
    private fun toggleOverviewMode() {
        _uiState.update {
            it.copy(
                isOverviewMode = !it.isOverviewMode,
                isFollowingUser = if (!it.isOverviewMode) false else it.isFollowingUser
            )
        }
    }
    
    /**
     * 返回上一页
     */
    private fun navigateBack() {
        simulationJob?.cancel()
        viewModelScope.launch {
            _navigationEvent.emit(NavigationNavigationEvent.Back)
        }
    }
    
    /**
     * 重新规划路线
     */
    private fun reroute() {
        // 在实际应用中，这里会调用路由服务重新计算路线
        // 目前仅重置到起点
        Log.i(TAG, "重新规划路线")
        
        val routeResult = _uiState.value.routeResult ?: return
        currentPointIndex = 0
        
        _uiState.update {
            it.copy(
                currentLocation = routeResult.points.firstOrNull() ?: LatLng.WUHAN_CENTER,
                currentInstructionIndex = 0,
                currentInstruction = routeResult.instructions.firstOrNull(),
                nextInstruction = routeResult.instructions.getOrNull(1),
                remainingDistance = routeResult.distance,
                remainingTime = routeResult.time,
                completedPointIndex = 0,
                navigationState = NavigationState.NAVIGATING,
                error = null
            )
        }
    }
    
    /**
     * 处理地图点击
     */
    private fun handleMapClick(position: LatLng) {
        // 点击地图时关闭跟随模式
        if (_uiState.value.isFollowingUser) {
            _uiState.update {
                it.copy(isFollowingUser = false)
            }
        }
    }
    
    /**
     * 更新当前位置
     */
    private fun updateLocation(newLocation: LatLng) {
        val state = _uiState.value
        val routeResult = state.routeResult ?: return
        
        // 更新当前位置
        _uiState.update { it.copy(currentLocation = newLocation) }
        
        // 检查是否接近下一个指令点
        checkInstructionProgress(newLocation, routeResult)
        
        // 检查是否到达目的地
        checkArrival(newLocation, routeResult)
    }
    
    /**
     * 检查指令进度
     */
    private fun checkInstructionProgress(currentLocation: LatLng, routeResult: RouteResult) {
        val state = _uiState.value
        val currentIndex = state.currentInstructionIndex
        
        if (currentIndex >= routeResult.instructions.size - 1) return
        
        val nextInstruction = routeResult.instructions.getOrNull(currentIndex + 1) ?: return
        val distanceToNext = currentLocation.distanceTo(nextInstruction.location)
        
        // 更新到下一指令的距离
        _uiState.update {
            it.copy(distanceToNextInstruction = distanceToNext)
        }
        
        // 如果接近下一指令点，切换到下一条指令
        if (distanceToNext < INSTRUCTION_THRESHOLD) {
            val newIndex = currentIndex + 1
            Log.d(TAG, "切换到下一条指令: $newIndex - ${nextInstruction.text}")
            
            _uiState.update {
                it.copy(
                    currentInstructionIndex = newIndex,
                    currentInstruction = nextInstruction,
                    nextInstruction = routeResult.instructions.getOrNull(newIndex + 1),
                    distanceToNextInstruction = routeResult.instructions.getOrNull(newIndex + 1)
                        ?.let { inst -> currentLocation.distanceTo(inst.location) } ?: 0.0
                )
            }
        }
    }
    
    /**
     * 检查是否到达目的地
     */
    private fun checkArrival(currentLocation: LatLng, routeResult: RouteResult) {
        val destination = routeResult.points.lastOrNull() ?: return
        val distanceToDestination = currentLocation.distanceTo(destination)
        
        if (distanceToDestination < ARRIVAL_THRESHOLD) {
            Log.i(TAG, "已到达目的地")
            
            simulationJob?.cancel()
            simulationJob = null
            
            _uiState.update {
                it.copy(
                    navigationState = NavigationState.ARRIVED,
                    remainingDistance = 0.0,
                    remainingTime = 0L,
                    currentInstructionIndex = routeResult.instructions.size - 1,
                    currentInstruction = routeResult.instructions.lastOrNull()
                )
            }
        }
    }
    
    /**
     * 启动导航模拟
     */
    private fun startSimulation() {
        simulationJob?.cancel()
        
        simulationJob = viewModelScope.launch {
            val routeResult = _uiState.value.routeResult ?: return@launch
            val points = routeResult.points
            
            if (points.isEmpty()) return@launch
            
            // 根据交通方式确定模拟速度
            val speed = when (routeResult.profile) {
                "car", "car-vario" -> SIMULATION_SPEED_CAR
                "bike", "trekking" -> SIMULATION_SPEED_BIKE
                else -> SIMULATION_SPEED_FOOT
            }
            
            Log.d(TAG, "开始模拟导航，速度: ${speed * 3.6} km/h")
            
            while (_uiState.value.navigationState == NavigationState.NAVIGATING) {
                delay(SIMULATION_INTERVAL)
                
                if (currentPointIndex >= points.size - 1) {
                    // 到达终点
                    checkArrival(points.last(), routeResult)
                    break
                }
                
                // 计算下一个位置
                val currentPos = points[currentPointIndex]
                val nextPos = points[currentPointIndex + 1]
                val segmentDistance = currentPos.distanceTo(nextPos)
                val moveDistance = speed * (SIMULATION_INTERVAL / 1000.0)
                
                if (moveDistance >= segmentDistance) {
                    // 移动到下一个路线点
                    currentPointIndex++
                    updateSimulationState(points[currentPointIndex], routeResult, points)
                } else {
                    // 在当前段上插值
                    val ratio = moveDistance / segmentDistance
                    val newLat = currentPos.lat + (nextPos.lat - currentPos.lat) * ratio
                    val newLon = currentPos.lon + (nextPos.lon - currentPos.lon) * ratio
                    val newPos = LatLng(newLat, newLon)
                    updateSimulationState(newPos, routeResult, points)
                }
            }
        }
    }
    
    /**
     * 更新模拟状态
     */
    private fun updateSimulationState(newLocation: LatLng, routeResult: RouteResult, points: List<LatLng>) {
        // 计算剩余距离
        var remaining = 0.0
        for (i in currentPointIndex until points.size - 1) {
            remaining += points[i].distanceTo(points[i + 1])
        }
        remaining += newLocation.distanceTo(points[currentPointIndex])
        
        // 计算剩余时间（基于速度比例）
        val progress = 1 - (remaining / totalRouteLength)
        val remainingTime = (routeResult.time * (1 - progress)).toLong()
        
        // 计算当前速度
        val speed = when (routeResult.profile) {
            "car", "car-vario" -> SIMULATION_SPEED_CAR * 3.6
            "bike", "trekking" -> SIMULATION_SPEED_BIKE * 3.6
            else -> SIMULATION_SPEED_FOOT * 3.6
        }
        
        // 更新预计到达时间
        val arrivalTime = calculateEstimatedArrivalTime(remainingTime)
        
        _uiState.update {
            it.copy(
                currentLocation = newLocation,
                remainingDistance = remaining,
                remainingTime = remainingTime,
                currentSpeed = speed,
                completedPointIndex = currentPointIndex,
                estimatedArrivalTime = arrivalTime
            )
        }
        
        // 检查指令进度
        checkInstructionProgress(newLocation, routeResult)
        
        // 检查是否到达
        checkArrival(newLocation, routeResult)
    }
    
    /**
     * 计算预计到达时间
     */
    private fun calculateEstimatedArrivalTime(remainingTimeMs: Long): String {
        val arrivalTimeMs = System.currentTimeMillis() + remainingTimeMs
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(Date(arrivalTimeMs))
    }
    
    /**
     * 清除错误
     */
    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    override fun onCleared() {
        super.onCleared()
        simulationJob?.cancel()
    }
}

