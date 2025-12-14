package com.example.amap_sim.ui.screen.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amap_sim.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 启动页 UI 状态
 */
data class SplashUiState(
    /** 当前阶段 */
    val stage: SplashStage = SplashStage.Checking,
    /** 进度 (0-100) */
    val progress: Int = 0,
    /** 提示信息 */
    val message: String = "正在准备...",
    /** 错误信息 */
    val error: String? = null,
    /** 是否初始化完成 */
    val isCompleted: Boolean = false
)

/**
 * 启动阶段
 */
enum class SplashStage {
    /** 检查数据 */
    Checking,
    /** 复制数据 */
    Copying,
    /** 加载服务 */
    Loading,
    /** 完成 */
    Completed,
    /** 错误 */
    Error
}

/**
 * 启动页 ViewModel
 * 
 * 负责：
 * - 检查离线数据是否需要初始化
 * - 初始化数据（从 assets 复制到内部存储）
 * - 预加载服务
 */
class SplashViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()
    
    private val dataManager by lazy { ServiceLocator.dataManager }
    
    /**
     * 开始初始化流程
     */
    fun startInitialization() {
        viewModelScope.launch {
            try {
                // 阶段1: 检查数据
                _uiState.update { 
                    it.copy(
                        stage = SplashStage.Checking,
                        progress = 0,
                        message = "正在检查数据..."
                    )
                }
                
                // 检查是否需要初始化
                if (dataManager.needsInitialization()) {
                    // 阶段2: 复制数据
                    _uiState.update { 
                        it.copy(
                            stage = SplashStage.Copying,
                            progress = 5,
                            message = "首次启动，正在准备离线数据..."
                        )
                    }
                    
                    // 执行数据初始化
                    val result = dataManager.initializeData { progress, message ->
                        _uiState.update { 
                            it.copy(
                                progress = 5 + (progress * 0.8).toInt(), // 5-85%
                                message = message
                            )
                        }
                    }
                    
                    // 检查初始化结果
                    result.onFailure { error ->
                        _uiState.update { 
                            it.copy(
                                stage = SplashStage.Error,
                                error = "数据初始化失败: ${error.message}",
                                message = "初始化失败"
                            )
                        }
                        return@launch
                    }
                } else {
                    // 数据已存在，快速跳过
                    _uiState.update { 
                        it.copy(
                            progress = 85,
                            message = "数据已准备就绪"
                        )
                    }
                }
                
                // 阶段3: 加载服务
                _uiState.update { 
                    it.copy(
                        stage = SplashStage.Loading,
                        progress = 90,
                        message = "正在加载服务..."
                    )
                }
                
                // 预加载路由和搜索服务（可选）
                preloadServices()
                
                // 阶段4: 完成
                _uiState.update { 
                    it.copy(
                        stage = SplashStage.Completed,
                        progress = 100,
                        message = "准备完成",
                        isCompleted = true
                    )
                }
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        stage = SplashStage.Error,
                        error = "初始化异常: ${e.message}",
                        message = "初始化失败"
                    )
                }
            }
        }
    }
    
    /**
     * 预加载服务（可选优化）
     */
    private suspend fun preloadServices() {
        try {
            // 可以在这里预初始化 GraphHopper 等服务
            // 但这会增加启动时间，所以暂时跳过
            // ServiceLocator.routingService.initialize()
        } catch (e: Exception) {
            // 预加载失败不影响启动
        }
    }
    
    /**
     * 重试初始化
     */
    fun retry() {
        _uiState.update { 
            SplashUiState()
        }
        startInitialization()
    }
}
