package com.example.amap_sim.ui.screen.mapcontainer.overlay.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amap_sim.data.local.AgentDataManager
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
    private val userDataManager = ServiceLocator.userDataManager
    private val agentDataManager: AgentDataManager = ServiceLocator.agentDataManager

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
                        val favorites = userDataManager.getFavorites()
                        val isFavorite = favorites.contains(currentPoiId)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                poi = poi,
                                error = null,
                                isFavorite = isFavorite,
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
            DetailEvent.CallPhone -> showPhoneDialog()
            DetailEvent.ConfirmCallPhone -> confirmCallPhone()
            DetailEvent.DismissPhoneDialog -> dismissPhoneDialog()
            DetailEvent.DismissCallSuccess -> dismissCallSuccess()
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
     *
     * 同时更新 AgentDataManager 的文件10（指令10：收藏周边最近的餐馆）
     * 和文件5（指令5：告诉我收藏夹收藏了几个地点）
     * 和文件20（指令20：收藏四个特定景点）
     */
    private fun toggleFavorite() {
        viewModelScope.launch {
            val newFavoriteState = !_uiState.value.isFavorite
            _uiState.update { it.copy(isFavorite = newFavoriteState) }

            if (newFavoriteState) {
                userDataManager.addFavorite(currentPoiId)

                // 如果收藏的是餐馆，更新文件10
                val poi = _uiState.value.poi
                if (poi != null && (poi.category.contains("餐") || poi.category.contains("美食"))) {
                    agentDataManager.updateFile10(poi.name, true)
                    Log.d(TAG, "已更新 Agent 文件10: name=${poi.name}, favorited=true")
                }
            } else {
                userDataManager.removeFavorite(currentPoiId)
            }

            // 更新文件5（收藏数量）
            val favoriteCount = userDataManager.getFavorites().size
            agentDataManager.updateFile5(favoriteCount)
            Log.d(TAG, "已更新 Agent 文件5: count=$favoriteCount")

            // 检查指令20：是否收藏了四个特定景点
            checkInstruction20()
        }
    }

    /**
     * 检查指令20：收藏四个特定景点
     *
     * 目标景点：
     * 1. 庚子革命烈士墓墓道牌坊
     * 2. 水生生物博物馆
     * 3. 董必武纪念像
     * 4. 烈士合葬墓
     */
    private suspend fun checkInstruction20() {
        val targetAttractions = listOf(
            "庚子革命烈士墓墓道牌坊",
            "水生生物博物馆",
            "董必武纪念像",
            "烈士合葬墓"
        )

        // 获取所有收藏的 POI ID
        val favoriteIds = userDataManager.getFavorites()

        // 获取所有收藏的 POI 名称
        val favoritedAttractions = mutableListOf<String>()
        for (id in favoriteIds) {
            try {
                val poiId = id.toLongOrNull() ?: continue
                val result = searchService.getPoiById(poiId)
                if (result.isSuccess) {
                    val poi = result.getOrNull()
                    if (poi != null && (poi.category.contains("景点") || poi.category.contains("旅游"))) {
                        favoritedAttractions.add(poi.name)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取收藏 POI 失败: id=$id", e)
            }
        }

        // 筛选出已收藏的目标景点
        val favoritedTargetAttractions = targetAttractions.filter { it in favoritedAttractions }

        // 检查是否所有目标景点都已收藏
        val allFavorited = favoritedTargetAttractions.size == targetAttractions.size

        // 无论收藏了几个，都更新文件，显示已收藏的目标景点
        agentDataManager.updateFile20(favoritedTargetAttractions, allFavorited)
        Log.d(TAG, "已更新 Agent 文件20: 已收藏 ${favoritedTargetAttractions.size}/${targetAttractions.size} 个目标景点")
        Log.d(TAG, "已收藏的景点: $favoritedTargetAttractions")
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
     * 显示电话确认对话框
     */
    private fun showPhoneDialog() {
        _uiState.update { it.copy(showPhoneDialog = true) }
    }

    /**
     * 确认拨打电话
     *
     * 同时更新 AgentDataManager 的文件19（指令19：拨打周边景点排行榜第一的景点电话）
     */
    private fun confirmCallPhone() {
        val poi = _uiState.value.poi
        val phone = poi?.phone
        if (poi != null && !phone.isNullOrEmpty()) {
            // 如果是景点，更新文件19
            if (poi.category.contains("景点") || poi.category.contains("旅游")) {
                agentDataManager.updateFile19(poi.name, phone, true)
                Log.d(TAG, "已更新 Agent 文件19: name=${poi.name}, phone=$phone, called=true")
            }
        }
        _uiState.update { it.copy(showPhoneDialog = false, showCallSuccess = true) }
    }

    /**
     * 取消拨打电话
     */
    private fun dismissPhoneDialog() {
        _uiState.update { it.copy(showPhoneDialog = false) }
    }

    /**
     * 关闭拨打成功提示
     */
    private fun dismissCallSuccess() {
        _uiState.update { it.copy(showCallSuccess = false) }
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

