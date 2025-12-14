package com.example.amap_sim.ui.screen.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amap_sim.ui.theme.AmapBlue
import com.example.amap_sim.ui.theme.AmapBlueDark
import com.example.amap_sim.ui.theme.AmapSimTheme
import kotlinx.coroutines.delay

/**
 * 启动页
 * 
 * 显示应用 Logo 和数据初始化进度
 */
@Composable
fun SplashScreen(
    viewModel: SplashViewModel = viewModel(),
    onInitComplete: () -> Unit = {},
    onInitError: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 启动初始化流程
    LaunchedEffect(Unit) {
        viewModel.startInitialization()
    }
    
    // 监听初始化完成
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            delay(300) // 短暂延迟，让用户看到 100%
            onInitComplete()
        }
    }
    
    // 监听错误
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            // 可以在这里处理错误，但我们仍然让用户可以进入主页
            // onInitError(error)
        }
    }
    
    SplashContent(
        uiState = uiState,
        onRetry = { viewModel.retry() }
    )
}

@Composable
private fun SplashContent(
    uiState: SplashUiState,
    onRetry: () -> Unit = {}
) {
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.progress / 100f,
        animationSpec = tween(300),
        label = "progress"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AmapBlue,
                        AmapBlueDark
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            // Logo 区域
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "地图",
                    modifier = Modifier.size(72.dp),
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 应用名称
            Text(
                text = "高德地图",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                ),
                color = Color.White
            )
            
            Text(
                text = "离线版",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 错误状态
            AnimatedVisibility(
                visible = uiState.stage == SplashStage.Error,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.error ?: "初始化失败",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = AmapBlue
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("重试")
                    }
                }
            }
            
            // 正常加载状态
            AnimatedVisibility(
                visible = uiState.stage != SplashStage.Error,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 进度条
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 提示信息
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // 进度百分比
                    Text(
                        text = "${uiState.progress}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // 底部版本信息
            Text(
                text = "v1.0.0 · 离线模式",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    AmapSimTheme {
        SplashContent(
            uiState = SplashUiState(
                stage = SplashStage.Copying,
                progress = 45,
                message = "正在复制路由数据..."
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenErrorPreview() {
    AmapSimTheme {
        SplashContent(
            uiState = SplashUiState(
                stage = SplashStage.Error,
                progress = 30,
                message = "初始化失败",
                error = "无法复制数据文件"
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenCompletedPreview() {
    AmapSimTheme {
        SplashContent(
            uiState = SplashUiState(
                stage = SplashStage.Completed,
                progress = 100,
                message = "准备完成",
                isCompleted = true
            )
        )
    }
}
