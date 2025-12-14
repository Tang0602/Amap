package com.example.amap_sim.ui.screen.search

import com.example.amap_sim.domain.model.PoiResult

/**
 * 搜索页 UI 状态
 */
data class SearchUiState(
    /** 搜索关键词 */
    val query: String = "",
    /** 搜索结果列表 */
    val searchResults: List<PoiResult> = emptyList(),
    /** 搜索历史 */
    val searchHistory: List<String> = emptyList(),
    /** 热门分类 */
    val popularCategories: List<CategoryItem> = emptyList(),
    /** 当前选中的分类 */
    val selectedCategory: String? = null,
    /** 是否正在加载 */
    val isLoading: Boolean = false,
    /** 是否显示搜索结果 */
    val showResults: Boolean = false,
    /** 错误信息 */
    val error: String? = null
)

/**
 * 分类项
 */
data class CategoryItem(
    /** 分类 ID */
    val id: String,
    /** 显示名称 */
    val displayName: String,
    /** 图标（Material Icon 名称） */
    val iconName: String,
    /** POI 数量 */
    val count: Int = 0
)

/**
 * 搜索页事件
 */
sealed class SearchEvent {
    /** 更新搜索关键词 */
    data class UpdateQuery(val query: String) : SearchEvent()
    /** 执行搜索 */
    data class Search(val query: String) : SearchEvent()
    /** 选择分类搜索 */
    data class SelectCategory(val category: String) : SearchEvent()
    /** 清除搜索 */
    data object ClearSearch : SearchEvent()
    /** 清除搜索历史 */
    data object ClearHistory : SearchEvent()
    /** 选择搜索历史项 */
    data class SelectHistory(val query: String) : SearchEvent()
    /** 选择 POI */
    data class SelectPoi(val poi: PoiResult) : SearchEvent()
    /** 清除错误 */
    data object ClearError : SearchEvent()
}

/**
 * 默认热门分类
 */
val defaultCategories = listOf(
    CategoryItem("restaurant", "美食", "restaurant"),
    CategoryItem("hotel", "酒店", "hotel"),
    CategoryItem("subway_station", "地铁", "subway"),
    CategoryItem("bus_station", "公交", "directions_bus"),
    CategoryItem("parking", "停车场", "local_parking"),
    CategoryItem("fuel", "加油站", "local_gas_station"),
    CategoryItem("bank", "银行", "account_balance"),
    CategoryItem("hospital", "医院", "local_hospital"),
    CategoryItem("pharmacy", "药店", "local_pharmacy"),
    CategoryItem("supermarket", "超市", "shopping_cart"),
    CategoryItem("cafe", "咖啡", "local_cafe"),
    CategoryItem("attraction", "景点", "attractions")
)
