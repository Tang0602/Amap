package com.example.amap_sim.ui.screen.mapcontainer.overlay.detail

import com.example.amap_sim.domain.model.PoiResult

/**
 * POI 详情 Overlay UI 状态
 */
data class DetailUiState(
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
 * POI 详情事件
 */
sealed class DetailEvent {
    /** 返回 */
    data object NavigateBack : DetailEvent()
    /** 导航到该地点 */
    data object NavigateTo : DetailEvent()
    /** 切换收藏状态 */
    data object ToggleFavorite : DetailEvent()
    /** 拨打电话 */
    data object CallPhone : DetailEvent()
    /** 分享 */
    data object Share : DetailEvent()
    /** 在地图上查看 */
    data object ViewOnMap : DetailEvent()
}

/**
 * 详情导航事件
 */
sealed class DetailNavigationEvent {
    /** 返回上一页 */
    data object Back : DetailNavigationEvent()
    /** 跳转到路线规划 */
    data class NavigateToRoute(
        val destLat: Double,
        val destLon: Double,
        val destName: String
    ) : DetailNavigationEvent()
    /** 拨打电话 */
    data class MakePhoneCall(val phone: String) : DetailNavigationEvent()
    /** 分享 */
    data class SharePoi(val name: String, val address: String?) : DetailNavigationEvent()
}

