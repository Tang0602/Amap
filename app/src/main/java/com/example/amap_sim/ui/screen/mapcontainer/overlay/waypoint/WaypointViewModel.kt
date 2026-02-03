package com.example.amap_sim.ui.screen.mapcontainer.overlay.waypoint

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amap_sim.data.local.OfflineSearchService
import com.example.amap_sim.di.ServiceLocator
import com.example.amap_sim.domain.model.LatLng
import com.example.amap_sim.domain.model.PoiResult
import com.example.amap_sim.ui.screen.mapcontainer.overlay.route.LocationInput
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 途径点管理 ViewModel
 */
class WaypointViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "WaypointViewModel"
        private const val SEARCH_DEBOUNCE_MS = 300L
    }
    
    private val searchService: OfflineSearchService = ServiceLocator.searchService
    
    private val _uiState = MutableStateFlow(WaypointUiState())
    val uiState: StateFlow<WaypointUiState> = _uiState.asStateFlow()
    
    // 导航事件
    private val _navigationEvent = MutableSharedFlow<WaypointNavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()
    
    // 搜索任务
    private var searchJob: Job? = null
    
    // 默认中心点（武汉）
    private val defaultCenter = LatLng(30.5928, 114.3055)
    
    init {
        initializeSearchService()
        loadSuggestedLocations()
    }
    
    /**
     * 初始化搜索服务
     */
    private fun initializeSearchService() {
        viewModelScope.launch {
            try {
                if (!searchService.isReady()) {
                    Log.d(TAG, "搜索服务未初始化，正在初始化...")
                    val result = searchService.initialize()
                    if (result.isFailure) {
                        Log.e(TAG, "搜索服务初始化失败", result.exceptionOrNull())
                        _uiState.update { 
                            it.copy(error = "搜索服务初始化失败: ${result.exceptionOrNull()?.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "初始化搜索服务失败", e)
                _uiState.update { it.copy(error = "初始化失败: ${e.message}") }
            }
        }
    }
    
    /**
     * 加载建议位置（附近热门地点）
     */
    private fun loadSuggestedLocations() {
        viewModelScope.launch {
            try {
                if (!searchService.isReady()) {
                    return@launch
                }
                
                val result = searchService.searchNearby(
                    center = defaultCenter,
                    radiusMeters = 5000.0,
                    limit = 10
                )
                
                result.onSuccess { locations ->
                    _uiState.update { it.copy(suggestedLocations = locations) }
                }.onFailure { error ->
                    Log.e(TAG, "加载建议位置失败", error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载建议位置异常", e)
            }
        }
    }
    
    /**
     * 初始化途径点数据（从 RoutePlanningOverlay 传入）
     */
    fun initialize(
        startLocation: LocationInput,
        waypoints: List<LocationInput>,
        endLocation: LocationInput?
    ) {
        _uiState.update {
            it.copy(
                startLocation = startLocation,
                waypoints = waypoints,
                endLocation = endLocation
            )
        }
    }
    
    /**
     * 处理事件
     */
    fun onEvent(event: WaypointEvent) {
        when (event) {
            is WaypointEvent.SetStartLocation -> {
                _uiState.update { 
                    it.copy(
                        startLocation = event.location,
                        editingIndex = null,
                        searchKeyword = ""
                    )
                }
            }
            
            is WaypointEvent.SetEndLocation -> {
                _uiState.update { 
                    it.copy(
                        endLocation = event.location,
                        editingIndex = null,
                        searchKeyword = ""
                    )
                }
            }
            
            is WaypointEvent.AddWaypoint -> {
                val currentState = _uiState.value
                if (currentState.canAddMore) {
                    _uiState.update {
                        it.copy(
                            waypoints = it.waypoints + LocationInput.CurrentLocation,
                            editingIndex = it.waypoints.size
                        )
                    }
                }
            }
            
            is WaypointEvent.RemoveWaypoint -> {
                _uiState.update {
                    val newWaypoints = it.waypoints.toMutableList()
                    if (event.index in newWaypoints.indices) {
                        newWaypoints.removeAt(event.index)
                        it.copy(waypoints = newWaypoints)
                    } else {
                        it
                    }
                }
            }
            
            is WaypointEvent.SetWaypoint -> {
                _uiState.update {
                    val newWaypoints = it.waypoints.toMutableList()
                    if (event.index in newWaypoints.indices) {
                        newWaypoints[event.index] = event.location
                        it.copy(
                            waypoints = newWaypoints,
                            editingIndex = null,
                            searchKeyword = ""
                        )
                    } else {
                        it
                    }
                }
            }

            is WaypointEvent.MoveLocation -> {
                // 处理拖动排序
                // fromIndex 和 toIndex 是全局索引：0=起点, 1..n=途径点, n+1=终点
                val currentState = _uiState.value
                val allLocations = mutableListOf<LocationInput>()

                // 构建完整列表
                allLocations.add(currentState.startLocation)
                allLocations.addAll(currentState.waypoints)
                currentState.endLocation?.let { allLocations.add(it) }

                // 验证索引有效性
                if (event.fromIndex !in allLocations.indices || event.toIndex !in allLocations.indices) {
                    return
                }

                // 执行移动
                val item = allLocations.removeAt(event.fromIndex)
                allLocations.add(event.toIndex, item)

                // 更新状态
                val newStartLocation = allLocations.firstOrNull() ?: LocationInput.CurrentLocation
                val newEndLocation = if (allLocations.size > 1) allLocations.last() else null
                val newWaypoints = if (allLocations.size > 2) {
                    allLocations.subList(1, allLocations.size - 1)
                } else {
                    emptyList()
                }

                _uiState.update {
                    it.copy(
                        startLocation = newStartLocation,
                        waypoints = newWaypoints,
                        endLocation = newEndLocation
                    )
                }
            }
            
            is WaypointEvent.StartEditing -> {
                _uiState.update { it.copy(editingIndex = event.index, searchKeyword = "") }
            }
            
            is WaypointEvent.EndEditing -> {
                _uiState.update { 
                    it.copy(
                        editingIndex = null,
                        searchKeyword = "",
                        searchResults = emptyList()
                    )
                }
            }
            
            is WaypointEvent.UpdateSearchKeyword -> {
                _uiState.update { it.copy(searchKeyword = event.keyword) }
                
                // 防抖搜索
                if (event.keyword.isNotEmpty()) {
                    searchJob?.cancel()
                    searchJob = viewModelScope.launch {
                        delay(SEARCH_DEBOUNCE_MS)
                        performSearch(event.keyword)
                    }
                } else {
                    searchJob?.cancel()
                    _uiState.update { it.copy(searchResults = emptyList()) }
                }
            }
            
            is WaypointEvent.Search -> {
                performSearch(event.keyword)
            }
            
            is WaypointEvent.SelectSearchResult -> {
                selectLocation(event.poi)
            }
            
            is WaypointEvent.SelectSuggestedLocation -> {
                selectLocation(event.poi)
            }
            
            is WaypointEvent.SelectHistoryLocation -> {
                selectLocation(event.poi)
            }
            
            is WaypointEvent.QuickSelectMyLocation -> {
                selectLocationForEditing(LocationInput.CurrentLocation)
            }
            
            is WaypointEvent.QuickSelectFavorites -> {
                // 打开收藏夹 Overlay
                viewModelScope.launch {
                    _navigationEvent.emit(WaypointNavigationEvent.OpenFavorites)
                }
            }

            is WaypointEvent.QuickSelectMap -> {
                // 已删除地图选点功能
                Log.d(TAG, "快捷选择：地图选点（已删除）")
            }

            is WaypointEvent.QuickSelectHome -> {
                // 设置家地址为阳光小区
                val homeLocation = LocationInput.SpecificLocation(
                    name = "阳光小区",
                    coordinates = LatLng(30.5928, 114.3055), // 使用默认坐标，实际应该搜索获取
                    address = "阳光小区"
                )
                selectLocationForEditing(homeLocation)
            }

            is WaypointEvent.QuickSelectCompany -> {
                // 设置公司地址为字节跳动
                val companyLocation = LocationInput.SpecificLocation(
                    name = "字节跳动",
                    coordinates = LatLng(30.5928, 114.3055), // 使用默认坐标，实际应该搜索获取
                    address = "字节跳动"
                )
                selectLocationForEditing(companyLocation)
            }
            
            is WaypointEvent.Complete -> {
                val currentState = _uiState.value
                viewModelScope.launch {
                    _navigationEvent.emit(
                        WaypointNavigationEvent.Complete(
                            startLocation = currentState.startLocation,
                            waypoints = currentState.waypoints,
                            endLocation = currentState.endLocation
                        )
                    )
                }
            }
            
            is WaypointEvent.ClearError -> {
                _uiState.update { it.copy(error = null) }
            }
        }
    }
    
    /**
     * 执行搜索
     */
    private fun performSearch(keyword: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                if (!searchService.isReady()) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "搜索服务未就绪"
                        )
                    }
                    return@launch
                }
                
                val result = searchService.searchByKeyword(keyword, limit = 20)
                
                result.onSuccess { results ->
                    _uiState.update { 
                        it.copy(
                            searchResults = results,
                            isLoading = false,
                            error = null
                        )
                    }
                }.onFailure { error ->
                    Log.e(TAG, "搜索失败", error)
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "搜索失败: ${error.message}",
                            searchResults = emptyList()
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "搜索异常", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "搜索异常: ${e.message}",
                        searchResults = emptyList()
                    )
                }
            }
        }
    }
    
    /**
     * 选择位置（从搜索结果/建议位置/历史记录）
     */
    private fun selectLocation(poi: PoiResult) {
        val location = LocationInput.SpecificLocation(
            name = poi.name,
            coordinates = poi.location,
            address = poi.address
        )
        selectLocationForEditing(location)
    }
    
    /**
     * 选择位置并更新到当前编辑的字段
     */
    private fun selectLocationForEditing(location: LocationInput) {
        val currentState = _uiState.value
        val editingIndex = currentState.editingIndex ?: return
        
        when (editingIndex) {
            -1 -> onEvent(WaypointEvent.SetStartLocation(location))
            -2 -> onEvent(WaypointEvent.SetEndLocation(location))
            else -> {
                if (editingIndex in currentState.waypoints.indices) {
                    onEvent(WaypointEvent.SetWaypoint(editingIndex, location))
                }
            }
        }
    }
}

