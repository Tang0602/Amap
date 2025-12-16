package com.example.amap_sim.ui.screen.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amap_sim.domain.model.LatLng
import com.example.amap_sim.ui.screen.map.components.MapControlPanel
import com.example.amap_sim.ui.screen.map.components.MapsforgeMapView
import kotlinx.coroutines.launch

/**
 * 地图页面
 * 
 * 主地图页面，展示离线地图、标记点、路线等
 */
@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(),
    onNavigateToSearch: () -> Unit = {},
    onNavigateToRoute: (start: LatLng, end: LatLng) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // 显示错误信息
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            scope.launch {
                snackbarHostState.showSnackbar(error)
            }
            viewModel.clearError()
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
            // 地图组件
            MapsforgeMapView(
                modifier = Modifier.fillMaxSize(),
                center = uiState.center,
                zoomLevel = uiState.zoomLevel,
                markers = uiState.markers,
                routeResult = uiState.routeResult,
                currentLocation = uiState.currentLocation,
                showCurrentLocation = uiState.showCurrentLocation,
                commands = viewModel.mapCommands,
                onMapReady = {
                    viewModel.onMapReady()
                },
                onMapClick = { position ->
                    viewModel.onMapClick(position)
                },
                onMapLongPress = { position ->
                    viewModel.onMapLongPress(position)
                },
                onMarkerClick = { marker ->
                    viewModel.onMarkerClick(marker)
                },
                onMapMoveEnd = { center, zoom ->
                    viewModel.onMapMoveEnd(center, zoom)
                }
            )
            
            // 地图控制按钮（右下角）
            MapControlPanel(
                onZoomIn = { viewModel.zoomIn() },
                onZoomOut = { viewModel.zoomOut() },
                onLocationClick = { viewModel.moveToCurrentLocation() },
                showLocationButton = true,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 100.dp) // 给底部导航栏留出空间
            )
            
            // 加载指示器
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                    uiState.loadingMessage?.let { message ->
                        Text(
                            text = message,
                            modifier = Modifier.padding(top = 60.dp)
                        )
                    }
                }
            }
        }
    }
}
