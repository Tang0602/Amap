package com.example.amap_sim.ui.screen.mapcontainer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amap_sim.domain.model.RouteResult
import com.example.amap_sim.ui.screen.map.MapCommand
import com.example.amap_sim.ui.screen.map.components.MapsforgeMapView
import com.example.amap_sim.ui.screen.mapcontainer.components.MapControls
import com.example.amap_sim.ui.screen.mapcontainer.overlay.detail.DetailOverlay
import com.example.amap_sim.ui.screen.mapcontainer.overlay.home.HomeOverlay
import com.example.amap_sim.ui.screen.mapcontainer.overlay.route.RoutePlanningOverlay
import com.example.amap_sim.ui.screen.mapcontainer.overlay.search.SearchOverlay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 地图容器页面
 * 
 * 作为所有 Overlay 的宿主，管理：
 * - 底层地图渲染
 * - Overlay 切换与动画
 * - 返回键处理
 * - 全局地图控制按钮
 */
@Composable
fun MapContainerScreen(
    viewModel: MapContainerViewModel = viewModel(),
    onNavigateToNavigation: (RouteResult) -> Unit = {},
    onNavigateToDrive: () -> Unit = {},
    onNavigateToBike: () -> Unit = {},
    onNavigateToWalk: () -> Unit = {},
    onNavigateToMore: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // 处理返回键
    BackHandler(enabled = uiState.canGoBack) {
        viewModel.navigateBack()
    }
    
    // 显示错误信息
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            scope.launch {
                snackbarHostState.showSnackbar(error)
            }
            viewModel.clearError()
        }
    }
    
    // 将 MapContainerCommand 转换为 MapCommand
    val mapCommands = remember(viewModel) {
        viewModel.mapCommands.map { containerCommand ->
            when (containerCommand) {
                is MapContainerCommand.MoveTo -> MapCommand.MoveTo(
                    containerCommand.position,
                    containerCommand.zoomLevel,
                    containerCommand.animate
                )
                is MapContainerCommand.ZoomTo -> MapCommand.ZoomTo(
                    containerCommand.zoomLevel,
                    containerCommand.animate
                )
                is MapContainerCommand.ZoomIn -> MapCommand.ZoomIn
                is MapContainerCommand.ZoomOut -> MapCommand.ZoomOut
                is MapContainerCommand.FitBounds -> MapCommand.FitBounds(
                    containerCommand.minLat,
                    containerCommand.maxLat,
                    containerCommand.minLon,
                    containerCommand.maxLon,
                    containerCommand.padding
                )
                is MapContainerCommand.Redraw -> MapCommand.Redraw
            }
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
            // 底层地图
            MapsforgeMapView(
                modifier = Modifier.fillMaxSize(),
                center = uiState.center,
                zoomLevel = uiState.zoomLevel,
                markers = uiState.markers,
                routeResult = uiState.routeResult,
                currentLocation = uiState.currentLocation,
                showCurrentLocation = uiState.showCurrentLocation,
                commands = mapCommands,
                onMapReady = viewModel::onMapReady,
                onMapClick = viewModel::onMapClick,
                onMapLongPress = viewModel::onMapLongPress,
                onMarkerClick = viewModel::onMarkerClick,
                onMapMoveEnd = viewModel::onMapMoveEnd
            )
            
            // Overlay 层
            AnimatedContent(
                targetState = uiState.overlayState,
                transitionSpec = {
                    // 根据不同的状态切换使用不同的动画
                    when {
                        // 从 Home 到其他：向上滑入
                        initialState is MapOverlayState.Home -> {
                            (slideInVertically { it / 3 } + fadeIn()) togetherWith
                                    (slideOutVertically { -it / 3 } + fadeOut())
                        }
                        // 返回到 Home：向下滑出
                        targetState is MapOverlayState.Home -> {
                            (slideInVertically { -it / 3 } + fadeIn()) togetherWith
                                    (slideOutVertically { it / 3 } + fadeOut())
                        }
                        // 其他情况：淡入淡出
                        else -> fadeIn() togetherWith fadeOut()
                    }
                },
                label = "overlay_transition"
            ) { overlayState ->
                when (overlayState) {
                    is MapOverlayState.Home -> {
                        HomeOverlay(
                            mapController = viewModel,
                            onNavigateToSearch = { viewModel.openSearch() },
                            onNavigateToRoutePlanning = { viewModel.openRoutePlanning() },
                            onNavigateToDrive = onNavigateToDrive,
                            onNavigateToBike = onNavigateToBike,
                            onNavigateToWalk = onNavigateToWalk,
                            onNavigateToMore = onNavigateToMore
                        )
                    }
                    
                    is MapOverlayState.Search -> {
                        SearchOverlay(
                            mapController = viewModel,
                            onNavigateBack = { viewModel.navigateBack() },
                            onPoiSelected = { poi ->
                                viewModel.openPoiDetail(poi.id.toString())
                            }
                        )
                    }
                    
                    is MapOverlayState.Detail -> {
                        DetailOverlay(
                            poiId = overlayState.poiId,
                            mapController = viewModel,
                            onNavigateBack = { viewModel.navigateBack() },
                            onNavigateToRoute = { lat, lon, name ->
                                viewModel.openRoutePlanning(lat, lon, name)
                            }
                        )
                    }
                    
                    is MapOverlayState.RoutePlanning -> {
                        RoutePlanningOverlay(
                            destLat = overlayState.destLat,
                            destLon = overlayState.destLon,
                            destName = overlayState.destName,
                            mapController = viewModel,
                            onNavigateBack = { viewModel.navigateBack() },
                            onNavigateToSearch = { viewModel.openSearch() },
                            onStartNavigation = { routeResult ->
                                onNavigateToNavigation(routeResult)
                            }
                        )
                    }
                }
            }
            
            // 地图控制按钮（仅在 Home 和 Detail 状态显示）
            AnimatedVisibility(
                visible = uiState.overlayState is MapOverlayState.Home || 
                         uiState.overlayState is MapOverlayState.Detail,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                MapControls(
                    onZoomIn = { viewModel.zoomIn() },
                    onZoomOut = { viewModel.zoomOut() },
                    onLocate = { viewModel.moveToCurrentLocation() }
                )
            }
        }
    }
}

