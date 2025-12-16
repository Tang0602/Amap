package com.example.amap_sim.ui.screen.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amap_sim.domain.model.InstructionSign
import com.example.amap_sim.domain.model.LatLng
import com.example.amap_sim.domain.model.MarkerData
import com.example.amap_sim.domain.model.MarkerType
import com.example.amap_sim.domain.model.RouteInstruction
import com.example.amap_sim.domain.model.RouteResult
import com.example.amap_sim.ui.screen.map.MapCommand
import com.example.amap_sim.ui.screen.map.components.MapsforgeMapView
import com.example.amap_sim.ui.screen.navigation.components.ArrivalCard
import com.example.amap_sim.ui.screen.navigation.components.CurrentInstructionCard
import com.example.amap_sim.ui.screen.navigation.components.NavigationControls
import com.example.amap_sim.ui.screen.navigation.components.NavigationInfoBar
import com.example.amap_sim.ui.screen.navigation.components.NavigationStateIndicator
import com.example.amap_sim.ui.theme.AmapSimTheme
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * 导航页面
 * 
 * 提供实时导航体验：
 * - 当前指令显示
 * - 地图跟随
 * - 剩余时间/距离
 * - 暂停/继续/结束控制
 */
@Composable
fun NavigationScreen(
    routeResult: RouteResult,
    onNavigateBack: () -> Unit,
    onNavigationFinished: () -> Unit,
    viewModel: NavigationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 初始化路线
    LaunchedEffect(routeResult) {
        viewModel.setRoute(routeResult)
    }
    
    // 监听导航事件
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                NavigationNavigationEvent.Back -> onNavigateBack()
                NavigationNavigationEvent.NavigationFinished -> onNavigationFinished()
            }
        }
    }
    
    // 显示错误
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(NavigationEvent.ClearError)
        }
    }
    
    // 自动开始导航
    LaunchedEffect(uiState.navigationState) {
        if (uiState.navigationState == NavigationState.NOT_STARTED && uiState.routeResult != null) {
            viewModel.onEvent(NavigationEvent.StartNavigation)
        }
    }
    
    NavigationScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
        onFinished = onNavigationFinished
    )
}

@Composable
private fun NavigationScreenContent(
    uiState: NavigationUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (NavigationEvent) -> Unit,
    onFinished: () -> Unit
) {
    val routeResult = uiState.routeResult
    
    // 地图命令流
    val mapCommands = remember { MutableSharedFlow<MapCommand>(extraBufferCapacity = 10) }
    
    // 跟随模式下更新地图位置
    LaunchedEffect(uiState.currentLocation, uiState.isFollowingUser) {
        if (uiState.isFollowingUser && !uiState.isOverviewMode) {
            mapCommands.emit(
                MapCommand.MoveTo(
                    position = uiState.currentLocation,
                    zoomLevel = uiState.zoomLevel
                )
            )
        }
    }
    
    // 全览模式下显示整条路线
    LaunchedEffect(uiState.isOverviewMode) {
        if (uiState.isOverviewMode && routeResult != null) {
            val boundingBox = routeResult.getBoundingBox()
            if (boundingBox != null) {
                mapCommands.emit(
                    MapCommand.FitBounds(
                        minLat = boundingBox.minLat,
                        maxLat = boundingBox.maxLat,
                        minLon = boundingBox.minLon,
                        maxLon = boundingBox.maxLon,
                        padding = 100
                    )
                )
            }
        }
    }
    
    // 构建标记点列表
    val markers = remember(uiState.currentLocation, routeResult) {
        buildList {
            // 当前位置标记
            add(
                MarkerData(
                    id = "current_location",
                    position = uiState.currentLocation,
                    title = "当前位置",
                    type = MarkerType.CURRENT_LOCATION,
                    anchorY = 0.5f
                )
            )
            
            // 终点标记
            routeResult?.points?.lastOrNull()?.let { end ->
                add(
                    MarkerData(
                        id = "end",
                        position = end,
                        title = "目的地",
                        type = MarkerType.END
                    )
                )
            }
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 地图层
            MapsforgeMapView(
                modifier = Modifier.fillMaxSize(),
                center = uiState.currentLocation,
                zoomLevel = uiState.zoomLevel,
                markers = markers,
                routeResult = routeResult,
                commands = mapCommands,
                onMapClick = { position ->
                    onEvent(NavigationEvent.MapClick(position))
                }
            )
            
            // 顶部指令卡片
            AnimatedVisibility(
                visible = uiState.currentInstruction != null && !uiState.hasArrived,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            ) {
                CurrentInstructionCard(
                    instruction = uiState.currentInstruction,
                    nextInstruction = uiState.nextInstruction,
                    distanceToNext = uiState.getFormattedDistanceToNext()
                )
            }
            
            // 导航状态指示器
            NavigationStateIndicator(
                state = uiState.navigationState,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 160.dp)
            )
            
            // 右侧控制按钮
            NavigationControls(
                navigationState = uiState.navigationState,
                isFollowingUser = uiState.isFollowingUser,
                isOverviewMode = uiState.isOverviewMode,
                onToggleFollow = { onEvent(NavigationEvent.ToggleFollowMode) },
                onToggleOverview = { onEvent(NavigationEvent.ToggleOverviewMode) },
                onPause = { onEvent(NavigationEvent.PauseNavigation) },
                onResume = { onEvent(NavigationEvent.ResumeNavigation) },
                onStop = { onEvent(NavigationEvent.StopNavigation) },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            )
            
            // 底部信息栏
            AnimatedVisibility(
                visible = !uiState.hasArrived,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                NavigationInfoBar(
                    remainingDistance = uiState.getFormattedRemainingDistance(),
                    remainingTime = uiState.getFormattedRemainingTime(),
                    estimatedArrival = uiState.estimatedArrivalTime,
                    currentSpeed = uiState.currentSpeed
                )
            }
            
            // 到达提示卡片
            AnimatedVisibility(
                visible = uiState.hasArrived,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp)
            ) {
                ArrivalCard(
                    destinationName = routeResult?.instructions?.lastOrNull()?.streetName,
                    onDismiss = onFinished
                )
            }
        }
    }
}

// ========== Previews ==========

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun NavigationScreenPreview() {
    val mockRoute = RouteResult(
        distance = 5800.0,
        time = 900000,
        points = listOf(
            LatLng(30.5433, 114.3416),
            LatLng(30.5450, 114.3500),
            LatLng(30.5355, 114.3645)
        ),
        instructions = listOf(
            RouteInstruction(
                text = "出发",
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
                text = "右转进入解放大道",
                distance = 500.0,
                time = 90000,
                sign = InstructionSign.RIGHT,
                location = LatLng(30.5450, 114.3500),
                streetName = "解放大道",
                turnAngle = 90.0
            ),
            RouteInstruction(
                text = "到达目的地",
                distance = 0.0,
                time = 0,
                sign = InstructionSign.ARRIVE,
                location = LatLng(30.5355, 114.3645),
                streetName = "武汉大学",
                turnAngle = null
            )
        ),
        profile = "car"
    )
    
    AmapSimTheme {
        NavigationScreenContent(
            uiState = NavigationUiState(
                routeResult = mockRoute,
                currentLocation = LatLng(30.5433, 114.3416),
                currentInstructionIndex = 1,
                currentInstruction = mockRoute.instructions[1],
                nextInstruction = mockRoute.instructions[2],
                distanceToNextInstruction = 300.0,
                remainingDistance = 5500.0,
                remainingTime = 840000,
                currentSpeed = 42.0,
                navigationState = NavigationState.NAVIGATING,
                estimatedArrivalTime = "14:35"
            ),
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
            onFinished = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NavigationScreenArrivedPreview() {
    val mockRoute = RouteResult(
        distance = 5800.0,
        time = 900000,
        points = listOf(
            LatLng(30.5433, 114.3416),
            LatLng(30.5355, 114.3645)
        ),
        instructions = listOf(
            RouteInstruction(
                text = "到达目的地",
                distance = 0.0,
                time = 0,
                sign = InstructionSign.ARRIVE,
                location = LatLng(30.5355, 114.3645),
                streetName = "武汉大学",
                turnAngle = null
            )
        ),
        profile = "car"
    )
    
    AmapSimTheme {
        NavigationScreenContent(
            uiState = NavigationUiState(
                routeResult = mockRoute,
                currentLocation = LatLng(30.5355, 114.3645),
                currentInstruction = mockRoute.instructions.last(),
                navigationState = NavigationState.ARRIVED,
                remainingDistance = 0.0,
                remainingTime = 0
            ),
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
            onFinished = {}
        )
    }
}

