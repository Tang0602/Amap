package com.example.amap_sim.ui.screen.mapcontainer.overlay.nearby

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amap_sim.data.local.OfflineSearchService
import com.example.amap_sim.di.ServiceLocator
import com.example.amap_sim.domain.model.LatLng
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 周边搜索 Overlay ViewModel
 */
class NearbyViewModel : ViewModel() {

    companion object {
        private const val TAG = "NearbyViewModel"
    }

    private val searchService: OfflineSearchService = ServiceLocator.searchService

    private val _uiState = MutableStateFlow(NearbyUiState())
    val uiState: StateFlow<NearbyUiState> = _uiState.asStateFlow()

    // 导航事件
    private val _navigationEvent = MutableSharedFlow<NearbyNavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    /**
     * 处理事件
     */
    fun onEvent(event: NearbyEvent) {
        when (event) {
            is NearbyEvent.SelectCategory -> selectCategory(event.category)
            is NearbyEvent.Search -> performSearch(event.center, event.excludePoiId)
            is NearbyEvent.SelectPoi -> selectPoi(event.poi)
            is NearbyEvent.ClearError -> clearError()
        }
    }

    /**
     * 选择分类
     */
    private fun selectCategory(category: NearbyCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
        // 如果已有中心点，重新搜索（保留excludePoiId）
        _uiState.value.center?.let { center ->
            performSearch(center, _uiState.value.excludePoiId)
        }
    }

    /**
     * 执行搜索
     */
    private fun performSearch(center: LatLng, excludePoiId: String? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    center = center,
                    isLoading = true,
                    error = null,
                    excludePoiId = excludePoiId
                )
            }

            try {
                // 确保搜索服务已初始化
                if (!searchService.isReady()) {
                    val initResult = searchService.initialize()
                    if (initResult.isFailure) {
                        _uiState.update {
                            it.copy(
                                error = "搜索服务不可用",
                                isLoading = false,
                                searchResults = emptyList()
                            )
                        }
                        return@launch
                    }
                }

                val category = _uiState.value.selectedCategory
                val dbCategory = if (category == null || category == NearbyCategory.ALL) {
                    null
                } else {
                    category.dbCategory
                }

                Log.d(TAG, "开始周边搜索: center=(${center.lat}, ${center.lon}), radius=${_uiState.value.radiusMeters}m, category=$dbCategory, excludePoiId=$excludePoiId")

                val result = searchService.searchNearby(
                    center = center,
                    radiusMeters = _uiState.value.radiusMeters,
                    category = dbCategory,
                    limit = 50
                )

                if (result.isSuccess) {
                    val pois = result.getOrNull() ?: emptyList()
                    Log.d(TAG, "搜索返回 ${pois.size} 条结果，excludePoiId=$excludePoiId")

                    // 过滤掉要排除的POI
                    val filteredPois = if (excludePoiId != null) {
                        pois.filter { poi ->
                            val poiIdStr = poi.id.toString()
                            val shouldKeep = poiIdStr != excludePoiId
                            if (!shouldKeep) {
                                Log.d(TAG, "过滤掉POI: id=$poiIdStr, name=${poi.name}")
                            }
                            shouldKeep
                        }
                    } else {
                        pois
                    }

                    Log.d(TAG, "过滤后剩余 ${filteredPois.size} 条结果")

                    _uiState.update {
                        it.copy(
                            searchResults = filteredPois,
                            isLoading = false
                        )
                    }
                    Log.d(TAG, "周边搜索完成，找到 ${pois.size} 条结果，过滤后 ${filteredPois.size} 条")
                    filteredPois.take(5).forEachIndexed { index, poi ->
                        Log.d(TAG, "  结果[$index]: id=${poi.id}, ${poi.name} at (${poi.lat}, ${poi.lon}), distance=${poi.distance}m")
                    }
                } else {
                    val error = result.exceptionOrNull()?.message ?: "搜索失败"
                    _uiState.update {
                        it.copy(
                            error = error,
                            isLoading = false,
                            searchResults = emptyList()
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "周边搜索失败", e)
                _uiState.update {
                    it.copy(
                        error = e.message ?: "搜索失败",
                        isLoading = false,
                        searchResults = emptyList()
                    )
                }
            }
        }
    }

    /**
     * 选择 POI
     */
    private fun selectPoi(poi: com.example.amap_sim.domain.model.PoiResult) {
        viewModelScope.launch {
            _navigationEvent.emit(NearbyNavigationEvent.NavigateToPoiDetail(poi))
        }
    }

    /**
     * 清除错误
     */
    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * 周边搜索导航事件
 */
sealed class NearbyNavigationEvent {
    data class NavigateToPoiDetail(val poi: com.example.amap_sim.domain.model.PoiResult) : NearbyNavigationEvent()
}
