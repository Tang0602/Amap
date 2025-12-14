package com.example.amap_sim.ui.screen.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * 地图缩放控制按钮
 * 
 * @param onZoomIn 放大回调
 * @param onZoomOut 缩小回调
 * @param modifier Modifier
 */
@Composable
fun MapZoomControls(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = Color.White
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 放大按钮
            IconButton(
                onClick = onZoomIn,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "放大",
                    tint = Color(0xFF666666)
                )
            }
            
            // 分隔线
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 8.dp),
                color = Color(0xFFE0E0E0)
            )
            
            // 缩小按钮
            IconButton(
                onClick = onZoomOut,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "缩小",
                    tint = Color(0xFF666666)
                )
            }
        }
    }
}

/**
 * 定位按钮
 * 
 * @param onClick 点击回调
 * @param isLocating 是否正在定位
 * @param modifier Modifier
 */
@Composable
fun LocationButton(
    onClick: () -> Unit,
    isLocating: Boolean = false,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        containerColor = Color.White,
        contentColor = if (isLocating) MaterialTheme.colorScheme.primary else Color(0xFF666666),
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "定位",
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * 地图控制面板
 * 
 * 包含缩放按钮和定位按钮
 * 
 * @param onZoomIn 放大回调
 * @param onZoomOut 缩小回调
 * @param onLocationClick 定位回调
 * @param isLocating 是否正在定位
 * @param showLocationButton 是否显示定位按钮
 * @param modifier Modifier
 */
@Composable
fun MapControlPanel(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onLocationClick: () -> Unit = {},
    isLocating: Boolean = false,
    showLocationButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 缩放控制
        MapZoomControls(
            onZoomIn = onZoomIn,
            onZoomOut = onZoomOut
        )
        
        // 定位按钮
        if (showLocationButton) {
            LocationButton(
                onClick = onLocationClick,
                isLocating = isLocating
            )
        }
    }
}

/**
 * 圆形图标按钮
 * 
 * @param icon 图标
 * @param contentDescription 内容描述
 * @param onClick 点击回调
 * @param modifier Modifier
 * @param enabled 是否启用
 * @param backgroundColor 背景颜色
 * @param iconTint 图标颜色
 */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = Color.White,
    iconTint: Color = Color(0xFF666666)
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        containerColor = backgroundColor,
        contentColor = if (enabled) iconTint else iconTint.copy(alpha = 0.5f),
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * 比例尺组件（预留）
 */
@Composable
fun MapScaleBar(
    distanceMeters: Int,
    modifier: Modifier = Modifier
) {
    // TODO: 实现比例尺显示
    // Mapsforge 内置比例尺可通过 mapView.mapScaleBar.isVisible = true 启用
}
