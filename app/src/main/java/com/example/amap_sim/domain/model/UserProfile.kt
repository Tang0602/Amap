package com.example.amap_sim.domain.model

/**
 * 用户资料
 */
data class UserProfile(
    /** 用户 ID */
    val userId: String = "284834783",
    /** 用户名 */
    val userName: String = "高德用户",
    /** 头像路径 */
    val avatarPath: String = ""
)

/**
 * 历史路线记录
 */
data class RouteHistory(
    /** 记录 ID */
    val id: String,
    /** 起点名称 */
    val startName: String,
    /** 起点坐标 */
    val startLocation: LatLng,
    /** 终点名称 */
    val endName: String,
    /** 终点坐标 */
    val endLocation: LatLng,
    /** 导航时间戳 */
    val timestamp: Long,
    /** 路线距离（米） */
    val distance: Double = 0.0,
    /** 预计时长（秒） */
    val duration: Int = 0
)
