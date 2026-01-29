package com.example.amap_sim.ui.screen.mapcontainer.overlay.nearby

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amap_sim.domain.model.PoiResult
import com.example.amap_sim.ui.theme.AmapBlue

/**
 * 周边搜索 Overlay
 */
@Composable
fun NearbyOverlay(
    onClose: () -> Unit,
    onPoiSelected: (PoiResult) -> Unit,
    onSearchTriggered: () -> Unit,
    mapController: com.example.amap_sim.ui.screen.mapcontainer.MapStateController,
    initialCenter: com.example.amap_sim.domain.model.LatLng? = null,
    excludePoiId: String? = null,
    viewModel: NearbyViewModel = viewModel()
) {
    android.util.Log.d("NearbyOverlay", "NearbyOverlay 正在渲染, initialCenter=$initialCenter, excludePoiId=$excludePoiId")
    val uiState by viewModel.uiState.collectAsState()

    // 如果有初始中心点，自动触发搜索
    LaunchedEffect(initialCenter, excludePoiId) {
        initialCenter?.let { center ->
            viewModel.onEvent(NearbyEvent.Search(center, excludePoiId))
        }
    }

    // 监听导航事件
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is NearbyNavigationEvent.NavigateToPoiDetail -> {
                    onPoiSelected(event.poi)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 顶部栏
        TopBar(onClose = onClose)

        // 分类筛选
        CategoryFilter(
            selectedCategory = uiState.selectedCategory,
            onCategorySelected = { category ->
                viewModel.onEvent(NearbyEvent.SelectCategory(category))
            }
        )

        Divider()

        // 搜索按钮
        SearchButton(
            isLoading = uiState.isLoading,
            searchCenter = initialCenter,
            onSearchClick = {
                // 如果有指定的中心点（从POI详情页打开），使用该中心点
                // 否则使用当前位置
                val center = initialCenter ?: mapController.getCurrentLocation()
                center?.let { location ->
                    viewModel.onEvent(NearbyEvent.Search(location, excludePoiId))
                }
            }
        )

        Divider()

        // 结果列表
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            ErrorView(
                error = uiState.error!!,
                onDismiss = { viewModel.onEvent(NearbyEvent.ClearError) }
            )
        } else if (uiState.searchResults.isEmpty() && uiState.center != null) {
            EmptyView()
        } else {
            ResultList(
                results = uiState.searchResults,
                onPoiClick = { poi ->
                    viewModel.onEvent(NearbyEvent.SelectPoi(poi))
                }
            )
        }
    }
}

/**
 * 顶部栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(onClose: () -> Unit) {
    TopAppBar(
        title = { Text("周边搜索") },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, "关闭")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

/**
 * 分类筛选
 */
@Composable
private fun CategoryFilter(
    selectedCategory: NearbyCategory?,
    onCategorySelected: (NearbyCategory) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(NearbyCategory.entries) { category ->
            CategoryChip(
                category = category,
                isSelected = selectedCategory == category,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

/**
 * 分类筛选项
 */
@Composable
private fun CategoryChip(
    category: NearbyCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(20.dp)),
        color = if (isSelected) AmapBlue else MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = when (category) {
                    NearbyCategory.FOOD -> Icons.Default.Restaurant
                    NearbyCategory.HOTEL -> Icons.Default.Hotel
                    NearbyCategory.ATTRACTION -> Icons.Default.Attractions
                    NearbyCategory.ALL -> Icons.Default.Apps
                },
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 搜索按钮
 */
@Composable
private fun SearchButton(
    isLoading: Boolean,
    searchCenter: com.example.amap_sim.domain.model.LatLng?,
    onSearchClick: () -> Unit
) {
    Button(
        onClick = onSearchClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = AmapBlue
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            if (isLoading) {
                "搜索中..."
            } else if (searchCenter != null) {
                "搜索此地点周边"
            } else {
                "搜索当前位置周边"
            }
        )
    }
}

/**
 * 结果列表
 */
@Composable
private fun ResultList(
    results: List<PoiResult>,
    onPoiClick: (PoiResult) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "找到 ${results.size} 个地点",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }

        items(results) { poi ->
            PoiResultItem(
                poi = poi,
                onClick = { onPoiClick(poi) }
            )
            Divider()
        }
    }
}

/**
 * POI 结果项
 */
@Composable
private fun PoiResultItem(
    poi: PoiResult,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = poi.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Column {
                Text(
                    text = poi.getCategoryDisplayName(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!poi.address.isNullOrEmpty()) {
                    Text(
                        text = poi.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                poi.distance?.let { distance ->
                    if (distance > 0) {
                        Text(
                            text = formatDistance(distance),
                            style = MaterialTheme.typography.bodySmall,
                            color = AmapBlue
                        )
                    }
                }
            }
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = AmapBlue
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

/**
 * 空状态视图
 */
@Composable
private fun EmptyView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "未找到周边地点",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 错误视图
 */
@Composable
private fun ErrorView(
    error: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Button(onClick = onDismiss) {
                Text("关闭")
            }
        }
    }
}

/**
 * 格式化距离
 */
private fun formatDistance(meters: Double): String {
    return when {
        meters < 1000 -> "${meters.toInt()}m"
        else -> String.format("%.1fkm", meters / 1000)
    }
}
