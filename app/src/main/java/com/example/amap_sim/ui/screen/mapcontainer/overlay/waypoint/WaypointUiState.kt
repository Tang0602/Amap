package com.example.amap_sim.ui.screen.mapcontainer.overlay.waypoint

import com.example.amap_sim.domain.model.PoiResult
import com.example.amap_sim.ui.screen.mapcontainer.overlay.route.LocationInput

/**
 * 途径点管理 Overlay UI 状态
 */
data class WaypointUiState(
    /** 起点位置 */
    val startLocation: LocationInput = LocationInput.CurrentLocation,
    /** 途径点列表 */
    val waypoints: List<LocationInput> = emptyList(),
    /** 终点位置 */
    val endLocation: LocationInput? = null,
    /** 当前编辑的字段索引（-1=起点, -2=终点, >=0=途径点索引） */
    val editingIndex: Int? = null,
    /** 搜索关键词（当前输入框的文字） */
    val searchKeyword: String = "",
    /** 搜索结果列表（当有搜索关键词时显示） */
    val searchResults: List<PoiResult> = emptyList(),
    /** 建议位置列表（当无搜索关键词时显示） */
    val suggestedLocations: List<PoiResult> = emptyList(),
    /** 历史记录列表（当无搜索关键词时显示） */
    val historyLocations: List<PoiResult> = emptyList(),
    /** 是否正在搜索 */
    val isLoading: Boolean = false,
    /** 错误信息 */
    val error: String? = null,
    /** 最多可添加的途径点数量 */
    val maxWaypoints: Int = 15
) {
    /**
     * 剩余可添加的途径点数量
     */
    val remainingWaypoints: Int
        get() = maxWaypoints - waypoints.size
    
    /**
     * 是否可以添加更多途径点
     */
    val canAddMore: Boolean
        get() = remainingWaypoints > 0
    
    /**
     * 是否有搜索关键词
     */
    val hasSearchKeyword: Boolean
        get() = searchKeyword.isNotEmpty()
    
    /**
     * 是否显示搜索结果
     */
    val showSearchResults: Boolean
        get() = hasSearchKeyword && searchResults.isNotEmpty()
    
    /**
     * 是否显示建议位置
     */
    val showSuggestions: Boolean
        get() = !hasSearchKeyword && suggestedLocations.isNotEmpty()
    
    /**
     * 是否显示历史记录
     */
    val showHistory: Boolean
        get() = !hasSearchKeyword && historyLocations.isNotEmpty()
    
    /**
     * 是否显示快捷选择按钮
     */
    val showQuickButtons: Boolean
        get() = !hasSearchKeyword
}

/**
 * 途径点事件
 */
sealed class WaypointEvent {
    /** 设置起点 */
    data class SetStartLocation(val location: LocationInput) : WaypointEvent()
    /** 设置终点 */
    data class SetEndLocation(val location: LocationInput) : WaypointEvent()
    /** 添加途径点 */
    data object AddWaypoint : WaypointEvent()
    /** 删除途径点 */
    data class RemoveWaypoint(val index: Int) : WaypointEvent()
    /** 设置途径点 */
    data class SetWaypoint(val index: Int, val location: LocationInput) : WaypointEvent()
    /** 开始编辑字段 */
    data class StartEditing(val index: Int) : WaypointEvent()
    /** 结束编辑 */
    data object EndEditing : WaypointEvent()
    /** 更新搜索关键词 */
    data class UpdateSearchKeyword(val keyword: String) : WaypointEvent()
    /** 执行搜索 */
    data class Search(val keyword: String) : WaypointEvent()
    /** 选择搜索结果 */
    data class SelectSearchResult(val poi: PoiResult) : WaypointEvent()
    /** 选择建议位置 */
    data class SelectSuggestedLocation(val poi: PoiResult) : WaypointEvent()
    /** 选择历史记录 */
    data class SelectHistoryLocation(val poi: PoiResult) : WaypointEvent()
    /** 快捷选择：我的位置 */
    data object QuickSelectMyLocation : WaypointEvent()
    /** 快捷选择：收藏的点 */
    data object QuickSelectFavorites : WaypointEvent()
    /** 快捷选择：地图选点 */
    data object QuickSelectMap : WaypointEvent()
    /** 快捷选择：家 */
    data object QuickSelectHome : WaypointEvent()
    /** 快捷选择：公司 */
    data object QuickSelectCompany : WaypointEvent()
    /** 完成 */
    data object Complete : WaypointEvent()
    /** 清除错误 */
    data object ClearError : WaypointEvent()
}

/**
 * 途径点导航事件
 */
sealed class WaypointNavigationEvent {
    /** 完成并返回 */
    data class Complete(
        val startLocation: LocationInput,
        val waypoints: List<LocationInput>,
        val endLocation: LocationInput?
    ) : WaypointNavigationEvent()
}

