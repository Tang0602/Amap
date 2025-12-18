package com.example.amap_sim.ui.screen.mapcontainer.overlay.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amap_sim.data.local.OfflineSearchService
import com.example.amap_sim.di.ServiceLocator
import com.example.amap_sim.domain.model.LatLng
import com.example.amap_sim.domain.model.MarkerData
import com.example.amap_sim.domain.model.MarkerType
import com.example.amap_sim.domain.model.PoiResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * POI 详情 Overlay ViewModel
 */
class DetailViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "DetailViewModel"
    }
    
    private val searchService: OfflineSearchService = ServiceLocator.searchService
    
    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()
    
    // 导航事件
    private val _navigationEvent = MutableSharedFlow<DetailNavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()
    
    // 当前 POI ID
    private var currentPoiId: String = ""
    
    init {
        observePoiChanges()
    }
    
    /**
     * 通过 ID 加载 POI 详情
     */
    fun loadPoiById(id: String) {
        if (id != currentPoiId || _uiState.value.poi == null) {
            currentPoiId = id
            loadPoiDetail()
        }
    }
    
    /**
     * 监听 POI 变化，计算地图更新信息
     * 
     * 业务逻辑：将 POI 转换为地图标记，并决定地图定位操作
     */
    private fun observePoiChanges() {
        viewModelScope.launch {
            _uiState
                .map { it.poi }
                .distinctUntilChanged()
                .collect { poi ->
                    updateMapState(poi)
                }
        }
    }
    
    /**
     * 更新地图状态
     * 
     * 根据 POI 详情计算需要的地图更新操作
     */
    private fun updateMapState(poi: PoiResult?) {
        if (poi == null) {
            _uiState.update { it.copy(mapUpdate = DetailMapUpdate.Clear) }
            return
        }
        
        // 将 POI 转换为地图标记
        val marker = MarkerData(
            id = "detail_poi",
            position = LatLng(poi.lat, poi.lon),
            title = poi.name,
            type = MarkerType.POI
        )
        
        val mapUpdate = DetailMapUpdate.ShowPoi(
            marker = marker,
            position = LatLng(poi.lat, poi.lon),
            zoomLevel = 16
        )
        
        _uiState.update { it.copy(mapUpdate = mapUpdate) }
    }
    
    /**
     * 加载 POI 详情
     */
    private fun loadPoiDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // 确保搜索服务已初始化
                if (!searchService.isReady()) {
                    val initResult = searchService.initialize()
                    if (initResult.isFailure) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "搜索服务不可用"
                            )
                        }
                        return@launch
                    }
                }
                
                // 获取 POI 详情
                val id = currentPoiId.toLongOrNull()
                if (id == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "无效的 POI ID"
                        )
                    }
                    return@launch
                }
                
                val result = searchService.getPoiById(id)
                
                if (result.isSuccess) {
                    val poi = result.getOrNull()
                    if (poi != null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                poi = poi,
                                error = null,
                                mapUpdate = null // 由 observePoiChanges 自动计算
                            )
                        }
                        Log.d(TAG, "加载 POI 详情成功: ${poi.name}")
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "未找到该地点"
                            )
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exceptionOrNull()?.message ?: "加载失败"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载 POI 详情失败", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "加载失败"
                    )
                }
            }
        }
    }
    
    /**
     * 处理事件
     */
    fun onEvent(event: DetailEvent) {
        when (event) {
            DetailEvent.NavigateBack -> navigateBack()
            DetailEvent.NavigateTo -> navigateToPoi()
            DetailEvent.ToggleFavorite -> toggleFavorite()
            DetailEvent.CallPhone -> callPhone()
            DetailEvent.Share -> sharePoi()
            DetailEvent.ViewOnMap -> viewOnMap()
        }
    }
    
    /**
     * 返回
     */
    private fun navigateBack() {
        viewModelScope.launch {
            _navigationEvent.emit(DetailNavigationEvent.Back)
        }
    }
    
    /**
     * 导航到该地点
     */
    private fun navigateToPoi() {
        val poi = _uiState.value.poi ?: return
        viewModelScope.launch {
            _navigationEvent.emit(
                DetailNavigationEvent.NavigateToRoute(
                    destLat = poi.lat,
                    destLon = poi.lon,
                    destName = poi.name
                )
            )
        }
    }
    
    /**
     * 切换收藏状态
     */
    private fun toggleFavorite() {
        _uiState.update { it.copy(isFavorite = !it.isFavorite) }
        // TODO: 持久化收藏状态
    }
    
    /**
     * 拨打电话
     */
    private fun callPhone() {
        val phone = _uiState.value.poi?.phone ?: return
        viewModelScope.launch {
            _navigationEvent.emit(DetailNavigationEvent.MakePhoneCall(phone))
        }
    }
    
    /**
     * 分享
     */
    private fun sharePoi() {
        val poi = _uiState.value.poi ?: return
        viewModelScope.launch {
            _navigationEvent.emit(
                DetailNavigationEvent.SharePoi(
                    name = poi.name,
                    address = poi.address
                )
            )
        }
    }
    
    /**
     * 在地图上查看
     */
    private fun viewOnMap() {
        // 在 Overlay 架构下，地图已经在底层显示
    }
}

