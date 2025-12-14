package com.example.amap_sim.ui.screen.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amap_sim.data.local.OfflineSearchService
import com.example.amap_sim.di.ServiceLocator
import com.example.amap_sim.domain.model.LatLng
import com.example.amap_sim.domain.model.PoiResult
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
 * 搜索页 ViewModel
 */
class SearchViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "SearchViewModel"
        private const val SEARCH_DEBOUNCE_MS = 300L
        private const val MAX_HISTORY_SIZE = 10
    }
    
    private val searchService: OfflineSearchService = ServiceLocator.searchService
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    // 导航事件
    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()
    
    // 搜索任务
    private var searchJob: Job? = null
    
    // 默认中心点（武汉）
    private val defaultCenter = LatLng(30.5928, 114.3055)
    
    // 搜索历史（内存中）
    private val searchHistoryList = mutableListOf<String>()
    
    init {
        loadInitialData()
    }
    
    /**
     * 加载初始数据
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                // 加载热门分类
                val categories = loadPopularCategories()
                _uiState.update { it.copy(popularCategories = categories) }
            } catch (e: Exception) {
                Log.e(TAG, "加载初始数据失败", e)
            }
        }
    }
    
    /**
     * 加载热门分类
     */
    private suspend fun loadPopularCategories(): List<CategoryItem> {
        return try {
            val result = searchService.getPopularCategories()
            if (result.isSuccess) {
                val dbCategories = result.getOrNull() ?: emptyList()
                // 合并数据库分类和默认分类
                defaultCategories.map { default ->
                    val dbCategory = dbCategories.find { it.first == default.id }
                    default.copy(count = dbCategory?.second ?: 0)
                }
            } else {
                defaultCategories
            }
        } catch (e: Exception) {
            Log.w(TAG, "加载热门分类失败，使用默认分类", e)
            defaultCategories
        }
    }
    
    /**
     * 处理事件
     */
    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.UpdateQuery -> updateQuery(event.query)
            is SearchEvent.Search -> performSearch(event.query)
            is SearchEvent.SelectCategory -> searchByCategory(event.category)
            is SearchEvent.ClearSearch -> clearSearch()
            is SearchEvent.ClearHistory -> clearHistory()
            is SearchEvent.SelectHistory -> selectHistory(event.query)
            is SearchEvent.SelectPoi -> selectPoi(event.poi)
            is SearchEvent.ClearError -> clearError()
        }
    }
    
    /**
     * 更新搜索关键词（带防抖）
     */
    private fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query, selectedCategory = null) }
        
        // 取消之前的搜索任务
        searchJob?.cancel()
        
        if (query.isBlank()) {
            _uiState.update { 
                it.copy(
                    showResults = false, 
                    searchResults = emptyList(),
                    error = null
                ) 
            }
            return
        }
        
        // 防抖搜索
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            performSearchInternal(query)
        }
    }
    
    /**
     * 执行搜索
     */
    private fun performSearch(query: String) {
        if (query.isBlank()) return
        
        // 取消之前的搜索任务
        searchJob?.cancel()
        
        // 添加到搜索历史
        addToHistory(query)
        
        searchJob = viewModelScope.launch {
            performSearchInternal(query)
        }
    }
    
    /**
     * 内部搜索实现
     */
    private suspend fun performSearchInternal(query: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        try {
            val result = searchService.searchByKeyword(
                keyword = query,
                limit = 50,
                center = defaultCenter
            )
            
            if (result.isSuccess) {
                val pois = result.getOrNull() ?: emptyList()
                _uiState.update { 
                    it.copy(
                        searchResults = pois,
                        showResults = true,
                        isLoading = false
                    ) 
                }
                Log.d(TAG, "搜索 '$query' 完成，找到 ${pois.size} 条结果")
            } else {
                val error = result.exceptionOrNull()?.message ?: "搜索失败"
                _uiState.update { 
                    it.copy(
                        error = error,
                        isLoading = false,
                        showResults = true,
                        searchResults = emptyList()
                    ) 
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "搜索失败", e)
            _uiState.update { 
                it.copy(
                    error = e.message ?: "搜索失败",
                    isLoading = false,
                    showResults = true,
                    searchResults = emptyList()
                ) 
            }
        }
    }
    
    /**
     * 分类搜索
     */
    private fun searchByCategory(category: String) {
        // 取消之前的搜索任务
        searchJob?.cancel()
        
        _uiState.update { 
            it.copy(
                selectedCategory = category,
                query = "",
                isLoading = true,
                error = null
            ) 
        }
        
        searchJob = viewModelScope.launch {
            try {
                val result = searchService.searchByCategory(
                    category = category,
                    center = defaultCenter,
                    limit = 50
                )
                
                if (result.isSuccess) {
                    val pois = result.getOrNull() ?: emptyList()
                    _uiState.update { 
                        it.copy(
                            searchResults = pois,
                            showResults = true,
                            isLoading = false
                        ) 
                    }
                    Log.d(TAG, "分类搜索 '$category' 完成，找到 ${pois.size} 条结果")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "搜索失败"
                    _uiState.update { 
                        it.copy(
                            error = error,
                            isLoading = false,
                            showResults = true,
                            searchResults = emptyList()
                        ) 
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "分类搜索失败", e)
                _uiState.update { 
                    it.copy(
                        error = e.message ?: "搜索失败",
                        isLoading = false,
                        showResults = true,
                        searchResults = emptyList()
                    ) 
                }
            }
        }
    }
    
    /**
     * 清除搜索
     */
    private fun clearSearch() {
        searchJob?.cancel()
        _uiState.update { 
            it.copy(
                query = "",
                searchResults = emptyList(),
                showResults = false,
                selectedCategory = null,
                error = null
            ) 
        }
    }
    
    /**
     * 添加到搜索历史
     */
    private fun addToHistory(query: String) {
        if (query.isBlank()) return
        
        // 移除已存在的相同项
        searchHistoryList.remove(query)
        // 添加到头部
        searchHistoryList.add(0, query)
        // 限制大小
        while (searchHistoryList.size > MAX_HISTORY_SIZE) {
            searchHistoryList.removeLast()
        }
        // 更新 UI
        _uiState.update { it.copy(searchHistory = searchHistoryList.toList()) }
    }
    
    /**
     * 清除搜索历史
     */
    private fun clearHistory() {
        searchHistoryList.clear()
        _uiState.update { it.copy(searchHistory = emptyList()) }
    }
    
    /**
     * 选择历史项
     */
    private fun selectHistory(query: String) {
        _uiState.update { it.copy(query = query) }
        performSearch(query)
    }
    
    /**
     * 选择 POI
     */
    private fun selectPoi(poi: PoiResult) {
        viewModelScope.launch {
            _navigationEvent.emit(NavigationEvent.NavigateToPoiDetail(poi))
        }
    }
    
    /**
     * 清除错误
     */
    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }
}

/**
 * 导航事件
 */
sealed class NavigationEvent {
    data class NavigateToPoiDetail(val poi: PoiResult) : NavigationEvent()
    data class NavigateToRoute(val destination: PoiResult) : NavigationEvent()
}
