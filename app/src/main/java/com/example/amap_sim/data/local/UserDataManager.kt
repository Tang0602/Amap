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
        UserProfile(
            userId = prefs.getString(KEY_USER_ID, "") ?: "",
            userName = prefs.getString(KEY_USER_NAME, "") ?: "",
            avatarPath = prefs.getString(KEY_AVATAR_PATH, "") ?: ""
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
     */
    suspend fun getRouteHistory(): List<RouteHistory> = withContext(Dispatchers.IO) {
        try {
            val json = prefs.getString(KEY_ROUTE_HISTORY, null) ?: return@withContext emptyList()
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
            emptyList()
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
     */
    suspend fun getFavorites(): List<String> = withContext(Dispatchers.IO) {
        try {
            val json = prefs.getString(KEY_FAVORITES, null) ?: return@withContext emptyList()
            val jsonArray = JSONArray(json)
            (0 until jsonArray.length()).map { i -> jsonArray.getString(i) }
        } catch (e: Exception) {
            emptyList()
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
