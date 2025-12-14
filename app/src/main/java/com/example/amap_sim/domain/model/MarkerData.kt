package com.example.amap_sim.domain.model

import androidx.annotation.DrawableRes

/**
 * 地图标记点数据
 */
data class MarkerData(
    /** 唯一标识 */
    val id: String,
    /** 位置 */
    val position: LatLng,
    /** 标题 */
    val title: String? = null,
    /** 描述 */
    val snippet: String? = null,
    /** 标记类型 */
    val type: MarkerType = MarkerType.DEFAULT,
    /** 自定义图标资源 ID */
    @DrawableRes val iconRes: Int? = null,
    /** 锚点 X (0.0-1.0) */
    val anchorX: Float = 0.5f,
    /** 锚点 Y (0.0-1.0) */
    val anchorY: Float = 1.0f,
    /** 是否可拖拽 */
    val draggable: Boolean = false,
    /** 附加数据 */
    val extra: Any? = null
)

/**
 * 标记点类型
 */
enum class MarkerType {
    /** 默认标记 */
    DEFAULT,
    /** 起点 */
    START,
    /** 终点 */
    END,
    /** 途经点 */
    WAYPOINT,
    /** 当前位置 */
    CURRENT_LOCATION,
    /** POI 标记 */
    POI,
    /** 搜索结果 */
    SEARCH_RESULT
}
