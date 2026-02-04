package com.example.amap_sim.ui.screen.mapcontainer.overlay.nearby

import com.example.amap_sim.domain.model.LatLng
import com.example.amap_sim.domain.model.PoiResult

/**
 * 周边搜索 Overlay UI 状态
 */
data class NearbyUiState(
    /** 当前中心点 */
    val center: LatLng? = null,
    /** 搜索半径（米） */
    val radiusMeters: Double = 2000.0,
    /** 当前选中的分类 */
    val selectedCategory: NearbyCategory? = null,
    /** 搜索结果列表 */
    val searchResults: List<PoiResult> = emptyList(),
    /** 是否正在加载 */
    val isLoading: Boolean = false,
    /** 错误信息 */
    val error: String? = null,
    /** 要排除的POI ID */
    val excludePoiId: String? = null,
    /** 当前显示的排行榜分类（null表示不显示排行榜） */
    val rankingCategory: NearbyCategory? = null,
    /** 排行榜数据 */
    val rankingList: List<PoiResult> = emptyList()
)

/**
 * 周边分类
 */
enum class NearbyCategory(
    val displayName: String,
    val dbCategory: String,
    val iconName: String
) {
    FOOD("美食", "餐饮", "restaurant"),
    HOTEL("酒店", "住宿", "hotel"),
    ATTRACTION("景点", "景点", "attractions"),
    ALL("全部", "", "apps")
}

/**
 * 周边搜索事件
 */
sealed class NearbyEvent {
    /** 选择分类 */
    data class SelectCategory(val category: NearbyCategory) : NearbyEvent()
    /** 执行搜索 */
    data class Search(val center: LatLng, val excludePoiId: String? = null) : NearbyEvent()
    /** 选择 POI */
    data class SelectPoi(val poi: PoiResult) : NearbyEvent()
    /** 清除错误 */
    data object ClearError : NearbyEvent()
    /** 显示分类排行榜 */
    data class ShowCategoryRanking(val category: NearbyCategory, val center: LatLng) : NearbyEvent()
    /** 隐藏排行榜 */
    data object HideRanking : NearbyEvent()
}
