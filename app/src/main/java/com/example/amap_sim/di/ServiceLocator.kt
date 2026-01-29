package com.example.amap_sim.di

import android.content.Context
import com.example.amap_sim.data.local.BRouterService
import com.example.amap_sim.data.local.OfflineDataManager
import com.example.amap_sim.data.local.OfflineRoutingService
import com.example.amap_sim.data.local.OfflineSearchService
import com.example.amap_sim.data.local.UserDataManager

/**
 * 简单的服务定位器
 * 
 * 提供全局访问核心服务的能力
 * 在 Application 中初始化
 * 
 * 路由引擎选择：
 * - 默认使用 BRouter（专为移动端优化）
 * - 可通过 USE_BROUTER 常量切换回 GraphHopper
 */
object ServiceLocator {
    
    /**
     * 路由引擎选择开关
     * true: 使用 BRouter（推荐）
     * false: 使用 GraphHopper（已弃用）
     */
    private const val USE_BROUTER = true
    
    @Volatile
    private var isInitialized = false
    
    private var _dataManager: OfflineDataManager? = null
    private var _brouterService: BRouterService? = null
    private var _graphHopperService: OfflineRoutingService? = null
    private var _searchService: OfflineSearchService? = null
    private var _userDataManager: UserDataManager? = null
    
    /**
     * 数据管理器
     */
    val dataManager: OfflineDataManager
        get() = _dataManager ?: throw IllegalStateException(
            "ServiceLocator 未初始化，请先调用 initialize()"
        )
    
    /**
     * BRouter 路由服务（推荐使用）
     */
    val brouterService: BRouterService
        get() = _brouterService ?: throw IllegalStateException(
            "ServiceLocator 未初始化，请先调用 initialize()"
        )
    
    /**
     * GraphHopper 路由服务（已弃用）
     * @deprecated 请使用 brouterService
     */
    @Deprecated("请使用 brouterService", ReplaceWith("brouterService"))
    val routingService: OfflineRoutingService
        get() = _graphHopperService ?: throw IllegalStateException(
            "ServiceLocator 未初始化，请先调用 initialize()"
        )
    
    /**
     * 搜索服务
     */
    val searchService: OfflineSearchService
        get() = _searchService ?: throw IllegalStateException(
            "ServiceLocator 未初始化，请先调用 initialize()"
        )

    /**
     * 用户数据管理
     */
    val userDataManager: UserDataManager
        get() = _userDataManager ?: throw IllegalStateException(
            "ServiceLocator 未初始化，请先调用 initialize()"
        )
    
    /**
     * 检查是否使用 BRouter
     */
    fun isUsingBRouter(): Boolean = USE_BROUTER && dataManager.isBRouterAvailable()
    
    /**
     * 初始化服务定位器
     * 
     * 在 Application.onCreate() 中调用
     */
    @Synchronized
    fun initialize(context: Context) {
        if (isInitialized) return
        
        val appContext = context.applicationContext
        
        _dataManager = OfflineDataManager.getInstance(appContext)

        // 根据配置和数据可用性选择路由引擎
        _brouterService = BRouterService(_dataManager!!)
        _graphHopperService = OfflineRoutingService(_dataManager!!)

        _searchService = OfflineSearchService(_dataManager!!)
        _userDataManager = UserDataManager(appContext)
        
        isInitialized = true
    }
    
    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean = isInitialized
    
    /**
     * 释放所有资源（用于测试或清理）
     */
    @Synchronized
    fun release() {
        _brouterService?.close()
        _graphHopperService?.close()
        _searchService?.close()
        
        _dataManager = null
        _brouterService = null
        _graphHopperService = null
        _searchService = null
        _userDataManager = null
        
        isInitialized = false
    }
}
