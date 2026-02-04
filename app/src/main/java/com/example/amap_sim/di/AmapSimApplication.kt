package com.example.amap_sim.di

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 仿高德地图应用 Application
 * 
 * 职责：
 * - 初始化 ServiceLocator
 * - 提供全局 Context
 */
class AmapSimApplication : Application() {

    // Application 级别的协程作用域
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        private const val TAG = "AmapSimApplication"
        
        @Volatile
        private var instance: AmapSimApplication? = null
        
        /**
         * 获取 Application 实例
         */
        fun getInstance(): AmapSimApplication {
            return instance ?: throw IllegalStateException(
                "Application 未初始化"
            )
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this

        Log.i(TAG, "Application 启动")

        // 初始化服务定位器
        ServiceLocator.initialize(this)

        Log.i(TAG, "ServiceLocator 初始化完成")

        // 初始化 Agent 数据文件
        applicationScope.launch {
            try {
                // 临时设置为 true 以强制重新创建文件（用于测试标准格式）
                // 测试完成后可以改为 false
                ServiceLocator.agentDataManager.initializeFiles(forceRecreate = false)
                Log.i(TAG, "Agent 数据文件初始化完成")
            } catch (e: Exception) {
                Log.e(TAG, "Agent 数据文件初始化失败", e)
            }
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        
        // 释放资源
        ServiceLocator.release()
        
        Log.i(TAG, "Application 终止")
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        
        when (level) {
            TRIM_MEMORY_RUNNING_CRITICAL,
            TRIM_MEMORY_COMPLETE -> {
                Log.w(TAG, "内存紧张，level=$level")
                // 可以在这里释放一些缓存
            }
        }
    }
}
