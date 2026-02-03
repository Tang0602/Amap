package com.example.amap_sim.ui.screen.mapcontainer.overlay.waypoint

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.amap_sim.ui.screen.mapcontainer.overlay.route.LocationInput
import com.example.amap_sim.ui.screen.mapcontainer.overlay.search.components.PoiListItem
import com.example.amap_sim.ui.screen.mapcontainer.overlay.waypoint.components.QuickSelectButtons
import com.example.amap_sim.ui.screen.mapcontainer.overlay.waypoint.components.WaypointInputField
import com.example.amap_sim.ui.theme.AmapBlue
import com.example.amap_sim.ui.theme.Gray500

/**
 * 添加途径点 Overlay
 * 
 * 用于管理路线规划中的途径点
 */
@Composable
fun AddWaypointOverlay(
    startLocation: LocationInput = LocationInput.CurrentLocation,
    waypoints: List<LocationInput> = emptyList(),
    endLocation: LocationInput? = null,
    onNavigateBack: () -> Unit,
    onComplete: (LocationInput, List<LocationInput>, LocationInput?) -> Unit,
    onOpenFavorites: () -> Unit = {},
    viewModel: WaypointViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 初始化数据
    LaunchedEffect(startLocation, waypoints, endLocation) {
        viewModel.initialize(startLocation, waypoints, endLocation)
    }

    // 监听导航事件
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is WaypointNavigationEvent.Complete -> {
                    // 先调用 onComplete，让 MapContainerScreen 更新状态
                    // 不要调用 onNavigateBack()，因为 onComplete 中已经处理了导航
                    onComplete(event.startLocation, event.waypoints, event.endLocation)
                }
                is WaypointNavigationEvent.OpenFavorites -> {
                    // 打开收藏夹 Overlay
                    onOpenFavorites()
                }
            }
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 顶部面板（包含顶部栏、输入字段列表、操作按钮行）
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Column {
                    // 顶部栏
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Text(
                            text = "添加途径点",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // 输入字段列表
                    // 构建完整的地点列表（起点 + 途径点 + 终点）
                    val allLocations = buildList {
                        add(-1 to uiState.startLocation) // 起点
                        uiState.waypoints.forEachIndexed { index, waypoint ->
                            add(index to waypoint) // 途径点
                        }
                        uiState.endLocation?.let { add(-2 to it) } // 终点
                    }

                    // 当途径点数量 >= 2 时，使用可滚动列表
                    if (uiState.waypoints.size >= 2) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                itemsIndexed(
                                    items = allLocations,
                                    key = { _, (index, _) -> index }
                                ) { globalIndex, (index, location) ->
                                    Column {
                                        WaypointInputField(
                                            index = index,
                                            location = location,
                                            isEditing = uiState.editingIndex == index,
                                            searchKeyword = if (uiState.editingIndex == index) uiState.searchKeyword else "",
                                            onFieldClick = { viewModel.onEvent(WaypointEvent.StartEditing(index)) },
                                            onDeleteClick = if (index >= 0) {
                                                { viewModel.onEvent(WaypointEvent.RemoveWaypoint(index)) }
                                            } else null,
                                            onSearchKeywordChange = {
                                                viewModel.onEvent(WaypointEvent.UpdateSearchKeyword(it))
                                            }
                                        )
                                        if (globalIndex < allLocations.size - 1) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // 途径点数量 < 2 时，使用固定列表
                        Column {
                            allLocations.forEachIndexed { globalIndex, (index, location) ->
                                WaypointInputField(
                                    index = index,
                                    location = location,
                                    isEditing = uiState.editingIndex == index,
                                    searchKeyword = if (uiState.editingIndex == index) uiState.searchKeyword else "",
                                    onFieldClick = { viewModel.onEvent(WaypointEvent.StartEditing(index)) },
                                    onDeleteClick = if (index >= 0) {
                                        { viewModel.onEvent(WaypointEvent.RemoveWaypoint(index)) }
                                    } else null,
                                    onSearchKeywordChange = {
                                        viewModel.onEvent(WaypointEvent.UpdateSearchKeyword(it))
                                    }
                                )
                                if (globalIndex < allLocations.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                    
                    // 操作按钮行
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 添加途径点按钮
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    enabled = uiState.canAddMore,
                                    onClick = { viewModel.onEvent(WaypointEvent.AddWaypoint) }
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AddCircleOutline,
                                contentDescription = "添加途径点",
                                tint = if (uiState.canAddMore) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    Gray500
                                },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "添加途经点",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (uiState.canAddMore) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        Gray500
                                    }
                                )
                                if (uiState.remainingWaypoints > 0) {
                                    Text(
                                        text = "还可添加${uiState.remainingWaypoints}个",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Gray500
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // 完成按钮
                        Button(
                            onClick = { viewModel.onEvent(WaypointEvent.Complete) },
                            colors = ButtonDefaults.buttonColors(containerColor = AmapBlue),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text(
                                text = "完成",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            // 内容区域（快捷按钮选项+建议位置列表，可滚动）
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                // 快捷选择按钮（无搜索关键词时显示）
                if (uiState.showQuickButtons) {
                    item {
                        QuickSelectButtons(
                            onMyLocationClick = {
                                viewModel.onEvent(WaypointEvent.QuickSelectMyLocation)
                            },
                            onFavoritesClick = {
                                viewModel.onEvent(WaypointEvent.QuickSelectFavorites)
                            },
                            onHomeClick = {
                                viewModel.onEvent(WaypointEvent.QuickSelectHome)
                            },
                            onCompanyClick = {
                                viewModel.onEvent(WaypointEvent.QuickSelectCompany)
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                
                // 搜索结果列表（有搜索关键词时显示）
                if (uiState.showSearchResults) {
                    items(uiState.searchResults) { poi ->
                        PoiListItem(
                            poi = poi,
                            onClick = {
                                viewModel.onEvent(WaypointEvent.SelectSearchResult(poi))
                            },
                            onNavigateClick = null
                        )
                    }
                }
                
                // 建议位置列表（无搜索关键词时显示）
                if (uiState.showSuggestions) {
                    item {
                        Text(
                            text = "建议位置",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    items(uiState.suggestedLocations) { poi ->
                        PoiListItem(
                            poi = poi,
                            onClick = {
                                viewModel.onEvent(WaypointEvent.SelectSuggestedLocation(poi))
                            },
                            onNavigateClick = null
                        )
                    }
                }
                
                // 加载状态
                if (uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = AmapBlue
                            )
                        }
                    }
                }
                
                // 底部间距
                item {
                    Spacer(modifier = Modifier.height(16.dp).navigationBarsPadding())
                }
            }
        }
    }
}
