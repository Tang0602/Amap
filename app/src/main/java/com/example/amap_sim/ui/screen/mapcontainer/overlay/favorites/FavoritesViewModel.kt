package com.example.amap_sim.ui.screen.mapcontainer.overlay.favorites

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amap_sim.data.local.AgentDataManager
import com.example.amap_sim.data.local.OfflineSearchService
import com.example.amap_sim.di.ServiceLocator
import com.example.amap_sim.domain.model.PoiResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 收藏夹 Overlay ViewModel
 */
class FavoritesViewModel : ViewModel() {

    companion object {
        private const val TAG = "FavoritesViewModel"
    }

    private val searchService: OfflineSearchService = ServiceLocator.searchService
    private val userDataManager = ServiceLocator.userDataManager
    private val agentDataManager: AgentDataManager = ServiceLocator.agentDataManager

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<FavoritesNavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    init {
        loadFavorites()
    }

    /**
     * 加载收藏列表
     *
     * 同时更新 AgentDataManager 的文件5（指令5：告诉我收藏夹收藏了几个地点）
     */
    fun loadFavorites() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
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

                val favoriteIds = userDataManager.getFavorites()
                val favorites = mutableListOf<PoiResult>()

                for (id in favoriteIds) {
                    val poiId = id.toLongOrNull()
                    if (poiId != null) {
                        val result = searchService.getPoiById(poiId)
                        result.getOrNull()?.let { favorites.add(it) }
                    }
                }

                // 更新 Agent 数据文件5（指令5检测用）
                agentDataManager.updateFile5(favorites.size)
                Log.d(TAG, "已更新 Agent 文件5: count=${favorites.size}")

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        favorites = favorites,
                        error = null
                    )
                }
                Log.d(TAG, "加载收藏列表成功: ${favorites.size} 个")
            } catch (e: Exception) {
                Log.e(TAG, "加载收藏列表失败", e)
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
    fun onEvent(event: FavoritesEvent) {
        when (event) {
            FavoritesEvent.NavigateBack -> navigateBack()
            is FavoritesEvent.RemoveFavorite -> removeFavorite(event.poiId)
            is FavoritesEvent.OnFavoriteClick -> navigateToDetail(event.poiId)
        }
    }

    /**
     * 返回
     */
    private fun navigateBack() {
        viewModelScope.launch {
            _navigationEvent.emit(FavoritesNavigationEvent.Back)
        }
    }

    /**
     * 删除收藏
     */
    private fun removeFavorite(poiId: String) {
        viewModelScope.launch {
            userDataManager.removeFavorite(poiId)
            loadFavorites()
        }
    }

    /**
     * 跳转到详情页
     */
    private fun navigateToDetail(poiId: String) {
        viewModelScope.launch {
            _navigationEvent.emit(FavoritesNavigationEvent.NavigateToDetail(poiId))
        }
    }
}
