package com.example.amap_sim.domain.model

/**
 * 经纬度坐标
 */
data class LatLng(
    val lat: Double,
    val lon: Double
) {
    companion object {
        /**
         * 武汉市中心（黄鹤楼附近）
         */
        val WUHAN_CENTER = LatLng(30.5433, 114.3416)
        
        /**
         * 武汉市边界（用于验证坐标是否在武汉范围内）
         */
        val WUHAN_BOUNDS_MIN = LatLng(29.97, 113.70)
        val WUHAN_BOUNDS_MAX = LatLng(31.36, 115.08)
    }
    
    /**
     * 检查坐标是否在武汉市范围内
     */
    fun isInWuhanBounds(): Boolean {
        return lat >= WUHAN_BOUNDS_MIN.lat && lat <= WUHAN_BOUNDS_MAX.lat &&
               lon >= WUHAN_BOUNDS_MIN.lon && lon <= WUHAN_BOUNDS_MAX.lon
    }
    
    /**
     * 转换为 Mapsforge LatLong
     */
    fun toMapsforgeLatLong(): org.mapsforge.core.model.LatLong {
        return org.mapsforge.core.model.LatLong(lat, lon)
    }
    
    /**
     * 计算到另一个点的距离（米）
     * 使用 Haversine 公式
     */
    fun distanceTo(other: LatLng): Double {
        val earthRadius = 6371000.0 // 地球半径（米）
        val dLat = Math.toRadians(other.lat - lat)
        val dLon = Math.toRadians(other.lon - lon)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(other.lat)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }
    
    override fun toString(): String {
        return "($lat, $lon)"
    }
}

/**
 * 从 Mapsforge LatLong 转换
 */
fun org.mapsforge.core.model.LatLong.toLatLng(): LatLng {
    return LatLng(this.latitude, this.longitude)
}
