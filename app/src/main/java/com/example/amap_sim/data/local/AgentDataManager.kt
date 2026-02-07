package com.example.amap_sim.data.local

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Agent 指令检测数据管理器
 *
 * 负责管理用于 AI Agent 指令验证的 JSON 文件
 * 29 个指令对应 29 个独立的 JSON 文件
 * 这些文件存储在应用私有目录，可通过 ADB 读取用于验证
 */
class AgentDataManager(private val context: Context) {

    companion object {
        private const val TAG = "AgentDataManager"

        // JSON 文件名 - 29 个指令对应 29 个文件
        const val FILE_1_HIGHEST_SCORE_FOOD = "1_highest_score_food.json"
        const val FILE_2_LAST_NAVIGATION = "2_last_navigation.json"
        const val FILE_3_ACCOUNT_INFO = "3_account_info.json"
        const val FILE_4_NEAREST_HOTEL = "4_nearest_hotel.json"
        const val FILE_5_FAVORITES_COUNT = "5_favorites_count.json"
        const val FILE_6_MODIFY_USERNAME = "6_modify_username.json"
        const val FILE_7_NAVIGATE_TO_DESTINATION = "7_navigate_to_destination.json"
        const val FILE_8_OPEN_BRIGHT_MODE = "8_open_bright_mode.json"
        const val FILE_9_DELETE_RECENT_ROUTE = "9_delete_recent_route.json"
        const val FILE_10_FAVORITE_NEAREST_RESTAURANT = "10_favorite_nearest_restaurant.json"
        const val FILE_11_WALKING_TIME_TO_HOTEL = "11_walking_time_to_hotel.json"
        const val FILE_12_OPENING_HOURS = "12_opening_hours.json"
        const val FILE_13_POI_ADDRESS = "13_poi_address.json"
        const val FILE_14_TOP_FOOD_PHONE = "14_top_food_phone.json"
        const val FILE_15_FIRST_FAVORITE_RESTAURANT = "15_first_favorite_restaurant.json"
        const val FILE_16_WALK_TO_NEAREST_FOOD = "16_walk_to_nearest_food.json"
        const val FILE_17_NAVIGATE_FROM_POI = "17_navigate_from_poi.json"
        const val FILE_18_ADD_WAYPOINT_QUNFANGYUAN = "18_add_waypoint_qunfangyuan.json"
        const val FILE_19_CALL_TOP_ATTRACTION = "19_call_top_attraction.json"
        const val FILE_20_FAVORITE_NEARBY_ATTRACTIONS = "20_favorite_nearby_attractions.json"
        const val FILE_21_NEAREST_FOUR_STAR_HOTEL = "21_nearest_four_star_hotel.json"
        const val FILE_22_PARKING_FEE = "22_parking_fee.json"
        const val FILE_23_FOOD_NEAR_LOCATION = "23_food_near_location.json"
        const val FILE_24_WALKING_TIME_TO_FOOD = "24_walking_time_to_food.json"
        const val FILE_25_CYCLE_TO_FAVORITE = "25_cycle_to_favorite.json"
        const val FILE_26_WALK_TO_RECENT_RESTAURANT = "26_walk_to_recent_restaurant.json"
        const val FILE_27_ADD_FAVORITE_AS_WAYPOINT = "27_add_favorite_as_waypoint.json"
        const val FILE_28_ADD_MULTIPLE_WAYPOINTS = "28_add_multiple_waypoints.json"
        const val FILE_29_MULTI_STOP_NAVIGATION = "29_multi_stop_navigation.json"
    }

    /**
     * 初始化所有 JSON 文件
     * 在应用启动时调用，确保所有 29 个文件都存在并有初始数据
     * @param forceRecreate 是否强制重新创建文件（即使文件已存在）
     */
    suspend fun initializeFiles(forceRecreate: Boolean = false) = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "开始初始化 29 个 Agent 数据文件 (forceRecreate=$forceRecreate)")

            // 初始化所有 29 个文件
            initFile1(forceRecreate)
            initFile2(forceRecreate)
            initFile3(forceRecreate)
            initFile4(forceRecreate)
            initFile5(forceRecreate)
            initFile6(forceRecreate)
            initFile7(forceRecreate)
            initFile8(forceRecreate)
            initFile9(forceRecreate)
            initFile10(forceRecreate)
            initFile11(forceRecreate)
            initFile12(forceRecreate)
            initFile13(forceRecreate)
            initFile14(forceRecreate)
            initFile15(forceRecreate)
            initFile16(forceRecreate)
            initFile17(forceRecreate)
            initFile18(forceRecreate)
            initFile19(forceRecreate)
            initFile20(forceRecreate)
            initFile21(forceRecreate)
            initFile22(forceRecreate)
            initFile23(forceRecreate)
            initFile24(forceRecreate)
            initFile25(forceRecreate)
            initFile26(forceRecreate)
            initFile27(forceRecreate)
            initFile28(forceRecreate)
            initFile29(forceRecreate)

            Log.i(TAG, "29 个 Agent 数据文件初始化完成")
        } catch (e: Exception) {
            Log.e(TAG, "初始化 Agent 数据文件失败", e)
        }
    }

    // ==================== 文件初始化方法 ====================

    /** 1. 告诉我美食排行榜中评分最高的美食 */
    private fun initFile1(forceRecreate: Boolean = false) {
        val file = File(context.filesDir, FILE_1_HIGHEST_SCORE_FOOD)
        if (!file.exists() || forceRecreate) {
            val data = JSONObject().apply {
                put("name", "")
                put("rating", 0.0)
                put("category", "")
                put("address", "")
            }
            file.writeText(data.toString(4))
            Log.d(TAG, "创建文件: $FILE_1_HIGHEST_SCORE_FOOD")
        }
    }

    /** 2. 告诉我最近一次导航去了哪个地点 */
    private fun initFile2(forceRecreate: Boolean = false) {
        val file = File(context.filesDir, FILE_2_LAST_NAVIGATION)
        if (!file.exists() || forceRecreate) {
            val data = JSONObject().apply {
                put("destination", "")
                put("timestamp", 0L)
            }
            file.writeText(data.toString(4))
            Log.d(TAG, "创建文件: $FILE_2_LAST_NAVIGATION")
        }
    }

    /** 3. 告诉我账号的名字和id */
    private fun initFile3(forceRecreate: Boolean = false) {
        val file = File(context.filesDir, FILE_3_ACCOUNT_INFO)
        if (!file.exists() || forceRecreate) {
            val data = JSONObject().apply {
                put("userId", "284834783")
                put("userName", "高德用户")
            }
            file.writeText(data.toString(4))
            Log.d(TAG, "创建文件: $FILE_3_ACCOUNT_INFO")
        }
    }

    /** 4. 告诉我周边最近的酒店名字 */
    private fun initFile4(forceRecreate: Boolean = false) {
        val file = File(context.filesDir, FILE_4_NEAREST_HOTEL)
        if (!file.exists() || forceRecreate) {
            val data = JSONObject().apply {
                put("name", "")
                put("distance", 0.0)
            }
            file.writeText(data.toString(4))
            Log.d(TAG, "创建文件: $FILE_4_NEAREST_HOTEL")
        }
    }

    /** 5. 告诉我收藏夹收藏了几个地点 */
    private fun initFile5(forceRecreate: Boolean = false) {
        val file = File(context.filesDir, FILE_5_FAVORITES_COUNT)
        if (!file.exists() || forceRecreate) {
            val data = JSONObject().apply {
                put("count", 0)
            }
            file.writeText(data.toString(4))
            Log.d(TAG, "创建文件: $FILE_5_FAVORITES_COUNT")
        }
    }

    /** 6. 修改我的名字为123456 */
    private fun initFile6(forceRecreate: Boolean = false) {
        val file = File(context.filesDir, FILE_6_MODIFY_USERNAME)
        if (!file.exists() || forceRecreate) {
            val data = JSONObject().apply {
                put("userName", "")
                put("modified", false)
            }
            file.writeText(data.toString(4))
            Log.d(TAG, "创建文件: $FILE_6_MODIFY_USERNAME")
        }
    }

    /** 7. 导航去M+购物中心 */
    private fun initFile7(forceRecreate: Boolean = false) {
        val file = File(context.filesDir, FILE_7_NAVIGATE_TO_DESTINATION)
        if (!file.exists() || forceRecreate) {
            val data = JSONObject().apply {
                put("destination", "")
                put("started", false)
            }
            file.writeText(data.toString(4))
            Log.d(TAG, "创建文件: $FILE_7_NAVIGATE_TO_DESTINATION")
        }
    }

    /** 8. 打开明亮模式 */
    private fun initFile8(forceRecreate: Boolean = false) {
        val file = File(context.filesDir, FILE_8_OPEN_BRIGHT_MODE)
        if (!file.exists() || forceRecreate) {
            val data = JSONObject().apply {
                put("mode", "")
                put("opened", false)
            }
            file.writeText(data.toString(4))
            Log.d(TAG, "创建文件: $FILE_8_OPEN_BRIGHT_MODE")
        }
    }

    /** 9. 删除导航到M+购物中心的历史记录 */
    private fun initFile9(forceRecreate: Boolean = false) {
        val file = File(context.filesDir, FILE_9_DELETE_RECENT_ROUTE)
        if (!file.exists() || forceRecreate) {
            val data = JSONObject().apply {
                put("deleted", false)
                put("destinationName", "")
                put("routeId", "")
                put("timestamp", 0L)
            }
            file.writeText(data.toString(4))
            Log.d(TAG, "创建文件: $FILE_9_DELETE_RECENT_ROUTE")
        }
    }

    /** 10. 收藏周边最近的餐馆 */
    private fun initFile10(forceRecreate: Boolean = false) {
        val file = File(context.filesDir, FILE_10_FAVORITE_NEAREST_RESTAURANT)
        if (!file.exists() || forceRecreate) {
            val data = JSONObject().apply {
                put("name", "")
                put("favorited", false)
            }
            file.writeText(data.toString(4))
            Log.d(TAG, "创建文件: $FILE_10_FAVORITE_NEAREST_RESTAURANT")
        }
    }

    /** 11. 告诉我步行去最近的酒店需要几分钟 */
    private fun initFile11(forceRecreate: Boolean = false) {
        val file = File(context.filesDir, FILE_11_WALKING_TIME_TO_HOTEL)
        if (!file.exists() || forceRecreate) {
            val data = JSONObject().apply {
                put("hotelName", "")
                put("walkingMinutes", 0)
            }
            file.writeText(data.toString(4))
            Log.d(TAG, "创建文件: $FILE_11_WALKING_TIME_TO_HOTEL")
        }
    }

    /** 12. 告诉我八七会议会址纪念馆的开放时间有几个小时 */
    private fun initFile12(forceRecreate: Boolean = false) {
        val file = File(context.filesDir, FILE_12_OPENING_HOURS)
        if (!file.exists() || forceRecreate) {
            val data = JSONObject().apply {
                put("poiName", "")
                put("openingHours", 0)
            }
            file.writeText(data.toString(4))
            Log.d(TAG, "创建文件: $FILE_12_OPENING_HOURS")
        }
    }

    /** 13. 告诉我M+购物中心的地址 */
    private fun initFile13(forceRecreate: Boolean = false) {
        val file = File(context.filesDir, FILE_13_POI_ADDRESS)
        if (!file.exists() || forceRecreate) {
            val data = JSONObject().apply {
                put("poiName", "")
                put("address", "")
            }
            file.writeText(data.toString(4))
            Log.d(TAG, "创建文件: $FILE_13_POI_ADDRESS")
        }
    }

    /** 14. 告诉我美食排行榜第一的地点的电话号码 */
    private fun initFile14(forceRecreate: Boolean = false) {
        val file = File(context.filesDir, FILE_14_TOP_FOOD_PHONE)
        if (!file.exists() || forceRecreate) {
            val data = JSONObject().apply {
                put("name", "")
                put("phone", "")
            }
            file.writeText(data.toString(4))
            Log.d(TAG, "创建文件: $FILE_14_TOP_FOOD_PHONE")
        }
    }

    /** 15. 告诉我收藏的第一行饭店的名称 */
    private fun initFile15(forceRecreate: Boolean = false) {
        val file = File(context.filesDir, FILE_15_FIRST_FAVORITE_RESTAURANT)
        if (!file.exists() || forceRecreate) {
            val data = JSONObject().apply {
                put("name", "")
            }
            file.writeText(data.toString(4))
            Log.d(TAG, "创建文件: $FILE_15_FIRST_FAVORITE_RESTAURANT")
        }
    }

    /** 16. 步行导航去周边最近的美食店 */
    private fun initFile16(forceRecreate: Boolean = false) {
        val file = File(context.filesDir, FILE_16_WALK_TO_NEAREST_FOOD)
        if (!file.exists() || forceRecreate) {
            val data = JSONObject().apply {
                put("destination", "")
                put("mode", "")
                put("started", false)
            }
            file.writeText(data.toString(4))
            Log.d(TAG, "创建文件: $FILE_16_WALK_TO_NEAREST_FOOD")
        }
    }

    /** 17. 从M+购物中心导航到我的位置 */
    private fun initFile17(forceRecreate: Boolean = false) {
        val file = File(context.filesDir, FILE_17_NAVIGATE_FROM_POI)
        if (!file.exists() || forceRecreate) {
            val data = JSONObject().apply {
                put("from", "")
                put("to", "")
                put("started", false)
            }
            file.writeText(data.toString(4))
            Log.d(TAG, "创建文件: $FILE_17_NAVIGATE_FROM_POI")
        }
    }

    /** 18. 在导航去滨江饭店的路线中添加途经点群芳园 */
    private fun initFile18(forceRecreate: Boolean = false) {
        val file = File(context.filesDir, FILE_18_ADD_WAYPOINT_QUNFANGYUAN)
        if (!file.exists() || forceRecreate) {
            val data = JSONObject().apply {
                put("destination", "")
                put("waypoint", "")
                put("added", false)
            }
            file.writeText(data.toString(4))
            Log.d(TAG, "创建文件: $FILE_18_ADD_WAYPOINT_QUNFANGYUAN")
        }
    }

    /** 19. 拨打周边景点排行榜第一的景点电话 */
    private fun initFile19(forceRecreate: Boolean = false) {
        val file = File(context.filesDir, FILE_19_CALL_TOP_ATTRACTION)
        if (!file.exists() || forceRecreate) {
            val data = JSONObject().apply {
                put("name", "")
                put("phone", "")
                put("called", false)
            }
            file.writeText(data.toString(4))
            Log.d(TAG, "创建文件: $FILE_19_CALL_TOP_ATTRACTION")
        }
    }

    /** 20. 收藏所有周边1km以内（包括1km）的所有景点 */
    private fun initFile20(forceRecreate: Boolean = false) {
        val file = File(context.filesDir, FILE_20_FAVORITE_NEARBY_ATTRACTIONS)
        if (!file.exists() || forceRecreate) {
            val data = JSONObject().apply {
                put("count", 0)
                put("favorited", false)
            }
            file.writeText(data.toString(4))
            Log.d(TAG, "创建文件: $FILE_20_FAVORITE_NEARBY_ATTRACTIONS")
        }
    }

    /** 21. 告诉我最近的一家四星级酒店名字（根据简介看） */
    private fun initFile21(forceRecreate: Boolean = false) {
        val file = File(context.filesDir, FILE_21_NEAREST_FOUR_STAR_HOTEL)
        if (!file.exists() || forceRecreate) {
            val data = JSONObject().apply {
                put("name", "")
                put("rating", 0)
            }
            file.writeText(data.toString(4))
            Log.d(TAG, "创建文件: $FILE_21_NEAREST_FOUR_STAR_HOTEL")
        }
    }

    /** 22. 告诉我台北路公共停车场停车收费标准 */
    private fun initFile22(forceRecreate: Boolean = false) {
        val file = File(context.filesDir, FILE_22_PARKING_FEE)
        if (!file.exists() || forceRecreate) {
            val data = JSONObject().apply {
                put("poiName", "")
                put("parkingFee", "")
            }
            file.writeText(data.toString(4))
            Log.d(TAG, "创建文件: $FILE_22_PARKING_FEE")
        }
    }

    /** 23. 告诉我江汉大学（汉口校区）周边美食排行榜第一名是什么 */
    private fun initFile23(forceRecreate: Boolean = false) {
        val file = File(context.filesDir, FILE_23_FOOD_NEAR_LOCATION)
        if (!file.exists() || forceRecreate) {
            val data = JSONObject().apply {
                put("location", "")
                put("topFood", "")
            }
            file.writeText(data.toString(4))
            Log.d(TAG, "创建文件: $FILE_23_FOOD_NEAR_LOCATION")
        }
    }

    /** 24. 告诉我现在的位置，距离武汉市公安局（江岸分局）的周边美食排行榜第一名驾车需要几分钟 */
    private fun initFile24(forceRecreate: Boolean = false) {
        val file = File(context.filesDir, FILE_24_WALKING_TIME_TO_FOOD)
        if (!file.exists() || forceRecreate) {
            val data = JSONObject().apply {
                put("location", "")
                put("topFood", "")
                put("drivingMinutes", 0)
            }
            file.writeText(data.toString(4))
            Log.d(TAG, "创建文件: $FILE_24_WALKING_TIME_TO_FOOD")
        }
    }

    /** 25. 骑行导航去我收藏的饭店中最近的一家 */
    private fun initFile25(forceRecreate: Boolean = false) {
        val file = File(context.filesDir, FILE_25_CYCLE_TO_FAVORITE)
        if (!file.exists() || forceRecreate) {
            val data = JSONObject().apply {
                put("destination", "")
                put("mode", "")
                put("started", false)
            }
            file.writeText(data.toString(4))
            Log.d(TAG, "创建文件: $FILE_25_CYCLE_TO_FAVORITE")
        }
    }

    /** 26. 步行导航去我最近去过的一家餐馆 */
    private fun initFile26(forceRecreate: Boolean = false) {
        val file = File(context.filesDir, FILE_26_WALK_TO_RECENT_RESTAURANT)
        if (!file.exists() || forceRecreate) {
            val data = JSONObject().apply {
                put("destination", "")
                put("mode", "")
                put("started", false)
            }
            file.writeText(data.toString(4))
            Log.d(TAG, "创建文件: $FILE_26_WALK_TO_RECENT_RESTAURANT")
        }
    }

    /** 27. 在导航去滨江饭店的路线中添加收藏中第一个地点作为途径点 */
    private fun initFile27(forceRecreate: Boolean = false) {
        val file = File(context.filesDir, FILE_27_ADD_FAVORITE_AS_WAYPOINT)
        if (!file.exists() || forceRecreate) {
            val data = JSONObject().apply {
                put("destination", "")
                put("waypoint", "")
                put("added", false)
            }
            file.writeText(data.toString(4))
            Log.d(TAG, "创建文件: $FILE_27_ADD_FAVORITE_AS_WAYPOINT")
        }
    }

    /** 28. 在导航去M+购物中心的路线中添加第一个途经点芦苇滩，第二个途经点武汉市人民政府 */
    private fun initFile28(forceRecreate: Boolean = false) {
        val file = File(context.filesDir, FILE_28_ADD_MULTIPLE_WAYPOINTS)
        if (!file.exists() || forceRecreate) {
            val data = JSONObject().apply {
                put("destination", "")
                put("waypoints", JSONArray())
                put("added", false)
            }
            file.writeText(data.toString(4))
            Log.d(TAG, "创建文件: $FILE_28_ADD_MULTIPLE_WAYPOINTS")
        }
    }

    /** 29. 完成路线导航：M+购物中心到武汉市人民政府再到芦苇滩最后到我的位置的路线导航 */
    private fun initFile29(forceRecreate: Boolean = false) {
        val file = File(context.filesDir, FILE_29_MULTI_STOP_NAVIGATION)
        if (!file.exists() || forceRecreate) {
            val data = JSONObject().apply {
                put("stops", JSONArray())
                put("completed", false)
            }
            file.writeText(data.toString(4))
            Log.d(TAG, "创建文件: $FILE_29_MULTI_STOP_NAVIGATION")
        }
    }

    // ==================== 文件更新方法 ====================

    /** 更新文件1：美食排行榜评分最高的美食 */
    fun updateFile1(name: String, rating: Double, category: String, address: String) {
        val file = File(context.filesDir, FILE_1_HIGHEST_SCORE_FOOD)
        val data = JSONObject().apply {
            put("name", name)
            put("rating", rating)
            put("category", category)
            put("address", address)
        }
        file.writeText(data.toString(4))
        Log.d(TAG, "更新文件: $FILE_1_HIGHEST_SCORE_FOOD")
    }

    /** 更新文件2：最近一次导航目的地 */
    fun updateFile2(destination: String, timestamp: Long) {
        val file = File(context.filesDir, FILE_2_LAST_NAVIGATION)
        val data = JSONObject().apply {
            put("destination", destination)
            put("timestamp", timestamp)
        }
        file.writeText(data.toString(4))
        Log.d(TAG, "更新文件: $FILE_2_LAST_NAVIGATION")
    }

    /** 更新文件3：账号信息 */
    fun updateFile3(userId: String, userName: String) {
        val file = File(context.filesDir, FILE_3_ACCOUNT_INFO)
        val data = JSONObject().apply {
            put("userId", userId)
            put("userName", userName)
        }
        file.writeText(data.toString(4))
        Log.d(TAG, "更新文件: $FILE_3_ACCOUNT_INFO")
    }

    /** 更新文件4：周边最近的酒店 */
    fun updateFile4(name: String, distance: Double) {
        val file = File(context.filesDir, FILE_4_NEAREST_HOTEL)
        val data = JSONObject().apply {
            put("name", name)
            put("distance", distance)
        }
        file.writeText(data.toString(4))
        Log.d(TAG, "更新文件: $FILE_4_NEAREST_HOTEL")
    }

    /** 更新文件5：收藏夹地点数量 */
    fun updateFile5(count: Int) {
        val file = File(context.filesDir, FILE_5_FAVORITES_COUNT)
        val data = JSONObject().apply {
            put("count", count)
        }
        file.writeText(data.toString(4))
        Log.d(TAG, "更新文件: $FILE_5_FAVORITES_COUNT")
    }

    /** 更新文件6：修改用户名 */
    fun updateFile6(userName: String, modified: Boolean) {
        val file = File(context.filesDir, FILE_6_MODIFY_USERNAME)
        val data = JSONObject().apply {
            put("userName", userName)
            put("modified", modified)
        }
        file.writeText(data.toString(4))
        Log.d(TAG, "更新文件: $FILE_6_MODIFY_USERNAME")
    }

    /** 更新文件7：步行导航去M+购物中心 */
    fun updateFile7(destination: String, started: Boolean) {
        val file = File(context.filesDir, FILE_7_NAVIGATE_TO_DESTINATION)
        val data = JSONObject().apply {
            put("destination", destination)
            put("started", started)
        }
        file.writeText(data.toString(4))
        Log.d(TAG, "更新文件: $FILE_7_NAVIGATE_TO_DESTINATION")
    }

    /** 更新文件8：打开夜间模式 */
    fun updateFile8(mode: String, opened: Boolean) {
        val file = File(context.filesDir, FILE_8_OPEN_BRIGHT_MODE)
        val data = JSONObject().apply {
            put("mode", mode)
            put("opened", opened)
        }
        file.writeText(data.toString(4))
        Log.d(TAG, "更新文件: $FILE_8_OPEN_BRIGHT_MODE")
    }

    /** 更新文件9：删除导航到M+购物中心的历史记录 */
    fun updateFile9(deleted: Boolean, destinationName: String, routeId: String, timestamp: Long) {
        val file = File(context.filesDir, FILE_9_DELETE_RECENT_ROUTE)
        val data = JSONObject().apply {
            put("deleted", deleted)
            put("destinationName", destinationName)
            put("routeId", routeId)
            put("timestamp", timestamp)
        }
        file.writeText(data.toString(4))
        Log.d(TAG, "更新文件: $FILE_9_DELETE_RECENT_ROUTE")
    }

    /** 更新文件10：收藏周边最近的餐馆 */
    fun updateFile10(name: String, favorited: Boolean) {
        val file = File(context.filesDir, FILE_10_FAVORITE_NEAREST_RESTAURANT)
        val data = JSONObject().apply {
            put("name", name)
            put("favorited", favorited)
        }
        file.writeText(data.toString(4))
        Log.d(TAG, "更新文件: $FILE_10_FAVORITE_NEAREST_RESTAURANT")
    }

    /** 更新文件11：步行到最近酒店时间 */
    fun updateFile11(hotelName: String, walkingMinutes: Int) {
        val file = File(context.filesDir, FILE_11_WALKING_TIME_TO_HOTEL)
        val data = JSONObject().apply {
            put("hotelName", hotelName)
            put("walkingMinutes", walkingMinutes)
        }
        file.writeText(data.toString(4))
        Log.d(TAG, "更新文件: $FILE_11_WALKING_TIME_TO_HOTEL")
    }

    /** 更新文件12：开放时间 */
    fun updateFile12(poiName: String, openingHours: Int) {
        val file = File(context.filesDir, FILE_12_OPENING_HOURS)
        val data = JSONObject().apply {
            put("poiName", poiName)
            put("openingHours", openingHours)
        }
        file.writeText(data.toString(4))
        Log.d(TAG, "更新文件: $FILE_12_OPENING_HOURS")
    }

    /** 更新文件13：地点地址 */
    fun updateFile13(poiName: String, address: String) {
        val file = File(context.filesDir, FILE_13_POI_ADDRESS)
        val data = JSONObject().apply {
            put("poiName", poiName)
            put("address", address)
        }
        file.writeText(data.toString(4))
        Log.d(TAG, "更新文件: $FILE_13_POI_ADDRESS")
    }

    /** 更新文件14：美食排行榜第一的电话 */
    fun updateFile14(name: String, phone: String) {
        val file = File(context.filesDir, FILE_14_TOP_FOOD_PHONE)
        val data = JSONObject().apply {
            put("name", name)
            put("phone", phone)
        }
        file.writeText(data.toString(4))
        Log.d(TAG, "更新文件: $FILE_14_TOP_FOOD_PHONE")
    }

    /** 更新文件15：收藏的第一行饭店名称 */
    fun updateFile15(name: String) {
        val file = File(context.filesDir, FILE_15_FIRST_FAVORITE_RESTAURANT)
        val data = JSONObject().apply {
            put("name", name)
        }
        file.writeText(data.toString(4))
        Log.d(TAG, "更新文件: $FILE_15_FIRST_FAVORITE_RESTAURANT")
    }

    /** 更新文件16：步行导航去最近美食店 */
    fun updateFile16(destination: String, mode: String, started: Boolean) {
        val file = File(context.filesDir, FILE_16_WALK_TO_NEAREST_FOOD)
        val data = JSONObject().apply {
            put("destination", destination)
            put("mode", mode)
            put("started", started)
        }
        file.writeText(data.toString(4))
        Log.d(TAG, "更新文件: $FILE_16_WALK_TO_NEAREST_FOOD")
    }

    /** 更新文件17：从M+导航到我的位置 */
    fun updateFile17(from: String, to: String, started: Boolean) {
        val file = File(context.filesDir, FILE_17_NAVIGATE_FROM_POI)
        val data = JSONObject().apply {
            put("from", from)
            put("to", to)
            put("started", started)
        }
        file.writeText(data.toString(4))
        Log.d(TAG, "更新文件: $FILE_17_NAVIGATE_FROM_POI")
    }

    /** 更新文件18：添加途经点群芳园 */
    fun updateFile18(destination: String, waypoint: String, added: Boolean) {
        val file = File(context.filesDir, FILE_18_ADD_WAYPOINT_QUNFANGYUAN)
        val data = JSONObject().apply {
            put("destination", destination)
            put("waypoint", waypoint)
            put("added", added)
        }
        file.writeText(data.toString(4))
        Log.d(TAG, "更新文件: $FILE_18_ADD_WAYPOINT_QUNFANGYUAN")
    }

    /** 更新文件19：拨打景点电话 */
    fun updateFile19(name: String, phone: String, called: Boolean) {
        val file = File(context.filesDir, FILE_19_CALL_TOP_ATTRACTION)
        val data = JSONObject().apply {
            put("name", name)
            put("phone", phone)
            put("called", called)
        }
        file.writeText(data.toString(4))
        Log.d(TAG, "更新文件: $FILE_19_CALL_TOP_ATTRACTION")
    }

    /** 更新文件20：收藏周边景点 */
    fun updateFile20(count: Int, favorited: Boolean) {
        val file = File(context.filesDir, FILE_20_FAVORITE_NEARBY_ATTRACTIONS)
        val data = JSONObject().apply {
            put("count", count)
            put("favorited", favorited)
        }
        file.writeText(data.toString(4))
        Log.d(TAG, "更新文件: $FILE_20_FAVORITE_NEARBY_ATTRACTIONS")
    }

    /** 更新文件21：最近的四星级酒店 */
    fun updateFile21(name: String, rating: Int) {
        val file = File(context.filesDir, FILE_21_NEAREST_FOUR_STAR_HOTEL)
        val data = JSONObject().apply {
            put("name", name)
            put("rating", rating)
        }
        file.writeText(data.toString(4))
        Log.d(TAG, "更新文件: $FILE_21_NEAREST_FOUR_STAR_HOTEL")
    }

    /** 更新文件22：停车费用 */
    fun updateFile22(poiName: String, parkingFee: String) {
        val file = File(context.filesDir, FILE_22_PARKING_FEE)
        val data = JSONObject().apply {
            put("poiName", poiName)
            put("parkingFee", parkingFee)
        }
        file.writeText(data.toString(4))
        Log.d(TAG, "更新文件: $FILE_22_PARKING_FEE")
    }

    /** 更新文件23：周边美食排行榜第一 */
    fun updateFile23(location: String, topFood: String) {
        val file = File(context.filesDir, FILE_23_FOOD_NEAR_LOCATION)
        val data = JSONObject().apply {
            put("location", location)
            put("topFood", topFood)
        }
        file.writeText(data.toString(4))
        Log.d(TAG, "更新文件: $FILE_23_FOOD_NEAR_LOCATION")
    }

    /** 更新文件24：驾车到美食店时间 */
    fun updateFile24(location: String, topFood: String, drivingMinutes: Int) {
        val file = File(context.filesDir, FILE_24_WALKING_TIME_TO_FOOD)
        val data = JSONObject().apply {
            put("location", location)
            put("topFood", topFood)
            put("drivingMinutes", drivingMinutes)
        }
        file.writeText(data.toString(4))
        Log.d(TAG, "更新文件: $FILE_24_WALKING_TIME_TO_FOOD")
    }

    /** 更新文件25：骑行导航去收藏饭店 */
    fun updateFile25(destination: String, mode: String, started: Boolean) {
        val file = File(context.filesDir, FILE_25_CYCLE_TO_FAVORITE)
        val data = JSONObject().apply {
            put("destination", destination)
            put("mode", mode)
            put("started", started)
        }
        file.writeText(data.toString(4))
        Log.d(TAG, "更新文件: $FILE_25_CYCLE_TO_FAVORITE")
    }

    /** 更新文件26：步行导航去最近去过的餐馆 */
    fun updateFile26(destination: String, mode: String, started: Boolean) {
        val file = File(context.filesDir, FILE_26_WALK_TO_RECENT_RESTAURANT)
        val data = JSONObject().apply {
            put("destination", destination)
            put("mode", mode)
            put("started", started)
        }
        file.writeText(data.toString(4))
        Log.d(TAG, "更新文件: $FILE_26_WALK_TO_RECENT_RESTAURANT")
    }

    /** 更新文件27：添加收藏地点作为途经点 */
    fun updateFile27(destination: String, waypoint: String, added: Boolean) {
        val file = File(context.filesDir, FILE_27_ADD_FAVORITE_AS_WAYPOINT)
        val data = JSONObject().apply {
            put("destination", destination)
            put("waypoint", waypoint)
            put("added", added)
        }
        file.writeText(data.toString(4))
        Log.d(TAG, "更新文件: $FILE_27_ADD_FAVORITE_AS_WAYPOINT")
    }

    /** 更新文件28：添加多个途经点 */
    fun updateFile28(destination: String, waypoints: List<String>, added: Boolean) {
        val file = File(context.filesDir, FILE_28_ADD_MULTIPLE_WAYPOINTS)
        val waypointsArray = JSONArray()
        waypoints.forEach { waypointsArray.put(it) }
        val data = JSONObject().apply {
            put("destination", destination)
            put("waypoints", waypointsArray)
            put("added", added)
        }
        file.writeText(data.toString(4))
        Log.d(TAG, "更新文件: $FILE_28_ADD_MULTIPLE_WAYPOINTS")
    }

    /** 更新文件29：多站点路线导航 */
    fun updateFile29(stops: List<String>, completed: Boolean) {
        val file = File(context.filesDir, FILE_29_MULTI_STOP_NAVIGATION)
        val stopsArray = JSONArray()
        stops.forEach { stopsArray.put(it) }
        val data = JSONObject().apply {
            put("stops", stopsArray)
            put("completed", completed)
        }
        file.writeText(data.toString(4))
        Log.d(TAG, "更新文件: $FILE_29_MULTI_STOP_NAVIGATION")
    }
}
