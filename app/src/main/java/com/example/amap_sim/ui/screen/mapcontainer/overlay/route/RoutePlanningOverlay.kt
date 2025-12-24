package com.example.amap_sim.ui.screen.mapcontainer.overlay.route

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amap_sim.domain.model.RouteResult
import com.example.amap_sim.ui.screen.mapcontainer.MapStateController
import com.example.amap_sim.ui.theme.AmapBlue
import com.example.amap_sim.ui.theme.AmapGreen
import com.example.amap_sim.ui.theme.Gray400
import com.example.amap_sim.ui.theme.Gray500
import com.example.amap_sim.ui.theme.MapMarkerEnd
import com.example.amap_sim.ui.theme.MapRouteBikeColor
import com.example.amap_sim.ui.theme.MapRouteWalkColor

/**
 * 路线规划 Overlay
 * 
 * 在地图上叠加路线规划界面，显示起终点和路线
 */
@Composable
fun RoutePlanningOverlay(
    destLat: Double? = null,
    destLon: Double? = null,
    destName: String? = null,
    initialProfile: TravelProfile? = null,
    startLocation: LocationInput? = null,
    waypoints: List<LocationInput>? = null,
    endLocation: LocationInput? = null,
    mapController: MapStateController,
    onNavigateBack: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToAddWaypoint: (LocationInput, List<LocationInput>, LocationInput?) -> Unit = { _, _, _ -> },
    onStartNavigation: (RouteResult) -> Unit,
    viewModel: RouteViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 调试日志：记录接收到的参数
    android.util.Log.d("RoutePlanningOverlay", "Recomposed with params: startLocation=$startLocation, waypoints.size=${waypoints?.size ?: "null"}, endLocation=$endLocation")
    
    // 初始化交通方式（如果从快捷入口进入）
    LaunchedEffect(initialProfile) {
        initialProfile?.let { profile ->
            viewModel.setInitialProfile(profile)
        }
    }
    
    // 初始化目的地（如果从详情页传入）
    LaunchedEffect(destLat, destLon, destName) {
        if (destLat != null && destLon != null) {
            viewModel.setDestination(destLat, destLon, destName)
        }
    }
    
    // 更新途径点（从添加途径点页面返回时）
    // 使用参数组合作为 key，确保只在参数真正变化时触发
    // 注意：waypoints 可能是空列表，所以需要特殊处理
    val waypointsKey = waypoints?.let { 
        // 使用列表的 size 和所有元素的 hashCode 组合作为 key
        // 这样可以检测列表内容的变化，同时处理空列表的情况
        "${it.size}_${it.map { it.hashCode() }.hashCode()}"
    } ?: "null"
    
    // 创建一个组合 key 来检测所有参数的变化
    val updateKey = "${startLocation?.hashCode()}_${waypointsKey}_${endLocation?.hashCode()}"
    
    LaunchedEffect(updateKey) {
        // 检查是否有任何参数被设置（表示是从 AddWaypointOverlay 返回）
        // 注意：waypoints 可能是空列表，所以不能简单地用 != null 判断
        val hasStartLocation = startLocation != null
        val hasWaypoints = waypoints != null // 即使是空列表，也是有效的
        val hasEndLocation = endLocation != null
        
        android.util.Log.d("RoutePlanningOverlay", "LaunchedEffect triggered: updateKey=$updateKey")
        android.util.Log.d("RoutePlanningOverlay", "hasStartLocation=$hasStartLocation, hasWaypoints=$hasWaypoints, hasEndLocation=$hasEndLocation")
        android.util.Log.d("RoutePlanningOverlay", "waypoints: ${waypoints?.size ?: "null"}")
        android.util.Log.d("RoutePlanningOverlay", "current uiState.waypoints.size: ${uiState.waypoints.size}")
        
        if (hasStartLocation || hasWaypoints || hasEndLocation) {
            android.util.Log.d("RoutePlanningOverlay", "Calling UpdateWaypoints event")
            viewModel.onEvent(
                RouteEvent.UpdateWaypoints(
                    startLocation = startLocation ?: uiState.startLocation,
                    waypoints = waypoints ?: uiState.waypoints, // 如果 waypoints 是 null，使用当前状态
                    endLocation = endLocation ?: uiState.endLocation
                )
            )
        } else {
            android.util.Log.d("RoutePlanningOverlay", "Skipping UpdateWaypoints: all parameters are null")
        }
    }
    
    // 监听地图更新状态，应用 ViewModel 计算的地图操作
    // UI 层只负责执行，不包含业务逻辑
    LaunchedEffect(uiState.mapUpdate) {
        when (val update = uiState.mapUpdate) {
            null -> { /* 无更新 */ }
            is RouteMapUpdate.Clear -> {
                mapController.clearMarkers()
                mapController.clearRoute()
            }
            is RouteMapUpdate.ShowRoute -> {
                // 显示起点、途径点和终点标记
                val allMarkers = listOf(update.startMarker) + update.waypointMarkers + listOf(update.endMarker)
                mapController.setMarkers(allMarkers)
                mapController.setRoute(update.routeResult)
            }
            is RouteMapUpdate.ShowMarkersOnly -> {
                // 显示起点、途径点和终点标记（无路线）
                val allMarkers = listOf(update.startMarker) + update.waypointMarkers + listOf(update.endMarker)
                mapController.setMarkers(allMarkers)
                mapController.clearRoute()
            }
        }
    }
    
    // 监听导航事件
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                RouteNavigationEvent.Back -> {
                    mapController.clearMarkers()
                    mapController.clearRoute()
                    onNavigateBack()
                }
                RouteNavigationEvent.SelectStartFromSearch -> onNavigateToSearch()
                RouteNavigationEvent.SelectEndFromSearch -> onNavigateToSearch()
                is RouteNavigationEvent.StartNavigation -> onStartNavigation(event.routeResult)
            }
        }
    }
    
    RoutePlanningOverlayContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = {
            mapController.clearMarkers()
            mapController.clearRoute()
            onNavigateBack()
        },
        onNavigateToAddWaypoint = onNavigateToAddWaypoint
    )
}

@Composable
private fun RoutePlanningOverlayContent(
    uiState: RouteUiState,
    onEvent: (RouteEvent) -> Unit,
    onBack: () -> Unit,
    onNavigateToAddWaypoint: (LocationInput, List<LocationInput>, LocationInput?) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 顶部面板
        TopPanel(
            uiState = uiState,
            onEvent = onEvent,
            onBack = onBack,
            onNavigateToAddWaypoint = onNavigateToAddWaypoint,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        )
        
        // 底部结果卡片（当有路线结果时显示）
        if (uiState.routeResult != null) {
            BottomResultPanel(
                routeResult = uiState.routeResult,
                profile = uiState.selectedProfile,
                onStartNavigation = { onEvent(RouteEvent.StartNavigation) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        
        // 加载状态
        if (uiState.isLoading) {
            LoadingOverlay()
        }
        
        // 错误状态
        uiState.error?.let { error ->
            ErrorOverlay(
                message = error,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

/**
 * 顶部面板
 */
@Composable
private fun TopPanel(
    uiState: RouteUiState,
    onEvent: (RouteEvent) -> Unit,
    onBack: () -> Unit,
    onNavigateToAddWaypoint: (LocationInput, List<LocationInput>, LocationInput?) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            // 顶部栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Text(
                    text = "路线规划",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // 起点终点输入卡片和途径点按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LocationInputCard(
                    startLocation = uiState.startLocation,
                    waypoints = uiState.waypoints,
                    endLocation = uiState.endLocation,
                    onStartClick = { onEvent(RouteEvent.ClickStartInput) },
                    onEndClick = { onEvent(RouteEvent.ClickEndInput) },
                    onSwapClick = { onEvent(RouteEvent.SwapLocations) },
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(16.dp))

                // 途径点按钮（在卡片外面右侧）
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(onClick = {
                        onNavigateToAddWaypoint(
                            uiState.startLocation,
                            uiState.waypoints,
                            uiState.endLocation
                        )
                    })
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddCircleOutline,
                        contentDescription = "添加途径点",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "途径点",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // 交通方式选择
            TravelModeSelector(
                selectedProfile = uiState.selectedProfile,
                onProfileSelect = { onEvent(RouteEvent.SelectProfile(it)) }
            )
        }
    }
}

/**
 * 起点终点输入卡片
 */
@Composable
private fun LocationInputCard(
    startLocation: LocationInput,
    waypoints: List<LocationInput>,
    endLocation: LocationInput?,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit,
    onSwapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标列
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(AmapGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color.White
                    )
                }
                
                // 途径点图标（如果有）
                waypoints.forEach { _ ->
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(12.dp)
                            .background(Gray400.copy(alpha = 0.5f))
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(AmapBlue.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = Color.White
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(12.dp)
                            .background(Gray400.copy(alpha = 0.5f))
                    )
                }
                
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(if (waypoints.isEmpty()) 20.dp else 12.dp)
                        .background(Gray400.copy(alpha = 0.5f))
                )
                
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MapMarkerEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 输入框列
            Column(modifier = Modifier.weight(1f)) {
                LocationInputField(
                    value = startLocation.getDisplayName(),
                    isCurrentLocation = startLocation is LocationInput.CurrentLocation,
                    onClick = onStartClick
                )
                
                // 途径点输入框（如果有）
                waypoints.forEachIndexed { index, waypoint ->
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    LocationInputField(
                        value = waypoint.getDisplayName(),
                        placeholder = "途经点 ${index + 1}",
                        onClick = { /* 途径点点击暂不处理 */ }
                    )
                }
                
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                
                LocationInputField(
                    value = endLocation?.getDisplayName() ?: "",
                    placeholder = "请输入目的地",
                    onClick = onEndClick
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 交换按钮
            IconButton(
                onClick = onSwapClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "交换起点终点",
                    tint = Gray500
                )
            }
        }
    }
}

/**
 * 位置输入框
 */
@Composable
private fun LocationInputField(
    value: String,
    placeholder: String = "",
    isCurrentLocation: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (value.isNotEmpty()) value else placeholder,
            style = MaterialTheme.typography.bodyMedium,
            color = if (value.isNotEmpty()) MaterialTheme.colorScheme.onSurface else Gray400,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        
        if (isCurrentLocation) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = AmapBlue.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "定位",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmapBlue,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * 交通方式选择器
 */
@Composable
private fun TravelModeSelector(
    selectedProfile: TravelProfile,
    onProfileSelect: (TravelProfile) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TravelProfile.entries.forEach { profile ->
            val isSelected = profile == selectedProfile
            
            FilterChip(
                selected = isSelected,
                onClick = { onProfileSelect(profile) },
                label = {
                    Text(
                        text = profile.displayName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = getProfileIcon(profile),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = getProfileColor(profile).copy(alpha = 0.15f),
                    selectedLabelColor = getProfileColor(profile),
                    selectedLeadingIconColor = getProfileColor(profile)
                )
            )
        }
    }
}

/**
 * 底部结果面板
 */
@Composable
private fun BottomResultPanel(
    routeResult: RouteResult,
    profile: TravelProfile,
    onStartNavigation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 路线概览
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = routeResult.getFormattedTime(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = getProfileColor(profile)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = routeResult.getFormattedDistance(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${profile.displayName} · ${routeResult.instructions.size} 步",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 开始导航按钮
            Button(
                onClick = onStartNavigation,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AmapBlue),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "开始导航",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * 加载中遮罩
 */
@Composable
private fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = AmapBlue,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "正在计算路线...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gray500
                )
            }
        }
    }
}

/**
 * 错误遮罩
 */
@Composable
private fun ErrorOverlay(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(32.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "路线计算失败",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Gray500
            )
        }
    }
}

/**
 * 获取交通方式图标
 */
private fun getProfileIcon(profile: TravelProfile): ImageVector {
    return when (profile) {
        TravelProfile.CAR -> Icons.Default.DirectionsCar
        TravelProfile.BIKE -> Icons.Default.DirectionsBike
        TravelProfile.FOOT -> Icons.Default.DirectionsWalk
    }
}

/**
 * 获取交通方式颜色
 */
private fun getProfileColor(profile: TravelProfile): Color {
    return when (profile) {
        TravelProfile.CAR -> AmapBlue
        TravelProfile.BIKE -> MapRouteBikeColor
        TravelProfile.FOOT -> MapRouteWalkColor
    }
}

