package com.example.amap_sim.ui.screen.detail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amap_sim.data.local.OfflineSearchService
import com.example.amap_sim.di.ServiceLocator
import com.example.amap_sim.ui.navigation.Screen
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * POI 详情页 ViewModel
 */
class PoiDetailViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    companion object {
        private const val TAG = "PoiDetailViewModel"
    }
    
    private val searchService: OfflineSearchService = ServiceLocator.searchService
    
    private val _uiState = MutableStateFlow(PoiDetailUiState())
    val uiState: StateFlow<PoiDetailUiState> = _uiState.asStateFlow()
    
    // 导航事件
    private val _navigationEvent = MutableSharedFlow<PoiDetailNavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()
    
    // 从路由参数获取 POI ID
    private val poiId: String = savedStateHandle.get<String>(Screen.ARG_POI_ID) ?: ""
    
    init {
        loadPoiDetail()
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
                val id = poiId.toLongOrNull()
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
                                error = null
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
    fun onEvent(event: PoiDetailEvent) {
        when (event) {
            PoiDetailEvent.NavigateBack -> navigateBack()
            PoiDetailEvent.NavigateTo -> navigateToPoi()
            PoiDetailEvent.ToggleFavorite -> toggleFavorite()
            PoiDetailEvent.CallPhone -> callPhone()
            PoiDetailEvent.Share -> sharePoi()
            PoiDetailEvent.ViewOnMap -> viewOnMap()
        }
    }
    
    /**
     * 返回
     */
    private fun navigateBack() {
        viewModelScope.launch {
            _navigationEvent.emit(PoiDetailNavigationEvent.Back)
        }
    }
    
    /**
     * 导航到该地点
     */
    private fun navigateToPoi() {
        val poi = _uiState.value.poi ?: return
        viewModelScope.launch {
            _navigationEvent.emit(
                PoiDetailNavigationEvent.NavigateToRoute(
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
            _navigationEvent.emit(PoiDetailNavigationEvent.MakePhoneCall(phone))
        }
    }
    
    /**
     * 分享
     */
    private fun sharePoi() {
        val poi = _uiState.value.poi ?: return
        viewModelScope.launch {
            _navigationEvent.emit(
                PoiDetailNavigationEvent.SharePoi(
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
        // TODO: 跳转到地图页面并定位到该位置
    }
}
