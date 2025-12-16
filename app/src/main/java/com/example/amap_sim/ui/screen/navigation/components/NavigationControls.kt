package com.example.amap_sim.ui.screen.navigation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.amap_sim.ui.screen.navigation.NavigationState
import com.example.amap_sim.ui.theme.AmapBlue
import com.example.amap_sim.ui.theme.AmapGreen
import com.example.amap_sim.ui.theme.AmapOrange
import com.example.amap_sim.ui.theme.AmapRed
import com.example.amap_sim.ui.theme.AmapSimTheme
import com.example.amap_sim.ui.theme.Gray400
import com.example.amap_sim.ui.theme.Gray500

/**
 * 导航控制按钮组
 * 
 * 包含：
 * - 跟随模式切换
 * - 全览模式切换
 * - 暂停/继续
 * - 结束导航
 */
@Composable
fun NavigationControls(
    navigationState: NavigationState,
    isFollowingUser: Boolean,
    isOverviewMode: Boolean,
    onToggleFollow: () -> Unit,
    onToggleOverview: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 全览模式按钮
        ControlButton(
            icon = if (isOverviewMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
            contentDescription = if (isOverviewMode) "退出全览" else "全览模式",
            isActive = isOverviewMode,
            onClick = onToggleOverview
        )
        
        // 跟随模式按钮
        ControlButton(
            icon = if (isFollowingUser) Icons.Default.GpsFixed else Icons.Default.GpsOff,
            contentDescription = if (isFollowingUser) "跟随中" else "未跟随",
            isActive = isFollowingUser,
            onClick = onToggleFollow
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 暂停/继续按钮
        when (navigationState) {
            NavigationState.NAVIGATING -> {
                FloatingActionButton(
                    onClick = onPause,
                    containerColor = AmapOrange,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "暂停",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            NavigationState.PAUSED -> {
                FloatingActionButton(
                    onClick = onResume,
                    containerColor = AmapGreen,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "继续",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            else -> {}
        }
        
        // 结束导航按钮
        SmallFloatingActionButton(
            onClick = onStop,
            containerColor = AmapRed.copy(alpha = 0.9f),
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "结束导航",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 控制按钮
 */
@Composable
private fun ControlButton(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) AmapBlue else MaterialTheme.colorScheme.surface,
        label = "bg_color"
    )
    
    val iconColor by animateColorAsState(
        targetValue = if (isActive) Color.White else Gray500,
        label = "icon_color"
    )
    
    SmallFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = backgroundColor,
        contentColor = iconColor,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 4.dp
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * 导航状态指示器
 */
@Composable
fun NavigationStateIndicator(
    state: NavigationState,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (state) {
        NavigationState.NOT_STARTED -> "准备导航" to Gray500
        NavigationState.NAVIGATING -> "导航中" to AmapGreen
        NavigationState.PAUSED -> "已暂停" to AmapOrange
        NavigationState.ARRIVED -> "已到达" to AmapGreen
        NavigationState.OFF_ROUTE -> "偏离路线" to AmapRed
        NavigationState.ERROR -> "导航错误" to AmapRed
    }
    
    AnimatedVisibility(
        visible = state != NavigationState.NAVIGATING,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = color.copy(alpha = 0.15f),
            contentColor = color
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 到达提示卡片
 */
@Composable
fun ArrivalCard(
    destinationName: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = AmapGreen,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎉",
                style = MaterialTheme.typography.displaySmall
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "已到达目的地",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            destinationName?.let { name ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Surface(
                onClick = onDismiss,
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                contentColor = AmapGreen
            ) {
                Text(
                    text = "完成",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp)
                )
            }
        }
    }
}

// ========== Previews ==========

@Preview(showBackground = true)
@Composable
private fun NavigationControlsPreview() {
    AmapSimTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            NavigationControls(
                navigationState = NavigationState.NAVIGATING,
                isFollowingUser = true,
                isOverviewMode = false,
                onToggleFollow = {},
                onToggleOverview = {},
                onPause = {},
                onResume = {},
                onStop = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NavigationStateIndicatorPreview() {
    AmapSimTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NavigationStateIndicator(state = NavigationState.PAUSED)
            NavigationStateIndicator(state = NavigationState.ARRIVED)
            NavigationStateIndicator(state = NavigationState.OFF_ROUTE)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ArrivalCardPreview() {
    AmapSimTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ArrivalCard(
                destinationName = "武汉大学",
                onDismiss = {}
            )
        }
    }
}

