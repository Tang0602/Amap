package com.example.amap_sim.ui.screen.route

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.SwapVert
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amap_sim.domain.model.InstructionSign
import com.example.amap_sim.domain.model.LatLng
import com.example.amap_sim.domain.model.RouteInstruction
import com.example.amap_sim.domain.model.RouteResult
import com.example.amap_sim.ui.screen.route.components.InstructionDivider
import com.example.amap_sim.ui.screen.route.components.TurnInstructionItem
import com.example.amap_sim.ui.theme.AmapBlue
import com.example.amap_sim.ui.theme.AmapGreen
import com.example.amap_sim.ui.theme.AmapSimTheme
import com.example.amap_sim.ui.theme.Gray100
import com.example.amap_sim.ui.theme.Gray400
import com.example.amap_sim.ui.theme.Gray500
import com.example.amap_sim.ui.theme.MapMarkerEnd
import com.example.amap_sim.ui.theme.MapRouteBikeColor
import com.example.amap_sim.ui.theme.MapRouteWalkColor

/**
 * 路线规划页面
 */
@Composable
fun RoutePlanningScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSearch: (isSelectingStart: Boolean) -> Unit,
    onStartNavigation: (RouteResult) -> Unit,
    viewModel: RoutePlanningViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 监听导航事件
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                RoutePlanningNavigationEvent.Back -> onNavigateBack()
                RoutePlanningNavigationEvent.SelectStartFromSearch -> onNavigateToSearch(true)
                RoutePlanningNavigationEvent.SelectEndFromSearch -> onNavigateToSearch(false)
                is RoutePlanningNavigationEvent.StartNavigation -> onStartNavigation(event.routeResult)
            }
        }
    }
    
    RoutePlanningScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
private fun RoutePlanningScreenContent(
    uiState: RoutePlanningUiState,
    onEvent: (RoutePlanningEvent) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 顶部栏
            TopBar(
                onBackClick = { onEvent(RoutePlanningEvent.NavigateBack) }
            )
            
            // 起点终点输入区
            LocationInputCard(
                startLocation = uiState.startLocation,
                endLocation = uiState.endLocation,
                onStartClick = { onEvent(RoutePlanningEvent.ClickStartInput) },
                onEndClick = { onEvent(RoutePlanningEvent.ClickEndInput) },
                onSwapClick = { onEvent(RoutePlanningEvent.SwapLocations) }
            )
            
            // 交通方式选择
            TravelModeSelector(
                selectedProfile = uiState.selectedProfile,
                onProfileSelect = { onEvent(RoutePlanningEvent.SelectProfile(it)) }
            )
            
            // 内容区域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    uiState.isLoading -> {
                        // 加载中
                        LoadingContent(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    uiState.error != null -> {
                        // 错误状态
                        ErrorContent(
                            message = uiState.error,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    uiState.routeResult != null -> {
                        // 路线结果
                        RouteResultContent(
                            routeResult = uiState.routeResult,
                            profile = uiState.selectedProfile,
                            showInstructions = uiState.showInstructions,
                            onToggleInstructions = { onEvent(RoutePlanningEvent.ToggleInstructions) }
                        )
                    }
                    uiState.endLocation == null -> {
                        // 未选择终点
                        EmptyContent(
                            message = "请输入目的地",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
            
            // 底部按钮
            if (uiState.routeResult != null) {
                BottomButton(
                    onStartNavigation = { onEvent(RoutePlanningEvent.StartNavigation) }
                )
            }
        }
    }
}

/**
 * 顶部栏
 */
@Composable
private fun TopBar(
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
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
}

/**
 * 起点终点输入卡片
 */
@Composable
private fun LocationInputCard(
    startLocation: LocationInput,
    endLocation: LocationInput?,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit,
    onSwapClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 起点终点图标列
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 起点图标
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(AmapGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                }
                
                // 连接线
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(24.dp)
                        .background(Gray400.copy(alpha = 0.5f))
                )
                
                // 终点图标
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MapMarkerEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 输入框列
            Column(modifier = Modifier.weight(1f)) {
                // 起点输入
                LocationInputField(
                    label = "起点",
                    value = startLocation.getDisplayName(),
                    isCurrentLocation = startLocation is LocationInput.CurrentLocation,
                    onClick = onStartClick
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                
                // 终点输入
                LocationInputField(
                    label = "终点",
                    value = endLocation?.getDisplayName() ?: "",
                    placeholder = "请输入目的地",
                    onClick = onEndClick
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 交换按钮
            IconButton(
                onClick = onSwapClick,
                modifier = Modifier.size(40.dp)
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
    label: String,
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
            color = if (value.isNotEmpty()) {
                MaterialTheme.colorScheme.onSurface
            } else {
                Gray400
            },
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
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

/**
 * 路线结果内容
 */
@Composable
private fun RouteResultContent(
    routeResult: RouteResult,
    profile: TravelProfile,
    showInstructions: Boolean,
    onToggleInstructions: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // 路线概览卡片
        item {
            RouteOverviewCard(
                routeResult = routeResult,
                profile = profile,
                showInstructions = showInstructions,
                onToggleInstructions = onToggleInstructions
            )
        }
        
        // 详细指令（可展开）
        if (showInstructions) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "详细路线",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            itemsIndexed(
                items = routeResult.instructions,
                key = { index, _ -> index }
            ) { index, instruction ->
                TurnInstructionItem(
                    instruction = instruction,
                    index = index,
                    isLast = index == routeResult.instructions.lastIndex
                )
                if (index < routeResult.instructions.lastIndex) {
                    InstructionDivider()
                }
            }
        }
    }
}

/**
 * 路线概览卡片
 */
@Composable
private fun RouteOverviewCard(
    routeResult: RouteResult,
    profile: TravelProfile,
    showInstructions: Boolean,
    onToggleInstructions: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 主要信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 时间
                Text(
                    text = routeResult.getFormattedTime(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = getProfileColor(profile)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // 距离
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
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 展开/收起按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleInstructions)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showInstructions) "收起详情" else "展开详情",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmapBlue
                )
                Icon(
                    imageVector = if (showInstructions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = AmapBlue
                )
            }
        }
    }
}

/**
 * 加载中内容
 */
@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = AmapBlue,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "正在计算路线...",
            style = MaterialTheme.typography.bodyMedium,
            color = Gray500
        )
    }
}

/**
 * 错误内容
 */
@Composable
private fun ErrorContent(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
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

/**
 * 空内容
 */
@Composable
private fun EmptyContent(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Navigation,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Gray400
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = Gray500
        )
    }
}

/**
 * 底部按钮
 */
@Composable
private fun BottomButton(
    onStartNavigation: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Button(
            onClick = onStartNavigation,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AmapBlue
            ),
            shape = RoundedCornerShape(26.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "开始导航",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ========== Previews ==========

@Preview(showBackground = true)
@Composable
private fun RoutePlanningScreenPreview() {
    AmapSimTheme {
        RoutePlanningScreenContent(
            uiState = RoutePlanningUiState(
                startLocation = LocationInput.CurrentLocation,
                endLocation = LocationInput.SpecificLocation(
                    name = "武汉大学",
                    coordinates = LatLng(30.5355, 114.3645),
                    address = "湖北省武汉市武昌区八一路299号"
                ),
                selectedProfile = TravelProfile.CAR,
                routeResult = RouteResult(
                    distance = 5800.0,
                    time = 900000, // 15分钟
                    points = listOf(
                        LatLng(30.5433, 114.3416),
                        LatLng(30.5355, 114.3645)
                    ),
                    instructions = listOf(
                        RouteInstruction(
                            text = "从当前位置出发",
                            distance = 0.0,
                            time = 0,
                            sign = InstructionSign.DEPART,
                            location = LatLng(30.5433, 114.3416),
                            streetName = null,
                            turnAngle = null
                        ),
                        RouteInstruction(
                            text = "左转进入中山大道",
                            distance = 350.0,
                            time = 60000,
                            sign = InstructionSign.LEFT,
                            location = LatLng(30.5433, 114.3416),
                            streetName = "中山大道",
                            turnAngle = -90.0
                        ),
                        RouteInstruction(
                            text = "到达目的地",
                            distance = 0.0,
                            time = 0,
                            sign = InstructionSign.ARRIVE,
                            location = LatLng(30.5355, 114.3645),
                            streetName = null,
                            turnAngle = null
                        )
                    ),
                    profile = "car"
                )
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RoutePlanningEmptyPreview() {
    AmapSimTheme {
        RoutePlanningScreenContent(
            uiState = RoutePlanningUiState(
                startLocation = LocationInput.CurrentLocation,
                endLocation = null
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RoutePlanningLoadingPreview() {
    AmapSimTheme {
        RoutePlanningScreenContent(
            uiState = RoutePlanningUiState(
                startLocation = LocationInput.CurrentLocation,
                endLocation = LocationInput.SpecificLocation(
                    name = "武汉大学",
                    coordinates = LatLng(30.5355, 114.3645)
                ),
                isLoading = true
            ),
            onEvent = {}
        )
    }
}
