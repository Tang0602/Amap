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
    val isFavorite: Boolean = false,
    /** 地图更新信息（由 ViewModel 计算） */
    val mapUpdate: DetailMapUpdate? = null,
    /** 是否显示电话确认对话框 */
    val showPhoneDialog: Boolean = false,
    /** 是否显示拨打成功提示 */
    val showCallSuccess: Boolean = false
)

/**
 * 详情地图更新信息
 * 
 * 由 ViewModel 计算，包含需要更新到地图的标记和视图操作
 */
sealed class DetailMapUpdate {
    /** 清除所有标记 */
    data object Clear : DetailMapUpdate()
    
    /** 显示 POI 标记并定位 */
    data class ShowPoi(
        /** 标记点（已转换好的 MarkerData） */
        val marker: com.example.amap_sim.domain.model.MarkerData,
        /** 目标位置 */
        val position: com.example.amap_sim.domain.model.LatLng,
        /** 缩放级别 */
        val zoomLevel: Int = 16
    ) : DetailMapUpdate()
}

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
    /** 确认拨打电话 */
    data object ConfirmCallPhone : DetailEvent()
    /** 取消拨打电话 */
    data object DismissPhoneDialog : DetailEvent()
    /** 关闭拨打成功提示 */
    data object DismissCallSuccess : DetailEvent()
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

