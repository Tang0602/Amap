package com.example.amap_sim.ui.screen.mapcontainer.overlay.favorites

import com.example.amap_sim.domain.model.PoiResult

/**
 * 收藏夹 Overlay UI 状态
 */
data class FavoritesUiState(
    /** 是否加载中 */
    val isLoading: Boolean = true,
    /** 收藏的 POI 列表 */
    val favorites: List<PoiResult> = emptyList(),
    /** 错误信息 */
    val error: String? = null
)

/**
 * 收藏夹事件
 */
sealed class FavoritesEvent {
    /** 返回 */
    data object NavigateBack : FavoritesEvent()
    /** 删除收藏 */
    data class RemoveFavorite(val poiId: String) : FavoritesEvent()
    /** 点击收藏项 */
    data class OnFavoriteClick(val poiId: String) : FavoritesEvent()
}

/**
 * 收藏夹导航事件
 */
sealed class FavoritesNavigationEvent {
    /** 返回上一页 */
    data object Back : FavoritesNavigationEvent()
    /** 跳转到详情页 */
    data class NavigateToDetail(val poiId: String) : FavoritesNavigationEvent()
}
