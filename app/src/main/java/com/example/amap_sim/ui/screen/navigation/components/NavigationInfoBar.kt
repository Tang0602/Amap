package com.example.amap_sim.ui.screen.navigation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amap_sim.ui.theme.AmapBlue
import com.example.amap_sim.ui.theme.AmapSimTheme
import com.example.amap_sim.ui.theme.Gray500
import com.example.amap_sim.ui.theme.Gray700

/**
 * 导航信息栏
 * 
 * 显示在底部，包含：
 * - 剩余距离
 * - 剩余时间
 * - 预计到达时间
 * - 当前速度
 */
@Composable
fun NavigationInfoBar(
    remainingDistance: String,
    remainingTime: String,
    estimatedArrival: String?,
    currentSpeed: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 主要信息行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 剩余时间（大字）
                Column {
                    Text(
                        text = remainingTime,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = AmapBlue,
                        fontSize = 32.sp
                    )
                    Text(
                        text = "剩余时间",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500
                    )
                }
                
                // 预计到达时间
                estimatedArrival?.let { arrival ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = arrival,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Gray700,
                            fontSize = 28.sp
                        )
                        Text(
                            text = "预计到达",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray500
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 次要信息行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 剩余距离
                InfoItem(
                    icon = Icons.Default.Straighten,
                    label = "剩余距离",
                    value = remainingDistance,
                    modifier = Modifier.weight(1f)
                )
                
                // 当前速度
                InfoItem(
                    icon = Icons.Default.Speed,
                    label = "当前速度",
                    value = "${currentSpeed.toInt()} km/h",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 信息项
 */
@Composable
private fun InfoItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(AmapBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = AmapBlue
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Gray500
            )
        }
    }
}

// ========== Previews ==========

@Preview(showBackground = true)
@Composable
private fun NavigationInfoBarPreview() {
    AmapSimTheme {
        NavigationInfoBar(
            remainingDistance = "5.8公里",
            remainingTime = "15分钟",
            estimatedArrival = "14:35",
            currentSpeed = 42.0
        )
    }
}

