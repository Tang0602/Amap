package com.example.amap_sim.di

import android.content.Context
import com.example.amap_sim.data.local.OfflineDataManager
import com.example.amap_sim.data.local.OfflineRoutingService
import com.example.amap_sim.data.local.OfflineSearchService

/**
 * 简单的服务定位器
 * 
 * 提供全局访问核心服务的能力
 * 在 Application 中初始化
 */
object ServiceLocator {
    
    @Volatile
    private var isInitialized = false
    
    private var _dataManager: OfflineDataManager? = null
    private var _routingService: OfflineRoutingService? = null
    private var _searchService: OfflineSearchService? = null
    
    /**
     * 数据管理器
     */
    val dataManager: OfflineDataManager
        get() = _dataManager ?: throw IllegalStateException(
            "ServiceLocator 未初始化，请先调用 initialize()"
        )
    
    /**
     * 路由服务
     */
    val routingService: OfflineRoutingService
        get() = _routingService ?: throw IllegalStateException(
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
     * 初始化服务定位器
     * 
     * 在 Application.onCreate() 中调用
     */
    @Synchronized
    fun initialize(context: Context) {
        if (isInitialized) return
        
        val appContext = context.applicationContext
        
        _dataManager = OfflineDataManager.getInstance(appContext)
        _routingService = OfflineRoutingService(_dataManager!!)
        _searchService = OfflineSearchService(_dataManager!!)
        
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
        _routingService?.close()
        _searchService?.close()
        
        _dataManager = null
        _routingService = null
        _searchService = null
        
        isInitialized = false
    }
}
