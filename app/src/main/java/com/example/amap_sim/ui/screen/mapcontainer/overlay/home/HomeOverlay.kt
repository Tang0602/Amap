package com.example.amap_sim.ui.screen.mapcontainer.overlay.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.amap_sim.ui.components.SearchBarDisplay
import com.example.amap_sim.ui.screen.mapcontainer.MapStateController
import com.example.amap_sim.ui.theme.AmapSimTheme

/**
 * 主页 Overlay
 * 
 * 在地图上叠加：
 * - 顶部搜索框
 * - 底部快捷功能入口
 */
@Composable
fun HomeOverlay(
    mapController: MapStateController,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToDrive: () -> Unit = {},
    onNavigateToBike: () -> Unit = {},
    onNavigateToWalk: () -> Unit = {},
    onNavigateToMore: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部搜索区域
        TopSearchArea(
            onSearchClick = onNavigateToSearch,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        )
        
        // 底部快捷功能入口
        BottomQuickActions(
            onDriveClick = onNavigateToDrive,
            onBikeClick = onNavigateToBike,
            onWalkClick = onNavigateToWalk,
            onMoreClick = onNavigateToMore,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

/**
 * 顶部搜索区域
 */
@Composable
private fun TopSearchArea(
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = Color.Transparent
    ) {
        SearchBarDisplay(
            onClick = onSearchClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 底部快捷功能入口
 */
@Composable
private fun BottomQuickActions(
    onDriveClick: () -> Unit,
    onBikeClick: () -> Unit,
    onWalkClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickActionItem(
                icon = Icons.Default.DirectionsCar,
                label = "驾车",
                onClick = onDriveClick
            )
            
            QuickActionItem(
                icon = Icons.Default.DirectionsBike,
                label = "骑行",
                onClick = onBikeClick
            )
            
            QuickActionItem(
                icon = Icons.Default.DirectionsWalk,
                label = "步行",
                onClick = onWalkClick
            )
            
            QuickActionItem(
                icon = Icons.Default.MoreVert,
                label = "更多",
                onClick = onMoreClick
            )
        }
    }
}

/**
 * 快捷功能项
 */
@Composable
private fun QuickActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )
        }
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BottomQuickActionsPreview() {
    AmapSimTheme {
        BottomQuickActions(
            onDriveClick = {},
            onBikeClick = {},
            onWalkClick = {},
            onMoreClick = {}
        )
    }
}

