package com.example.amap_sim.ui.screen.mapcontainer.overlay.search

import com.example.amap_sim.domain.model.PoiResult

/**
 * 搜索 Overlay UI 状态
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
 * 搜索事件
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
 * 
 * ID 使用数据库中的 main_category 值（中文）
 */
val defaultCategories = listOf(
    CategoryItem("餐饮", "美食", "restaurant"),
    CategoryItem("住宿", "酒店", "hotel"),
    CategoryItem("交通", "交通", "directions_bus"),
    CategoryItem("购物", "购物", "shopping_cart"),
    CategoryItem("金融", "银行", "account_balance"),
    CategoryItem("医疗", "医疗", "local_hospital"),
    CategoryItem("教育", "教育", "school"),
    CategoryItem("休闲", "休闲", "sports_esports"),
    CategoryItem("景点", "景点", "attractions"),
    CategoryItem("政务", "政务", "account_balance"),
    CategoryItem("生活服务", "生活", "home_repair_service"),
    CategoryItem("办公", "办公", "business")
)

