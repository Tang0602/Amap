package com.example.amap_sim.domain.model

/**
 * POI 搜索结果
 */
data class PoiResult(
    /** POI ID */
    val id: Long,
    /** 名称 */
    val name: String,
    /** 分类 */
    val category: String,
    /** 纬度 */
    val lat: Double,
    /** 经度 */
    val lon: Double,
    /** 地址（可选） */
    val address: String? = null,
    /** 电话（可选） */
    val phone: String? = null,
    /** 距离（米，搜索时计算） */
    val distance: Double? = null,
    /** 开放时间 */
    val openingHours: String? = null,
    /** 景点简介 */
    val description: String? = null,
    /** 行车时间 */
    val travelTime: String? = null,
    /** 评分 */
    val rating: Double? = null
) {
    /**
     * 获取坐标
     */
    val location: LatLng
        get() = LatLng(lat, lon)
    
    /**
     * 获取格式化的距离字符串
     */
    fun getFormattedDistance(): String? {
        return distance?.let {
            when {
                it < 1000 -> "${it.toInt()}米"
                else -> String.format("%.1f公里", it / 1000)
            }
        }
    }
    
    /**
     * 获取分类显示名称
     */
    fun getCategoryDisplayName(): String {
        return PoiCategory.getDisplayName(category)
    }
}

/**
 * POI 分类
 */
object PoiCategory {
    // 餐饮美食
    const val RESTAURANT = "restaurant"
    const val CAFE = "cafe"
    const val FAST_FOOD = "fast_food"
    const val BAR = "bar"
    
    // 购物
    const val SUPERMARKET = "supermarket"
    const val CONVENIENCE = "convenience"
    const val MALL = "mall"
    const val SHOP = "shop"
    
    // 交通
    const val SUBWAY_STATION = "subway_station"
    const val BUS_STATION = "bus_station"
    const val PARKING = "parking"
    const val GAS_STATION = "fuel"
    const val CHARGING_STATION = "charging_station"
    
    // 住宿
    const val HOTEL = "hotel"
    const val HOSTEL = "hostel"
    
    // 金融
    const val BANK = "bank"
    const val ATM = "atm"
    
    // 医疗
    const val HOSPITAL = "hospital"
    const val PHARMACY = "pharmacy"
    const val CLINIC = "clinic"
    
    // 教育
    const val SCHOOL = "school"
    const val UNIVERSITY = "university"
    const val KINDERGARTEN = "kindergarten"
    
    // 娱乐
    const val CINEMA = "cinema"
    const val THEATRE = "theatre"
    const val PARK = "park"
    
    // 生活服务
    const val TOILET = "toilets"
    const val POST_OFFICE = "post_office"
    const val POLICE = "police"
    
    // 景点
    const val ATTRACTION = "attraction"
    const val MUSEUM = "museum"
    const val VIEWPOINT = "viewpoint"
    
    /**
     * 获取分类显示名称
     * 
     * 支持中文 main_category（数据库存储的值）和英文分类常量
     */
    fun getDisplayName(category: String): String {
        return when (category) {
            // 中文 main_category（数据库实际存储值）
            "餐饮" -> "美食"
            "购物" -> "购物"
            "交通" -> "交通"
            "住宿" -> "酒店"
            "医疗" -> "医疗"
            "教育" -> "教育"
            "金融" -> "金融"
            "政务" -> "政务"
            "休闲" -> "休闲"
            "景点" -> "景点"
            "宗教" -> "宗教"
            "生活服务" -> "生活"
            "住宅" -> "住宅"
            "办公" -> "办公"
            // 兼容英文分类常量
            RESTAURANT -> "餐厅"
            CAFE -> "咖啡厅"
            FAST_FOOD -> "快餐"
            BAR -> "酒吧"
            SUPERMARKET -> "超市"
            CONVENIENCE -> "便利店"
            MALL -> "商场"
            SHOP -> "商店"
            SUBWAY_STATION -> "地铁站"
            BUS_STATION -> "公交站"
            PARKING -> "停车场"
            GAS_STATION -> "加油站"
            CHARGING_STATION -> "充电站"
            HOTEL -> "酒店"
            HOSTEL -> "旅馆"
            BANK -> "银行"
            ATM -> "ATM"
            HOSPITAL -> "医院"
            PHARMACY -> "药店"
            CLINIC -> "诊所"
            SCHOOL -> "学校"
            UNIVERSITY -> "大学"
            KINDERGARTEN -> "幼儿园"
            CINEMA -> "电影院"
            THEATRE -> "剧院"
            PARK -> "公园"
            TOILET -> "公厕"
            POST_OFFICE -> "邮局"
            POLICE -> "派出所"
            ATTRACTION -> "景点"
            MUSEUM -> "博物馆"
            VIEWPOINT -> "观景点"
            else -> category
        }
    }
    
    /**
     * 热门分类列表
     */
    val popularCategories = listOf(
        RESTAURANT,
        CAFE,
        SUPERMARKET,
        SUBWAY_STATION,
        BUS_STATION,
        PARKING,
        GAS_STATION,
        HOTEL,
        BANK,
        HOSPITAL,
        PHARMACY
    )
}
