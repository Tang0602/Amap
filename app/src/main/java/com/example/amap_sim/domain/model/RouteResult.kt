package com.example.amap_sim.domain.model

/**
 * 路线规划结果
 */
data class RouteResult(
    /** 总距离（米） */
    val distance: Double,
    /** 总时间（毫秒） */
    val time: Long,
    /** 路线点列表 */
    val points: List<LatLng>,
    /** 导航指令列表 */
    val instructions: List<RouteInstruction>,
    /** 交通方式：car, bike, foot */
    val profile: String
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
     * 获取格式化的时间字符串
     */
    fun getFormattedTime(): String {
        val totalMinutes = (time / 60000).toInt()
        return when {
            totalMinutes < 60 -> "${totalMinutes}分钟"
            else -> {
                val hours = totalMinutes / 60
                val minutes = totalMinutes % 60
                if (minutes > 0) "${hours}小时${minutes}分钟" else "${hours}小时"
            }
        }
    }
    
    /**
     * 获取路线边界框
     */
    fun getBoundingBox(): BoundingBox? {
        if (points.isEmpty()) return null
        var minLat = Double.MAX_VALUE
        var maxLat = Double.MIN_VALUE
        var minLon = Double.MAX_VALUE
        var maxLon = Double.MIN_VALUE
        
        for (point in points) {
            if (point.lat < minLat) minLat = point.lat
            if (point.lat > maxLat) maxLat = point.lat
            if (point.lon < minLon) minLon = point.lon
            if (point.lon > maxLon) maxLon = point.lon
        }
        
        return BoundingBox(
            minLat = minLat,
            maxLat = maxLat,
            minLon = minLon,
            maxLon = maxLon
        )
    }
}

/**
 * 边界框
 */
data class BoundingBox(
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double
) {
    val center: LatLng
        get() = LatLng((minLat + maxLat) / 2, (minLon + maxLon) / 2)
    
    /**
     * 转换为 Mapsforge BoundingBox
     */
    fun toMapsforgeBoundingBox(): org.mapsforge.core.model.BoundingBox {
        return org.mapsforge.core.model.BoundingBox(minLat, minLon, maxLat, maxLon)
    }
}
