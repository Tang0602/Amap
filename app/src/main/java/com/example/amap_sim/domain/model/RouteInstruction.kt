package com.example.amap_sim.domain.model

/**
 * 导航指令
 */
data class RouteInstruction(
    /** 指令文字描述 */
    val text: String,
    /** 距离（米） */
    val distance: Double,
    /** 时间（毫秒） */
    val time: Long,
    /** 指令类型 */
    val sign: InstructionSign,
    /** 指令点位置 */
    val location: LatLng,
    /** 道路名称 */
    val streetName: String?,
    /** 转弯角度（-180 ~ 180）*/
    val turnAngle: Double?
) {
    /**
     * 获取格式化的距离字符串
     */
    fun getFormattedDistance(): String {
        return when {
            distance < 1000 -> "${distance.toInt()}米"
            else -> String.format("%.1f公里", distance / 1000)
        }
    }
    
    /**
     * 获取指令图标描述（用于 UI 展示）
     */
    fun getIconDescription(): String {
        return sign.description
    }
}

/**
 * 指令类型
 */
enum class InstructionSign(val code: Int, val description: String) {
    // 直行类
    CONTINUE(0, "继续直行"),
    STRAIGHT(0, "直行"),
    
    // 左转类
    SLIGHT_LEFT(-1, "稍向左转"),
    LEFT(-2, "左转"),
    SHARP_LEFT(-3, "向左急转"),
    
    // 右转类
    SLIGHT_RIGHT(1, "稍向右转"),
    RIGHT(2, "右转"),
    SHARP_RIGHT(3, "向右急转"),
    
    // 掉头
    U_TURN(6, "掉头"),
    U_TURN_LEFT(-6, "向左掉头"),
    U_TURN_RIGHT(6, "向右掉头"),
    
    // 环岛
    ROUNDABOUT(-7, "进入环岛"),
    LEAVE_ROUNDABOUT(7, "驶出环岛"),
    
    // 起终点
    DEPART(4, "出发"),
    ARRIVE(5, "到达目的地"),
    
    // 途经点
    REACHED_VIA(8, "到达途经点"),
    
    // 其他
    KEEP_LEFT(-4, "靠左行驶"),
    KEEP_RIGHT(4, "靠右行驶"),
    
    UNKNOWN(-99, "继续");
    
    companion object {
        /**
         * 从 GraphHopper sign 代码转换
         */
        fun fromGraphHopperSign(sign: Int): InstructionSign {
            return when (sign) {
                0 -> CONTINUE
                -1 -> SLIGHT_LEFT
                -2 -> LEFT
                -3 -> SHARP_LEFT
                1 -> SLIGHT_RIGHT
                2 -> RIGHT
                3 -> SHARP_RIGHT
                -7 -> ROUNDABOUT
                7 -> LEAVE_ROUNDABOUT
                4 -> DEPART
                5 -> ARRIVE
                6 -> U_TURN_RIGHT
                -6 -> U_TURN_LEFT
                8 -> REACHED_VIA
                -4 -> KEEP_LEFT
                else -> UNKNOWN
            }
        }
    }
}
