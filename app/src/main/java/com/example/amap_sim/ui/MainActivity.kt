package com.example.amap_sim.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.amap_sim.ui.navigation.AmapNavGraph
import com.example.amap_sim.ui.theme.AmapSimTheme

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
            AmapSimTheme {
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
