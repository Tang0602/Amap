package com.example.amap_sim.ui.screen.detail

import com.example.amap_sim.domain.model.PoiResult

/**
 * POI 详情页 UI 状态
 */
data class PoiDetailUiState(
    /** 是否加载中 */
    val isLoading: Boolean = true,
    /** POI 详情 */
    val poi: PoiResult? = null,
    /** 错误信息 */
    val error: String? = null,
    /** 是否已收藏 */
    val isFavorite: Boolean = false
)

/**
 * POI 详情页事件
 */
sealed class PoiDetailEvent {
    /** 返回 */
    data object NavigateBack : PoiDetailEvent()
    /** 导航到该地点 */
    data object NavigateTo : PoiDetailEvent()
    /** 切换收藏状态 */
    data object ToggleFavorite : PoiDetailEvent()
    /** 拨打电话 */
    data object CallPhone : PoiDetailEvent()
    /** 分享 */
    data object Share : PoiDetailEvent()
    /** 在地图上查看 */
    data object ViewOnMap : PoiDetailEvent()
}

/**
 * 导航事件
 */
sealed class PoiDetailNavigationEvent {
    /** 返回上一页 */
    data object Back : PoiDetailNavigationEvent()
    /** 跳转到路线规划页 */
    data class NavigateToRoute(
        val destLat: Double,
        val destLon: Double,
        val destName: String
    ) : PoiDetailNavigationEvent()
    /** 拨打电话 */
    data class MakePhoneCall(val phone: String) : PoiDetailNavigationEvent()
    /** 分享 */
    data class SharePoi(val name: String, val address: String?) : PoiDetailNavigationEvent()
}
