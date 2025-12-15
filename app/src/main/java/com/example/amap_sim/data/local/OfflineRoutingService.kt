package com.example.amap_sim.data.local

import android.util.Log
import com.example.amap_sim.domain.model.LatLng
import com.example.amap_sim.domain.model.RouteResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 离线路由服务（GraphHopper 版本 - 已弃用）
 * 
 * ⚠️ 已弃用：GraphHopper 从 2.0 版本起不再支持 Android 离线路由
 * 请使用 BRouterService 替代
 * 
 * 此类保留用于向后兼容，但不再提供实际功能
 * 所有方法将返回错误或抛出异常
 * 
 * @see BRouterService
 * @deprecated 使用 BRouterService 替代
 */
@Deprecated(
    message = "GraphHopper 不再支持 Android，请使用 BRouterService",
    replaceWith = ReplaceWith("BRouterService")
)
class OfflineRoutingService(
    private val dataManager: OfflineDataManager
) {
    companion object {
        private const val TAG = "OfflineRoutingService"
        
        // 交通方式配置
        const val PROFILE_CAR = "car"
        const val PROFILE_BIKE = "bike"
        const val PROFILE_FOOT = "foot"
        
        private const val DEPRECATION_MESSAGE = 
            "GraphHopper 路由服务已弃用，请使用 BRouterService"
    }
    
    private val initMutex = Mutex()
    private var isInitialized = false
    
    /**
     * 初始化路由引擎（已弃用）
     * 
     * @deprecated 请使用 BRouterService.initialize()
     */
    @Deprecated(DEPRECATION_MESSAGE, ReplaceWith("BRouterService.initialize()"))
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        initMutex.withLock {
            Log.w(TAG, DEPRECATION_MESSAGE)
            
            // 检查是否有 GraphHopper 数据可用
            @Suppress("DEPRECATION")
            val routeDir = dataManager.getRouteDirectory()
            
            if (routeDir.exists() && routeDir.isDirectory) {
                // 数据存在，但无法使用（没有 GraphHopper 库）
                Log.w(TAG, "发现 GraphHopper 数据目录，但 GraphHopper 库未包含在项目中")
                Log.w(TAG, "如需使用 GraphHopper，请在 build.gradle.kts 中启用依赖")
                isInitialized = true
                Result.success(Unit)
            } else {
                isInitialized = false
                Result.failure(RuntimeException(DEPRECATION_MESSAGE))
            }
        }
    }
    
    /**
     * 检查是否已初始化
     */
    fun isReady(): Boolean = isInitialized
    
    /**
     * 计算两点间路径（已弃用）
     * 
     * @deprecated 请使用 BRouterService.calculateRoute()
     */
    @Deprecated(DEPRECATION_MESSAGE, ReplaceWith("BRouterService.calculateRoute(start, end, profile)"))
    suspend fun calculateRoute(
        start: LatLng,
        end: LatLng,
        profile: String = PROFILE_CAR
    ): Result<RouteResult> = withContext(Dispatchers.IO) {
        Log.w(TAG, DEPRECATION_MESSAGE)
        Result.failure(RuntimeException(DEPRECATION_MESSAGE))
    }
    
    /**
     * 计算多途经点路径（已弃用）
     * 
     * @deprecated 请使用 BRouterService.calculateRouteWithWaypoints()
     */
    @Deprecated(DEPRECATION_MESSAGE, ReplaceWith("BRouterService.calculateRouteWithWaypoints(points, profile)"))
    suspend fun calculateRouteWithWaypoints(
        points: List<LatLng>,
        profile: String = PROFILE_CAR
    ): Result<RouteResult> = withContext(Dispatchers.IO) {
        Log.w(TAG, DEPRECATION_MESSAGE)
        Result.failure(RuntimeException(DEPRECATION_MESSAGE))
    }
    
    /**
     * 获取支持的交通方式列表
     */
    fun getSupportedProfiles(): List<String> {
        return listOf(PROFILE_CAR, PROFILE_BIKE, PROFILE_FOOT)
    }
    
    /**
     * 获取交通方式的显示名称
     */
    fun getProfileDisplayName(profile: String): String {
        return when (profile) {
            PROFILE_CAR -> "驾车"
            PROFILE_BIKE -> "骑行"
            PROFILE_FOOT -> "步行"
            else -> profile
        }
    }
    
    /**
     * 释放资源
     */
    fun close() {
        isInitialized = false
        Log.i(TAG, "OfflineRoutingService 资源已释放")
    }
}

/**
 * 路由异常
 */
class RoutingException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
