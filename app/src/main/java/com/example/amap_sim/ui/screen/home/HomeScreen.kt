package com.example.amap_sim.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amap_sim.domain.model.LatLng
import com.example.amap_sim.ui.components.SearchBarDisplay
import com.example.amap_sim.ui.screen.map.MapScreen
import com.example.amap_sim.ui.screen.map.MapViewModel
import com.example.amap_sim.ui.theme.AmapBlue
import com.example.amap_sim.ui.theme.AmapSimTheme
import kotlinx.coroutines.launch

/**
 * 主页 - 仿高德地图首页
 * 
 * 包含：
 * - 顶部搜索框
 * - 地图视图
 * - 快捷功能入口
 * - 底部功能栏
 */
@Composable
fun HomeScreen(
    mapViewModel: MapViewModel = viewModel(),
    onNavigateToSearch: () -> Unit = {},
    onNavigateToRoute: (start: LatLng, end: LatLng) -> Unit = { _, _ -> },
    onNavigateToPoiDetail: (String) -> Unit = {},
    onNavigateToDrive: () -> Unit = {},
    onNavigateToBike: () -> Unit = {},
    onNavigateToWalk: () -> Unit = {},
    onNavigateToRoutePlanning: () -> Unit = {},
    onNavigateToMore: () -> Unit = {}
) {
    val uiState by mapViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // 显示错误信息
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            scope.launch {
                snackbarHostState.showSnackbar(error)
            }
            mapViewModel.clearError()
        }
    }
    
    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 地图层
            MapScreen(
                viewModel = mapViewModel,
                onNavigateToSearch = onNavigateToSearch,
                onNavigateToRoute = onNavigateToRoute
            )
            
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
                onRouteClick = onNavigateToRoutePlanning,
                onMoreClick = onNavigateToMore,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
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
        // 搜索框
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
    onRouteClick: () -> Unit,
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
                icon = Icons.Default.NearMe,
                label = "路线",
                onClick = onRouteClick,
                iconTint = AmapBlue
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
private fun TopSearchAreaPreview() {
    AmapSimTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Gray)
                .padding(vertical = 16.dp)
        ) {
            TopSearchArea(
                onSearchClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BottomQuickActionsPreview() {
    AmapSimTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            BottomQuickActions(
                onDriveClick = {},
                onBikeClick = {},
                onWalkClick = {},
                onRouteClick = {},
                onMoreClick = {}
            )
        }
    }
}
