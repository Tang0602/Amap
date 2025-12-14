package com.example.amap_sim.ui.screen.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amap_sim.domain.model.PoiResult
import com.example.amap_sim.ui.theme.AmapBlue
import com.example.amap_sim.ui.theme.AmapSimTheme
import com.example.amap_sim.ui.theme.Gray100
import com.example.amap_sim.ui.theme.Gray400
import com.example.amap_sim.ui.theme.Gray500

/**
 * POI 详情页
 */
@Composable
fun PoiDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRoute: (Double, Double) -> Unit,
    viewModel: PoiDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // 监听导航事件
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                PoiDetailNavigationEvent.Back -> onNavigateBack()
                is PoiDetailNavigationEvent.NavigateToRoute -> {
                    onNavigateToRoute(event.destLat, event.destLon)
                }
                is PoiDetailNavigationEvent.MakePhoneCall -> {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:${event.phone}")
                    }
                    context.startActivity(intent)
                }
                is PoiDetailNavigationEvent.SharePoi -> {
                    val shareText = buildString {
                        append(event.name)
                        if (!event.address.isNullOrBlank()) {
                            append("\n地址: ${event.address}")
                        }
                    }
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "分享"))
                }
            }
        }
    }
    
    PoiDetailScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
private fun PoiDetailScreenContent(
    uiState: PoiDetailUiState,
    onEvent: (PoiDetailEvent) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    // 加载中
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = AmapBlue
                    )
                }
                uiState.error != null -> {
                    // 错误状态
                    ErrorContent(
                        message = uiState.error,
                        onBack = { onEvent(PoiDetailEvent.NavigateBack) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.poi != null -> {
                    // POI 详情
                    PoiDetailContent(
                        poi = uiState.poi,
                        isFavorite = uiState.isFavorite,
                        onEvent = onEvent
                    )
                }
            }
            
            // 顶部返回按钮（悬浮在内容上方）
            TopBar(
                onBack = { onEvent(PoiDetailEvent.NavigateBack) },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
            )
        }
    }
}

/**
 * 顶部栏
 */
@Composable
private fun TopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = Color.White.copy(alpha = 0.9f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * POI 详情内容
 */
@Composable
private fun PoiDetailContent(
    poi: PoiResult,
    isFavorite: Boolean,
    onEvent: (PoiDetailEvent) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 地图预览区域
        MapPreviewSection(
            poi = poi,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
        
        // 主内容区域
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 名称和分类
            Text(
                text = poi.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 分类和距离
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = AmapBlue.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = poi.getCategoryDisplayName(),
                        style = MaterialTheme.typography.bodySmall,
                        color = AmapBlue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                
                poi.getFormattedDistance()?.let { distance ->
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "距您 $distance",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 操作按钮
            ActionButtonsRow(
                poi = poi,
                isFavorite = isFavorite,
                onNavigateTo = { onEvent(PoiDetailEvent.NavigateTo) },
                onToggleFavorite = { onEvent(PoiDetailEvent.ToggleFavorite) },
                onShare = { onEvent(PoiDetailEvent.Share) }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 详细信息卡片
            InfoCard(
                poi = poi,
                onCopyAddress = { address ->
                    clipboardManager.setText(AnnotatedString(address))
                },
                onCallPhone = { onEvent(PoiDetailEvent.CallPhone) }
            )
            
            // 底部安全区域
            Spacer(modifier = Modifier.navigationBarsPadding())
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 地图预览区域
 */
@Composable
private fun MapPreviewSection(
    poi: PoiResult,
    modifier: Modifier = Modifier
) {
    // 地图占位（实际项目中可以用 MapsforgeMapView 显示静态地图）
    Box(
        modifier = modifier.background(Color(0xFFE8F0FE))
    )
}

/**
 * 操作按钮行
 */
@Composable
private fun ActionButtonsRow(
    poi: PoiResult,
    isFavorite: Boolean,
    onNavigateTo: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 到这去按钮（主按钮）
        Button(
            onClick = onNavigateTo,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AmapBlue
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "到这去",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
        
        // 收藏按钮
        OutlinedButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "取消收藏" else "收藏",
                tint = if (isFavorite) Color.Red else Gray500,
                modifier = Modifier.size(22.dp)
            )
        }
        
        // 分享按钮
        OutlinedButton(
            onClick = onShare,
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "分享",
                tint = Gray500,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * 详细信息卡片
 */
@Composable
private fun InfoCard(
    poi: PoiResult,
    onCopyAddress: (String) -> Unit,
    onCallPhone: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 地址
            poi.address?.let { address ->
                if (address.isNotBlank()) {
                    InfoRow(
                        icon = Icons.Default.LocationOn,
                        label = "地址",
                        value = address,
                        actionIcon = Icons.Default.ContentCopy,
                        onAction = { onCopyAddress(address) }
                    )
                    
                    if (poi.phone != null) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
            
            // 电话
            poi.phone?.let { phone ->
                if (phone.isNotBlank()) {
                    InfoRow(
                        icon = Icons.Default.Call,
                        label = "电话",
                        value = phone,
                        actionIcon = Icons.Default.Call,
                        actionTint = AmapBlue,
                        onAction = onCallPhone
                    )
                }
            }
            
            // 如果没有地址和电话，显示坐标
            if (poi.address.isNullOrBlank() && poi.phone.isNullOrBlank()) {
                InfoRow(
                    icon = Icons.Default.Map,
                    label = "坐标",
                    value = "${String.format("%.6f", poi.lat)}, ${String.format("%.6f", poi.lon)}",
                    actionIcon = Icons.Default.ContentCopy,
                    onAction = { onCopyAddress("${poi.lat}, ${poi.lon}") }
                )
            }
        }
    }
}

/**
 * 信息行
 */
@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    actionIcon: ImageVector? = null,
    actionTint: Color = Gray400,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Gray400,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Gray500
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        if (actionIcon != null && onAction != null) {
            IconButton(
                onClick = onAction,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = null,
                    tint = actionTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 错误内容
 */
@Composable
private fun ErrorContent(
    message: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Gray400
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "加载失败",
            style = MaterialTheme.typography.bodyLarge,
            color = Gray500
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Gray400
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onBack) {
            Text("返回")
        }
    }
}

// ========== Previews ==========

@Preview(showBackground = true)
@Composable
private fun PoiDetailContentPreview() {
    AmapSimTheme {
        PoiDetailScreenContent(
            uiState = PoiDetailUiState(
                isLoading = false,
                poi = PoiResult(
                    id = 1,
                    name = "武汉大学",
                    category = "教育",
                    lat = 30.5355,
                    lon = 114.3645,
                    address = "湖北省武汉市武昌区八一路299号",
                    phone = "027-68752114",
                    distance = 1500.0
                ),
                isFavorite = false
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PoiDetailLoadingPreview() {
    AmapSimTheme {
        PoiDetailScreenContent(
            uiState = PoiDetailUiState(isLoading = true),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PoiDetailErrorPreview() {
    AmapSimTheme {
        PoiDetailScreenContent(
            uiState = PoiDetailUiState(
                isLoading = false,
                error = "网络连接失败"
            ),
            onEvent = {}
        )
    }
}
