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
        TopBar(
            onClose = onClose,
            isShowingRanking = uiState.rankingCategory != null,
            rankingCategory = uiState.rankingCategory,
            onBackFromRanking = { viewModel.onEvent(NearbyEvent.HideRanking) }
        )

        // 根据是否显示排行榜来决定显示内容
        if (uiState.rankingCategory != null) {
            // 显示排行榜
            CategoryRankingContent(
                uiState = uiState,
                category = uiState.rankingCategory!!,
                onPoiClick = { poi ->
                    viewModel.onEvent(NearbyEvent.SelectPoi(poi))
                }
            )
        } else {
            // 显示搜索界面
            SearchContent(
                uiState = uiState,
                initialCenter = initialCenter,
                excludePoiId = excludePoiId,
                mapController = mapController,
                viewModel = viewModel
            )
        }
    }
}

/**
 * 顶部栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    onClose: () -> Unit,
    isShowingRanking: Boolean,
    rankingCategory: NearbyCategory?,
    onBackFromRanking: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                if (isShowingRanking && rankingCategory != null) {
                    "${rankingCategory.displayName}排行榜"
                } else {
                    "周边搜索"
                }
            )
        },
        navigationIcon = {
            IconButton(onClick = if (isShowingRanking) onBackFromRanking else onClose) {
                Icon(
                    if (isShowingRanking) Icons.Default.ArrowBack else Icons.Default.Close,
                    if (isShowingRanking) "返回" else "关闭"
                )
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

/**
 * 搜索模式内容
 */
@Composable
private fun SearchContent(
    uiState: NearbyUiState,
    initialCenter: com.example.amap_sim.domain.model.LatLng?,
    excludePoiId: String?,
    mapController: com.example.amap_sim.ui.screen.mapcontainer.MapStateController,
    viewModel: NearbyViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 分类筛选
        CategoryFilter(
            selectedCategory = uiState.selectedCategory,
            onCategorySelected = { category ->
                viewModel.onEvent(NearbyEvent.SelectCategory(category))
            }
        )

        Divider()

        // 搜索按钮和排行榜入口
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 搜索按钮
            Button(
                onClick = {
                    val center = initialCenter ?: mapController.getCurrentLocation()
                    center?.let { location ->
                        viewModel.onEvent(NearbyEvent.Search(location, excludePoiId))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmapBlue
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    if (uiState.isLoading) {
                        "搜索中..."
                    } else if (initialCenter != null) {
                        "搜索此地点周边"
                    } else {
                        "搜索当前位置周边"
                    }
                )
            }

            // 排行榜入口按钮（仅在选择了美食/酒店/景点分类时显示）
            if (uiState.selectedCategory != null &&
                uiState.selectedCategory != NearbyCategory.ALL
            ) {
                OutlinedButton(
                    onClick = {
                        val center = initialCenter ?: mapController.getCurrentLocation()
                        center?.let { location ->
                            viewModel.onEvent(
                                NearbyEvent.ShowCategoryRanking(
                                    uiState.selectedCategory!!,
                                    location
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AmapBlue
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("查看${uiState.selectedCategory!!.displayName}排行榜")
                }
            }
        }

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
 * 分类排行榜内容
 */
@Composable
private fun CategoryRankingContent(
    uiState: NearbyUiState,
    category: NearbyCategory,
    onPoiClick: (PoiResult) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 排行榜说明
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when (category) {
                        NearbyCategory.FOOD -> Icons.Default.Restaurant
                        NearbyCategory.HOTEL -> Icons.Default.Hotel
                        NearbyCategory.ATTRACTION -> Icons.Default.Attractions
                        else -> Icons.Default.Apps
                    },
                    contentDescription = null,
                    tint = AmapBlue,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${category.displayName}排行榜",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "按评分排序 · 周边10km范围",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Divider()

        // 排行榜列表
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
                onDismiss = { /* 错误已在状态中 */ }
            )
        } else if (uiState.rankingList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "暂无${category.displayName}排行榜数据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "附近可能没有足够的${category.displayName}数据",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Text(
                        text = "Top ${uiState.rankingList.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                items(uiState.rankingList) { poi ->
                    val rank = uiState.rankingList.indexOf(poi) + 1
                    RankingPoiItem(
                        poi = poi,
                        rank = rank,
                        onClick = { onPoiClick(poi) }
                    )
                    Divider()
                }
            }
        }
    }
}

/**
 * 排行榜POI项
 */
@Composable
private fun RankingPoiItem(
    poi: PoiResult,
    rank: Int,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 排名徽章
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (rank) {
                        1 -> Color(0xFFFFD700) // 金色
                        2 -> Color(0xFFC0C0C0) // 银色
                        3 -> Color(0xFFCD7F32) // 铜色
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = "$rank",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (rank <= 3) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = poi.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        supportingContent = {
            Column {
                // 评分
                poi.rating?.let { rating ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = String.format("%.1f", rating),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFFB300)
                        )
                    }
                }
                // 地址
                if (!poi.address.isNullOrEmpty()) {
                    Text(
                        text = poi.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // 距离
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
        trailingContent = {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
