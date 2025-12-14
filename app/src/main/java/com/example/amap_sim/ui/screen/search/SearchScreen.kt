package com.example.amap_sim.ui.screen.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Attractions
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Subway
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amap_sim.domain.model.PoiResult
import com.example.amap_sim.ui.components.SearchBarInput
import com.example.amap_sim.ui.screen.search.components.PoiListItem
import com.example.amap_sim.ui.theme.AmapBlue
import com.example.amap_sim.ui.theme.AmapSimTheme
import com.example.amap_sim.ui.theme.Gray400
import com.example.amap_sim.ui.theme.Gray500

/**
 * 搜索页面
 */
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onPoiSelected: (PoiResult) -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 监听导航事件
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is NavigationEvent.NavigateToPoiDetail -> onPoiSelected(event.poi)
                is NavigationEvent.NavigateToRoute -> { /* TODO: 导航到路线规划 */ }
            }
        }
    }
    
    SearchScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack
    )
}

@Composable
private fun SearchScreenContent(
    uiState: SearchUiState,
    onEvent: (SearchEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
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
                autoFocus = !uiState.showResults,  // 只在初始页面自动获取焦点
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // 内容区域
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    // 显示搜索结果
                    uiState.showResults -> {
                        SearchResultsContent(
                            results = uiState.searchResults,
                            isLoading = uiState.isLoading,
                            error = uiState.error,
                            selectedCategory = uiState.selectedCategory,
                            onPoiClick = { onEvent(SearchEvent.SelectPoi(it)) }
                        )
                    }
                    // 显示初始页面（分类 + 历史）
                    else -> {
                        InitialContent(
                            categories = uiState.popularCategories,
                            searchHistory = uiState.searchHistory,
                            onCategoryClick = { category ->
                                focusManager.clearFocus()  // 隐藏键盘
                                onEvent(SearchEvent.SelectCategory(category))
                            },
                            onHistoryClick = { query ->
                                focusManager.clearFocus()  // 隐藏键盘
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
                // 加载中
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = AmapBlue
                )
            }
            error != null -> {
                // 错误状态
                ErrorContent(
                    message = error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            results.isEmpty() -> {
                // 空结果
                EmptyResultContent(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                // 搜索结果列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 结果统计
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
                    
                    // POI 列表
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
                    
                    // 底部间距
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
private fun EmptyResultContent(
    modifier: Modifier = Modifier
) {
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

@Preview(showBackground = true)
@Composable
private fun SearchScreenPreview() {
    AmapSimTheme {
        SearchScreenContent(
            uiState = SearchUiState(
                popularCategories = defaultCategories,
                searchHistory = listOf("武汉大学", "光谷广场", "汉口站")
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchResultsPreview() {
    AmapSimTheme {
        SearchScreenContent(
            uiState = SearchUiState(
                query = "武汉大学",
                showResults = true,
                searchResults = listOf(
                    PoiResult(
                        id = 1,
                        name = "武汉大学",
                        category = "university",
                        lat = 30.5355,
                        lon = 114.3645,
                        address = "湖北省武汉市武昌区八一路299号",
                        distance = 1500.0
                    ),
                    PoiResult(
                        id = 2,
                        name = "武汉大学医院",
                        category = "hospital",
                        lat = 30.5389,
                        lon = 114.3672,
                        address = "武汉市武昌区东湖路115号",
                        distance = 2100.0
                    )
                )
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyResultsPreview() {
    AmapSimTheme {
        SearchScreenContent(
            uiState = SearchUiState(
                query = "找不到的地方",
                showResults = true,
                searchResults = emptyList()
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}
