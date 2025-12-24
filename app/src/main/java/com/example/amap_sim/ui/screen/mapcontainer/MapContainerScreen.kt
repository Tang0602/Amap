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
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amap_sim.domain.model.RouteResult
import com.example.amap_sim.ui.components.map.MapCommand
import com.example.amap_sim.ui.components.map.MapsforgeMapView
import com.example.amap_sim.ui.screen.mapcontainer.components.MapControls
import com.example.amap_sim.ui.screen.mapcontainer.overlay.detail.DetailOverlay
import com.example.amap_sim.ui.screen.mapcontainer.overlay.home.HomeOverlay
import com.example.amap_sim.ui.screen.mapcontainer.overlay.route.RoutePlanningOverlay
import com.example.amap_sim.ui.screen.mapcontainer.overlay.waypoint.AddWaypointOverlay
import com.example.amap_sim.ui.screen.mapcontainer.overlay.route.TravelProfile
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
                // 添加调试日志
                android.util.Log.d("MapContainerScreen", "AnimatedContent recomposed with overlayState: $overlayState")
                if (overlayState is MapOverlayState.RoutePlanning) {
                    android.util.Log.d("MapContainerScreen", "RoutePlanning params: startLocation=${overlayState.startLocation}, waypoints.size=${overlayState.waypoints?.size ?: "null"}, endLocation=${overlayState.endLocation}")
                }
                when (overlayState) {
                    is MapOverlayState.Home -> {
                        HomeOverlay(
                            mapController = viewModel,
                            onNavigateToSearch = { viewModel.openSearch() },
                            onNavigateToDrive = { viewModel.openRoutePlanning(initialProfile = TravelProfile.CAR) },
                            onNavigateToBike = { viewModel.openRoutePlanning(initialProfile = TravelProfile.BIKE) },
                            onNavigateToWalk = { viewModel.openRoutePlanning(initialProfile = TravelProfile.FOOT) },
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
                        // 使用 key 来强制重新组合，确保参数变化时重新创建组件
                        key(
                            overlayState.startLocation?.hashCode(),
                            overlayState.waypoints?.size ?: 0,
                            overlayState.endLocation?.hashCode()
                        ) {
                            RoutePlanningOverlay(
                                destLat = overlayState.destLat,
                                destLon = overlayState.destLon,
                                destName = overlayState.destName,
                                initialProfile = overlayState.initialProfile,
                                startLocation = overlayState.startLocation,
                                waypoints = overlayState.waypoints,
                                endLocation = overlayState.endLocation,
                                mapController = viewModel,
                                onNavigateBack = { viewModel.navigateBack() },
                                onNavigateToSearch = { viewModel.openSearch() },
                                onNavigateToAddWaypoint = { startLocation, waypoints, endLocation ->
                                    viewModel.openAddWaypoint(startLocation, waypoints, endLocation)
                                },
                                onStartNavigation = { routeResult ->
                                    onNavigateToNavigation(routeResult)
                                }
                            )
                        }
                    }
                    
                    is MapOverlayState.AddWaypoint -> {
                        AddWaypointOverlay(
                            startLocation = overlayState.startLocation,
                            waypoints = overlayState.waypoints,
                            endLocation = overlayState.endLocation,
                            onNavigateBack = { viewModel.navigateBack() },
                            onComplete = { startLocation, waypoints, endLocation ->
                                // 更新 RoutePlanning 状态并返回
                                val currentState = viewModel.uiState.value
                                val history = currentState.overlayHistory
                                val previousState = history.lastOrNull()
                                
                                android.util.Log.d("MapContainerScreen", "onComplete called")
                                android.util.Log.d("MapContainerScreen", "startLocation: $startLocation")
                                android.util.Log.d("MapContainerScreen", "waypoints.size: ${waypoints.size}")
                                android.util.Log.d("MapContainerScreen", "endLocation: $endLocation")
                                android.util.Log.d("MapContainerScreen", "previousState: $previousState")
                                
                                if (previousState is MapOverlayState.RoutePlanning) {
                                    // 创建更新后的 RoutePlanning 状态
                                    // 使用传入的参数，即使 previousState 的参数是 null
                                    val updatedRoutePlanning = previousState.copy(
                                        startLocation = startLocation,
                                        waypoints = waypoints,
                                        endLocation = endLocation
                                    )
                                    
                                    android.util.Log.d("MapContainerScreen", "updatedRoutePlanning.startLocation: ${updatedRoutePlanning.startLocation}")
                                    android.util.Log.d("MapContainerScreen", "updatedRoutePlanning.waypoints.size: ${updatedRoutePlanning.waypoints?.size ?: "null"}")
                                    android.util.Log.d("MapContainerScreen", "updatedRoutePlanning.endLocation: ${updatedRoutePlanning.endLocation}")
                                    
                                    // 直接导航到更新后的 RoutePlanning 状态
                                    // 不添加到历史栈，因为这是返回到之前的状态
                                    viewModel.navigateToOverlay(
                                        updatedRoutePlanning,
                                        addToHistory = false
                                    )
                                } else {
                                    // 如果没有历史状态，直接返回
                                    android.util.Log.d("MapContainerScreen", "No previous state, calling navigateBack")
                                    viewModel.navigateBack()
                                }
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
                    onLocate = { viewModel.moveToCurrentLocation() },
                    onRoute = { viewModel.openRoutePlanning() }
                )
            }
        }
    }
}

