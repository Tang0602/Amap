package com.example.amap_sim.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.amap_sim.domain.model.LatLng
import com.example.amap_sim.domain.model.RouteHistory
import com.example.amap_sim.domain.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 用户数据管理
 */
class UserDataManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "user_data"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_AVATAR_PATH = "avatar_path"
        private const val KEY_ROUTE_HISTORY = "route_history"
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_THEME = "theme"
    }

    /**
     * 获取用户资料
     */
    suspend fun getUserProfile(): UserProfile = withContext(Dispatchers.IO) {
        val userId = prefs.getString(KEY_USER_ID, null)
        val userName = prefs.getString(KEY_USER_NAME, null)
        val avatarPath = prefs.getString(KEY_AVATAR_PATH, null)

        // 如果存储的是空值，清除并使用默认值
        if (userId.isNullOrEmpty() || userName.isNullOrEmpty()) {
            prefs.edit().apply {
                remove(KEY_USER_ID)
                remove(KEY_USER_NAME)
                remove(KEY_AVATAR_PATH)
                apply()
            }
        }

        UserProfile(
            userId = if (userId.isNullOrEmpty()) "284834783" else userId,
            userName = if (userName.isNullOrEmpty()) "高德用户" else userName,
            avatarPath = avatarPath ?: ""
        )
    }

    /**
     * 更新用户资料
     */
    suspend fun updateUserProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        prefs.edit().apply {
            putString(KEY_USER_ID, profile.userId)
            putString(KEY_USER_NAME, profile.userName)
            putString(KEY_AVATAR_PATH, profile.avatarPath)
            apply()
        }
    }

    /**
     * 获取历史路线
     * 如果为空，返回预设的历史记录
     */
    suspend fun getRouteHistory(): List<RouteHistory> = withContext(Dispatchers.IO) {
        try {
            val json = prefs.getString(KEY_ROUTE_HISTORY, null)
            if (json.isNullOrEmpty()) {
                // 返回预设的历史记录：从我的位置到Lilly Cafe，时间为2020年1月1日
                return@withContext listOf(
                    RouteHistory(
                        id = "preset_history_1",
                        startName = "我的位置",
                        startLocation = LatLng(30.5928, 114.3055),
                        endName = "Lilly Cafe",
                        endLocation = LatLng(30.5928, 114.3155), // 假设坐标
                        timestamp = 1577836800000L, // 2020年1月1日
                        distance = 1000.0,
                        duration = 300
                    )
                )
            }
            val jsonArray = JSONArray(json)
            (0 until jsonArray.length()).map { i ->
                val obj = jsonArray.getJSONObject(i)
                RouteHistory(
                    id = obj.getString("id"),
                    startName = obj.getString("startName"),
                    startLocation = LatLng(obj.getDouble("startLat"), obj.getDouble("startLon")),
                    endName = obj.getString("endName"),
                    endLocation = LatLng(obj.getDouble("endLat"), obj.getDouble("endLon")),
                    timestamp = obj.getLong("timestamp"),
                    distance = obj.optDouble("distance", 0.0),
                    duration = obj.optInt("duration", 0)
                )
            }
        } catch (e: Exception) {
            // 出错时返回预设值
            listOf(
                RouteHistory(
                    id = "preset_history_1",
                    startName = "我的位置",
                    startLocation = LatLng(30.5928, 114.3055),
                    endName = "Lilly Cafe",
                    endLocation = LatLng(30.5928, 114.3155),
                    timestamp = 1577836800000L,
                    distance = 1000.0,
                    duration = 300
                )
            )
        }
    }

    /**
     * 添加历史路线
     */
    suspend fun addRouteHistory(history: RouteHistory) = withContext(Dispatchers.IO) {
        try {
            val list = getRouteHistory().toMutableList()
            list.add(0, history)
            if (list.size > 50) {
                list.removeAt(list.size - 1)
            }
            saveRouteHistory(list)
        } catch (e: Exception) {
            // 忽略错误
        }
    }

    /**
     * 删除历史路线
     */
    suspend fun deleteRouteHistory(id: String) = withContext(Dispatchers.IO) {
        try {
            val list = getRouteHistory().toMutableList()
            list.removeAll { it.id == id }
            saveRouteHistory(list)
        } catch (e: Exception) {
            // 忽略错误
        }
    }

    /**
     * 保存历史路线
     */
    private fun saveRouteHistory(list: List<RouteHistory>) {
        val jsonArray = JSONArray()
        list.forEach { history ->
            val obj = JSONObject().apply {
                put("id", history.id)
                put("startName", history.startName)
                put("startLat", history.startLocation.lat)
                put("startLon", history.startLocation.lon)
                put("endName", history.endName)
                put("endLat", history.endLocation.lat)
                put("endLon", history.endLocation.lon)
                put("timestamp", history.timestamp)
                put("distance", history.distance)
                put("duration", history.duration)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_ROUTE_HISTORY, jsonArray.toString()).apply()
    }

    /**
     * 获取收藏夹
     * 如果为空，返回预设的收藏标记
     */
    suspend fun getFavorites(): List<String> = withContext(Dispatchers.IO) {
        try {
            val json = prefs.getString(KEY_FAVORITES, null)
            if (json.isNullOrEmpty()) {
                // 返回预设的收藏标记（特殊标记，由FavoritesViewModel处理）
                return@withContext listOf("PRESET:亚朵酒店", "PRESET:肖记公安牛肉鱼杂馆", "PRESET:小胡鸭")
            }
            val jsonArray = JSONArray(json)
            (0 until jsonArray.length()).map { i -> jsonArray.getString(i) }
        } catch (e: Exception) {
            // 出错时返回预设值
            listOf("PRESET:亚朵酒店", "PRESET:肖记公安牛肉鱼杂馆", "PRESET:小胡鸭")
        }
    }

    /**
     * 添加收藏
     */
    suspend fun addFavorite(poiId: String) = withContext(Dispatchers.IO) {
        try {
            val list = getFavorites().toMutableList()
            if (!list.contains(poiId)) {
                list.add(0, poiId)
                saveFavorites(list)
            }
        } catch (e: Exception) {
            // 忽略错误
        }
    }

    /**
     * 删除收藏
     */
    suspend fun removeFavorite(poiId: String) = withContext(Dispatchers.IO) {
        try {
            val list = getFavorites().toMutableList()
            list.remove(poiId)
            saveFavorites(list)
        } catch (e: Exception) {
            // 忽略错误
        }
    }

    /**
     * 保存收藏夹
     */
    private fun saveFavorites(list: List<String>) {
        val jsonArray = JSONArray()
        list.forEach { jsonArray.put(it) }
        prefs.edit().putString(KEY_FAVORITES, jsonArray.toString()).apply()
    }

    /**
     * 获取主题设置
     */
    suspend fun getTheme(): AppTheme = withContext(Dispatchers.IO) {
        val themeName = prefs.getString(KEY_THEME, AppTheme.BRIGHT.name) ?: AppTheme.BRIGHT.name
        try {
            AppTheme.valueOf(themeName)
        } catch (e: Exception) {
            AppTheme.BRIGHT
        }
    }

    /**
     * 保存主题设置
     */
    suspend fun saveTheme(theme: AppTheme) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_THEME, theme.name).apply()
    }

    /**
     * 初始化预设数据（收藏和历史记录）
     * 这些数据是内设的，无法从app上改变
     *
     * @param forceReset 是否强制重新初始化（清除现有数据）
     */
    suspend fun initializePresetData(searchService: OfflineSearchService, forceReset: Boolean = false) = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("UserDataManager", "开始初始化预设数据, forceReset=$forceReset")

            // 检查是否已经初始化过
            val isInitialized = prefs.getBoolean("preset_data_initialized", false)
            if (isInitialized && !forceReset) {
                android.util.Log.d("UserDataManager", "预设数据已初始化，跳过")
                return@withContext
            }

            // 如果强制重置，清除标记
            if (forceReset) {
                prefs.edit().putBoolean("preset_data_initialized", false).apply()
                android.util.Log.d("UserDataManager", "清除初始化标记")
            }

            // 确保搜索服务已初始化
            if (!searchService.isReady()) {
                android.util.Log.d("UserDataManager", "搜索服务未就绪，正在初始化...")
                val result = searchService.initialize()
                if (result.isFailure) {
                    android.util.Log.e("UserDataManager", "搜索服务初始化失败", result.exceptionOrNull())
                    return@withContext
                }
                android.util.Log.d("UserDataManager", "搜索服务初始化成功")
            }

            // 等待一下确保搜索服务完全就绪
            kotlinx.coroutines.delay(500)

            // 1. 初始化收藏数据
            android.util.Log.d("UserDataManager", "开始初始化收藏数据")
            val favoriteNames = listOf("亚朵酒店", "肖记公安牛肉鱼杂馆", "小胡鸭")
            val favoriteIds = mutableListOf<String>()
            val center = LatLng(30.5928, 114.3055)

            for (name in favoriteNames) {
                android.util.Log.d("UserDataManager", "搜索收藏: $name")
                val result = searchService.searchByKeyword(name, 20, center)
                val pois = result.getOrNull()
                android.util.Log.d("UserDataManager", "搜索结果数量: ${pois?.size ?: 0}")

                pois?.firstOrNull { it.name.contains(name) || name.contains(it.name) }?.let { poi ->
                    favoriteIds.add(poi.id.toString())
                    android.util.Log.d("UserDataManager", "找到收藏: ${poi.name}, ID: ${poi.id}")
                } ?: run {
                    android.util.Log.w("UserDataManager", "未找到收藏: $name")
                }
            }

            if (favoriteIds.isNotEmpty()) {
                saveFavorites(favoriteIds)
                android.util.Log.d("UserDataManager", "已保存 ${favoriteIds.size} 个收藏")
            } else {
                android.util.Log.w("UserDataManager", "未找到任何收藏POI")
            }

            // 2. 初始化历史导航记录
            android.util.Log.d("UserDataManager", "开始初始化历史记录")
            val lillyCafeResult = searchService.searchByKeyword("Lilly Cafe", 20, center)
            val lillyCafePois = lillyCafeResult.getOrNull()
            android.util.Log.d("UserDataManager", "Lilly Cafe 搜索结果数量: ${lillyCafePois?.size ?: 0}")

            lillyCafePois?.firstOrNull { it.name.contains("Lilly") }?.let { poi ->
                val history = RouteHistory(
                    id = UUID.randomUUID().toString(),
                    startName = "我的位置",
                    startLocation = center,
                    endName = poi.name,
                    endLocation = LatLng(poi.lat, poi.lon),
                    timestamp = 1577836800000L // 2020年1月1日 00:00:00 UTC
                )
                addRouteHistory(history)
                android.util.Log.d("UserDataManager", "已添加历史记录: ${poi.name}")
            } ?: run {
                android.util.Log.w("UserDataManager", "未找到 Lilly Cafe")
            }

            // 标记为已初始化
            prefs.edit().putBoolean("preset_data_initialized", true).apply()
            android.util.Log.i("UserDataManager", "预设数据初始化完成")

        } catch (e: Exception) {
            android.util.Log.e("UserDataManager", "初始化预设数据失败", e)
        }
    }
}

/**
 * 应用主题
 */
enum class AppTheme(val displayName: String) {
    /** 明亮模式（白色主题） */
    BRIGHT("明亮模式"),
    /** 夜间模式（黑色主题） */
    NIGHT("夜间模式"),
    /** 护眼模式（灰色主题） */
    EYE_CARE("护眼模式")
}
