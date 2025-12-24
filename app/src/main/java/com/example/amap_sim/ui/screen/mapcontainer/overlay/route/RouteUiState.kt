package com.example.amap_sim.ui.screen.mapcontainer.overlay.route

import com.example.amap_sim.domain.model.LatLng
import com.example.amap_sim.domain.model.RouteResult

/**
 * 路线规划 Overlay UI 状态
 */
data class RouteUiState(
    /** 起点位置 */
    val startLocation: LocationInput = LocationInput.CurrentLocation,
    /** 途径点列表 */
    val waypoints: List<LocationInput> = emptyList(),
    /** 终点位置 */
    val endLocation: LocationInput? = null,
    /** 当前选中的交通方式 */
    val selectedProfile: TravelProfile = TravelProfile.CAR,
    /** 路线结果 */
    val routeResult: RouteResult? = null,
    /** 是否正在计算路线 */
    val isLoading: Boolean = false,
    /** 错误信息 */
    val error: String? = null,
    /** 是否展开详细指令 */
    val showInstructions: Boolean = false,
    /** 起点输入框是否获得焦点 */
    val isStartInputFocused: Boolean = false,
    /** 终点输入框是否获得焦点 */
    val isEndInputFocused: Boolean = false,
    /** 地图更新信息（由 ViewModel 计算） */
    val mapUpdate: RouteMapUpdate? = null
)

/**
 * 位置输入
 */
sealed class LocationInput {
    /** 当前位置 */
    data object CurrentLocation : LocationInput() {
        override fun toString(): String = "我的位置"
    }
    
    /** 具体位置 */
    data class SpecificLocation(
        val name: String,
        val coordinates: LatLng,
        val address: String? = null
    ) : LocationInput() {
        override fun toString(): String = name
    }
    
    /**
     * 获取显示名称
     */
    fun getDisplayName(): String {
        return when (this) {
            is CurrentLocation -> "我的位置"
            is SpecificLocation -> name
        }
    }
    
    /**
     * 获取经纬度（当前位置返回武汉市中心作为默认）
     */
    fun getLatLng(): LatLng {
        return when (this) {
            is CurrentLocation -> LatLng.WUHAN_CENTER
            is SpecificLocation -> coordinates
        }
    }
}

/**
 * 交通方式
 */
enum class TravelProfile(
    val profileId: String,
    val displayName: String,
    val iconDescription: String
) {
    CAR("car", "驾车", "汽车"),
    BIKE("bike", "骑行", "自行车"),
    FOOT("foot", "步行", "步行");
    
    companion object {
        fun fromProfileId(id: String): TravelProfile {
            return entries.find { it.profileId == id } ?: CAR
        }
    }
}

/**
 * 路线地图更新信息
 * 
 * 由 ViewModel 计算，包含需要更新到地图的标记和路线
 */
sealed class RouteMapUpdate {
    /** 清除所有标记和路线 */
    data object Clear : RouteMapUpdate()
    
    /** 显示起点终点标记和路线 */
    data class ShowRoute(
        /** 起点标记（已转换好的 MarkerData） */
        val startMarker: com.example.amap_sim.domain.model.MarkerData,
        /** 途径点标记列表 */
        val waypointMarkers: List<com.example.amap_sim.domain.model.MarkerData>,
        /** 终点标记（已转换好的 MarkerData） */
        val endMarker: com.example.amap_sim.domain.model.MarkerData,
        /** 路线结果 */
        val routeResult: RouteResult
    ) : RouteMapUpdate()
    
    /** 只显示起点终点标记（无路线） */
    data class ShowMarkersOnly(
        /** 起点标记 */
        val startMarker: com.example.amap_sim.domain.model.MarkerData,
        /** 途径点标记列表 */
        val waypointMarkers: List<com.example.amap_sim.domain.model.MarkerData>,
        /** 终点标记 */
        val endMarker: com.example.amap_sim.domain.model.MarkerData
    ) : RouteMapUpdate()
}

/**
 * 路线规划事件
 */
sealed class RouteEvent {
    /** 返回 */
    data object NavigateBack : RouteEvent()
    /** 切换交通方式 */
    data class SelectProfile(val profile: TravelProfile) : RouteEvent()
    /** 交换起点终点 */
    data object SwapLocations : RouteEvent()
    /** 设置起点 */
    data class SetStartLocation(val location: LocationInput) : RouteEvent()
    /** 设置终点 */
    data class SetEndLocation(val location: LocationInput) : RouteEvent()
    /** 更新途径点 */
    data class UpdateWaypoints(
        val startLocation: LocationInput,
        val waypoints: List<LocationInput>,
        val endLocation: LocationInput?
    ) : RouteEvent()
    /** 计算路线 */
    data object CalculateRoute : RouteEvent()
    /** 切换显示指令详情 */
    data object ToggleInstructions : RouteEvent()
    /** 开始导航 */
    data object StartNavigation : RouteEvent()
    /** 点击起点输入框 */
    data object ClickStartInput : RouteEvent()
    /** 点击终点输入框 */
    data object ClickEndInput : RouteEvent()
    /** 清除错误 */
    data object ClearError : RouteEvent()
}

/**
 * 路线导航事件
 */
sealed class RouteNavigationEvent {
    /** 返回上一页 */
    data object Back : RouteNavigationEvent()
    /** 跳转到搜索页选择起点 */
    data object SelectStartFromSearch : RouteNavigationEvent()
    /** 跳转到搜索页选择终点 */
    data object SelectEndFromSearch : RouteNavigationEvent()
    /** 开始导航 */
    data class StartNavigation(val routeResult: RouteResult) : RouteNavigationEvent()
}

