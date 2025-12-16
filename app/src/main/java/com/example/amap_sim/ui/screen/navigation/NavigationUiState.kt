package com.example.amap_sim.ui.screen.navigation

import com.example.amap_sim.domain.model.LatLng
import com.example.amap_sim.domain.model.RouteInstruction
import com.example.amap_sim.domain.model.RouteResult

/**
 * 导航页 UI 状态
 */
data class NavigationUiState(
    /** 路线结果 */
    val routeResult: RouteResult? = null,
    /** 当前位置 */
    val currentLocation: LatLng = LatLng.WUHAN_CENTER,
    /** 当前指令索引 */
    val currentInstructionIndex: Int = 0,
    /** 当前指令 */
    val currentInstruction: RouteInstruction? = null,
    /** 下一条指令 */
    val nextInstruction: RouteInstruction? = null,
    /** 到下一个指令点的距离（米） */
    val distanceToNextInstruction: Double = 0.0,
    /** 剩余总距离（米） */
    val remainingDistance: Double = 0.0,
    /** 剩余时间（毫秒） */
    val remainingTime: Long = 0L,
    /** 当前速度（km/h） */
    val currentSpeed: Double = 0.0,
    /** 导航状态 */
    val navigationState: NavigationState = NavigationState.NOT_STARTED,
    /** 地图缩放级别 */
    val zoomLevel: Int = 17,
    /** 地图是否跟随当前位置 */
    val isFollowingUser: Boolean = true,
    /** 是否显示全览模式 */
    val isOverviewMode: Boolean = false,
    /** 错误信息 */
    val error: String? = null,
    /** 已完成的路线点索引（用于绘制已走过的路线） */
    val completedPointIndex: Int = 0,
    /** 预计到达时间 */
    val estimatedArrivalTime: String? = null
) {
    /**
     * 获取格式化的剩余距离
     */
    fun getFormattedRemainingDistance(): String {
        return when {
            remainingDistance < 1000 -> "${remainingDistance.toInt()}米"
            else -> String.format("%.1f公里", remainingDistance / 1000)
        }
    }
    
    /**
     * 获取格式化的剩余时间
     */
    fun getFormattedRemainingTime(): String {
        val totalMinutes = (remainingTime / 60000).toInt()
        return when {
            totalMinutes < 1 -> "即将到达"
            totalMinutes < 60 -> "${totalMinutes}分钟"
            else -> {
                val hours = totalMinutes / 60
                val minutes = totalMinutes % 60
                if (minutes > 0) "${hours}小时${minutes}分" else "${hours}小时"
            }
        }
    }
    
    /**
     * 获取格式化的到下一指令的距离
     */
    fun getFormattedDistanceToNext(): String {
        return when {
            distanceToNextInstruction < 50 -> "即将"
            distanceToNextInstruction < 1000 -> "${distanceToNextInstruction.toInt()}米后"
            else -> String.format("%.1f公里后", distanceToNextInstruction / 1000)
        }
    }
    
    /**
     * 是否正在导航中
     */
    val isNavigating: Boolean
        get() = navigationState == NavigationState.NAVIGATING
    
    /**
     * 是否已到达目的地
     */
    val hasArrived: Boolean
        get() = navigationState == NavigationState.ARRIVED
}

/**
 * 导航状态
 */
enum class NavigationState {
    /** 未开始 */
    NOT_STARTED,
    /** 导航中 */
    NAVIGATING,
    /** 已暂停 */
    PAUSED,
    /** 已到达 */
    ARRIVED,
    /** 偏离路线 */
    OFF_ROUTE,
    /** 错误 */
    ERROR
}

/**
 * 导航页事件
 */
sealed class NavigationEvent {
    /** 开始导航 */
    data object StartNavigation : NavigationEvent()
    /** 暂停导航 */
    data object PauseNavigation : NavigationEvent()
    /** 恢复导航 */
    data object ResumeNavigation : NavigationEvent()
    /** 结束导航 */
    data object StopNavigation : NavigationEvent()
    /** 切换跟随模式 */
    data object ToggleFollowMode : NavigationEvent()
    /** 切换全览模式 */
    data object ToggleOverviewMode : NavigationEvent()
    /** 返回上一页 */
    data object NavigateBack : NavigationEvent()
    /** 重新规划路线 */
    data object Reroute : NavigationEvent()
    /** 点击地图 */
    data class MapClick(val position: LatLng) : NavigationEvent()
    /** 模拟位置更新（仅用于测试） */
    data class SimulateLocationUpdate(val position: LatLng) : NavigationEvent()
    /** 清除错误 */
    data object ClearError : NavigationEvent()
}

/**
 * 导航页导航事件（页面跳转）
 */
sealed class NavigationNavigationEvent {
    /** 返回上一页 */
    data object Back : NavigationNavigationEvent()
    /** 导航结束，返回首页 */
    data object NavigationFinished : NavigationNavigationEvent()
}

