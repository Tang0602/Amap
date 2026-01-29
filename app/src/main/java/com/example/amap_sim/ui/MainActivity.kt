package com.example.amap_sim.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.amap_sim.data.local.AppTheme
import com.example.amap_sim.di.ServiceLocator
import com.example.amap_sim.ui.navigation.AmapNavGraph
import com.example.amap_sim.ui.theme.AmapSimTheme
import kotlinx.coroutines.launch

/**
 * 主 Activity
 *
 * 应用唯一的 Activity，使用 Compose 和 Navigation 管理所有页面
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 安装系统启动页（Android 12+）
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // 启用边到边显示
        enableEdgeToEdge()

        setContent {
            // 读取并应用主题
            var currentTheme by remember { mutableStateOf(AppTheme.BRIGHT) }

            LaunchedEffect(Unit) {
                lifecycleScope.launch {
                    currentTheme = ServiceLocator.userDataManager.getTheme()
                }
            }

            // 监听主题变化
            LaunchedEffect(Unit) {
                lifecycleScope.launch {
                    // 定期检查主题变化（简单实现）
                    while (true) {
                        kotlinx.coroutines.delay(500)
                        val newTheme = ServiceLocator.userDataManager.getTheme()
                        if (newTheme != currentTheme) {
                            currentTheme = newTheme
                        }
                    }
                }
            }

            AmapSimTheme(appTheme = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AmapNavGraph()
                }
            }
        }
    }
}
