package com.example.amap_sim.ui.screen.mapcontainer.overlay.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Attractions
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amap_sim.domain.model.PoiResult
import com.example.amap_sim.ui.components.SearchBarInput
import com.example.amap_sim.ui.screen.mapcontainer.MapStateController
import com.example.amap_sim.ui.screen.mapcontainer.overlay.search.components.PoiListItem
import com.example.amap_sim.ui.theme.AmapBlue
import com.example.amap_sim.ui.theme.Gray400
import com.example.amap_sim.ui.theme.Gray500

/**
 * 搜索 Overlay
 * 
 * 复用 SearchViewModel，在地图上叠加搜索界面
 * 搜索结果可在地图上显示标记点
 */
@Composable
fun SearchOverlay(
    mapController: MapStateController,
    onNavigateBack: () -> Unit,
    onPoiSelected: (PoiResult) -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    
    // 监听导航事件
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is SearchNavigationEvent.NavigateToPoiDetail -> onPoiSelected(event.poi)
                is SearchNavigationEvent.NavigateToRoute -> { /* TODO: 导航到路线规划 */ }
            }
        }
    }
    
    // 监听地图更新状态，应用 ViewModel 计算的地图操作
    // UI 层只负责执行，不包含业务逻辑
    LaunchedEffect(uiState.mapUpdate) {
        when (val update = uiState.mapUpdate) {
            null -> { /* 无更新 */ }
            is SearchMapUpdate.Clear -> {
                mapController.clearMarkers()
            }
            is SearchMapUpdate.ShowMarkers -> {
                mapController.setMarkers(update.markers)
                
                when {
                    update.fitBounds && update.bounds != null -> {
                        mapController.fitBounds(
                            minLat = update.bounds.minLat,
                            maxLat = update.bounds.maxLat,
                            minLon = update.bounds.minLon,
                            maxLon = update.bounds.maxLon
                        )
                    }
                    update.moveToPosition && update.position != null -> {
                        mapController.moveTo(update.position, update.zoomLevel)
                    }
                }
            }
        }
    }
    
    SearchOverlayContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = {
            mapController.clearMarkers()
            onNavigateBack()
        }
    )
}

@Composable
private fun SearchOverlayContent(
    uiState: SearchUiState,
    onEvent: (SearchEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 搜索框
            SearchBarInput(
                value = uiState.query,
                onValueChange = { onEvent(SearchEvent.UpdateQuery(it)) },
                onBackClick = onNavigateBack,
                onSearch = { onEvent(SearchEvent.Search(it)) },
                onClear = { onEvent(SearchEvent.ClearSearch) },
                autoFocus = !uiState.showResults,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // 内容区域
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.showResults -> {
                        SearchResultsContent(
                            results = uiState.searchResults,
                            isLoading = uiState.isLoading,
                            error = uiState.error,
                            selectedCategory = uiState.selectedCategory,
                            onPoiClick = { onEvent(SearchEvent.SelectPoi(it)) }
                        )
                    }
                    else -> {
                        InitialContent(
                            categories = uiState.popularCategories,
                            searchHistory = uiState.searchHistory,
                            onCategoryClick = { category ->
                                focusManager.clearFocus()
                                onEvent(SearchEvent.SelectCategory(category))
                            },
                            onHistoryClick = { query ->
                                focusManager.clearFocus()
                                onEvent(SearchEvent.SelectHistory(query))
                            },
                            onClearHistory = { onEvent(SearchEvent.ClearHistory) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 初始内容（分类 + 搜索历史）
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InitialContent(
    categories: List<CategoryItem>,
    searchHistory: List<String>,
    onCategoryClick: (String) -> Unit,
    onHistoryClick: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        // 搜索历史
        if (searchHistory.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "搜索历史",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = onClearHistory) {
                        Text(
                            text = "清除",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray500
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    searchHistory.forEach { history ->
                        HistoryChip(
                            text = history,
                            onClick = { onHistoryClick(history) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        
        // 热门分类
        item {
            Text(
                text = "热门分类",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                categories.forEach { category ->
                    CategoryChip(
                        category = category,
                        onClick = { onCategoryClick(category.id) }
                    )
                }
            }
        }
    }
}

/**
 * 搜索历史 Chip
 */
@Composable
private fun HistoryChip(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Gray500
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 分类 Chip
 */
@Composable
private fun CategoryChip(
    category: CategoryItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getCategoryIcon(category.id),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = AmapBlue
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * 搜索结果内容
 */
@Composable
private fun SearchResultsContent(
    results: List<PoiResult>,
    isLoading: Boolean,
    error: String?,
    selectedCategory: String?,
    onPoiClick: (PoiResult) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = AmapBlue
                )
            }
            error != null -> {
                ErrorContent(
                    message = error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            results.isEmpty() -> {
                EmptyResultContent(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedCategory != null) {
                                    "共 ${results.size} 个${getCategoryDisplayName(selectedCategory)}"
                                } else {
                                    "共 ${results.size} 个结果"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray500
                            )
                        }
                    }
                    
                    items(
                        items = results,
                        key = { it.id }
                    ) { poi ->
                        PoiListItem(
                            poi = poi,
                            onClick = { onPoiClick(poi) },
                            showDivider = poi != results.last()
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

/**
 * 空结果内容
 */
@Composable
private fun EmptyResultContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Gray400
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "未找到相关结果",
            style = MaterialTheme.typography.bodyLarge,
            color = Gray500
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "换个关键词试试吧",
            style = MaterialTheme.typography.bodyMedium,
            color = Gray400,
            textAlign = TextAlign.Center
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
            text = "搜索出错了",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Gray500,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 获取分类图标
 */
private fun getCategoryIcon(categoryId: String): ImageVector {
    return when (categoryId) {
        "餐饮" -> Icons.Default.Restaurant
        "住宿" -> Icons.Default.Hotel
        "交通" -> Icons.Default.DirectionsBus
        "购物" -> Icons.Default.ShoppingCart
        "金融" -> Icons.Default.AccountBalance
        "医疗" -> Icons.Default.LocalHospital
        "教育" -> Icons.Default.School
        "休闲" -> Icons.Default.SportsEsports
        "景点" -> Icons.Default.Attractions
        "政务" -> Icons.Default.AccountBalance
        "生活服务" -> Icons.Default.HomeRepairService
        "办公" -> Icons.Default.Business
        else -> Icons.Default.LocationOn
    }
}

/**
 * 获取分类显示名称
 */
private fun getCategoryDisplayName(categoryId: String): String {
    return when (categoryId) {
        "餐饮" -> "美食"
        "住宿" -> "酒店"
        "交通" -> "交通"
        "购物" -> "购物"
        "金融" -> "银行"
        "医疗" -> "医疗"
        "教育" -> "教育"
        "休闲" -> "休闲"
        "景点" -> "景点"
        "政务" -> "政务"
        "生活服务" -> "生活"
        "办公" -> "办公"
        else -> categoryId
    }
}

