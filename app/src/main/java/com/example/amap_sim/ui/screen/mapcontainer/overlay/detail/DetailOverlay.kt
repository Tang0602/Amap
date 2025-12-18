package com.example.amap_sim.ui.screen.mapcontainer.overlay.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amap_sim.domain.model.PoiResult
import com.example.amap_sim.ui.screen.mapcontainer.MapStateController
import com.example.amap_sim.ui.theme.AmapBlue
import com.example.amap_sim.ui.theme.Gray400
import com.example.amap_sim.ui.theme.Gray500

/**
 * POI 详情 Overlay
 * 
 * 在地图上叠加 POI 详情卡片，地图自动定位到 POI 位置
 */
@Composable
fun DetailOverlay(
    poiId: String,
    mapController: MapStateController,
    onNavigateBack: () -> Unit,
    onNavigateToRoute: (Double, Double, String) -> Unit,
    viewModel: DetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // 当 poiId 变化时，加载新的 POI 详情
    LaunchedEffect(poiId) {
        viewModel.loadPoiById(poiId)
    }
    
    // 监听地图更新状态，应用 ViewModel 计算的地图操作
    // UI 层只负责执行，不包含业务逻辑
    LaunchedEffect(uiState.mapUpdate) {
        when (val update = uiState.mapUpdate) {
            null -> { /* 无更新 */ }
            is DetailMapUpdate.Clear -> {
                mapController.clearMarkers()
            }
            is DetailMapUpdate.ShowPoi -> {
                mapController.setMarkers(listOf(update.marker))
                mapController.moveTo(update.position, update.zoomLevel)
            }
        }
    }
    
    // 监听导航事件
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                DetailNavigationEvent.Back -> {
                    mapController.clearMarkers()
                    onNavigateBack()
                }
                is DetailNavigationEvent.NavigateToRoute -> {
                    onNavigateToRoute(event.destLat, event.destLon, event.destName)
                }
                is DetailNavigationEvent.MakePhoneCall -> {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:${event.phone}")
                    }
                    context.startActivity(intent)
                }
                is DetailNavigationEvent.SharePoi -> {
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
    
    DetailOverlayContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = {
            mapController.clearMarkers()
            onNavigateBack()
        }
    )
}

@Composable
private fun DetailOverlayContent(
    uiState: DetailUiState,
    onEvent: (DetailEvent) -> Unit,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 顶部返回按钮
        TopBar(
            onBack = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
        )
        
        // 底部详情卡片
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = AmapBlue
                )
            }
            uiState.error != null -> {
                ErrorContent(
                    message = uiState.error,
                    onBack = onBack,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            uiState.poi != null -> {
                PoiDetailCard(
                    poi = uiState.poi,
                    isFavorite = uiState.isFavorite,
                    onEvent = onEvent,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
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
 * POI 详情卡片
 */
@Composable
private fun PoiDetailCard(
    poi: PoiResult,
    isFavorite: Boolean,
    onEvent: (DetailEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 名称和分类
            Text(
                text = poi.name,
                style = MaterialTheme.typography.titleLarge,
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 地址信息
            poi.address?.takeIf { it.isNotBlank() }?.let { address ->
                InfoRow(
                    icon = Icons.Default.LocationOn,
                    value = address,
                    actionIcon = Icons.Default.ContentCopy,
                    onAction = { clipboardManager.setText(AnnotatedString(address)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // 电话
            poi.phone?.takeIf { it.isNotBlank() }?.let {
                InfoRow(
                    icon = Icons.Default.Call,
                    value = it,
                    actionIcon = Icons.Default.Call,
                    actionTint = AmapBlue,
                    onAction = { onEvent(DetailEvent.CallPhone) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 操作按钮
            ActionButtonsRow(
                poi = poi,
                isFavorite = isFavorite,
                onNavigateTo = { onEvent(DetailEvent.NavigateTo) },
                onToggleFavorite = { onEvent(DetailEvent.ToggleFavorite) },
                onShare = { onEvent(DetailEvent.Share) }
            )
        }
    }
}

/**
 * 信息行
 */
@Composable
private fun InfoRow(
    icon: ImageVector,
    value: String,
    actionIcon: ImageVector? = null,
    actionTint: Color = Gray400,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Gray400,
            modifier = Modifier.size(18.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        
        if (actionIcon != null && onAction != null) {
            IconButton(
                onClick = onAction,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = null,
                    tint = actionTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
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
        // 到这去按钮
        Button(
            onClick = onNavigateTo,
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AmapBlue),
            shape = RoundedCornerShape(22.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "到这去",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        
        // 收藏按钮
        OutlinedButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "取消收藏" else "收藏",
                tint = if (isFavorite) Color.Red else Gray500,
                modifier = Modifier.size(20.dp)
            )
        }
        
        // 分享按钮
        OutlinedButton(
            onClick = onShare,
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "分享",
                tint = Gray500,
                modifier = Modifier.size(20.dp)
            )
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
    Card(
        modifier = modifier.padding(32.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Gray400
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "加载失败",
                style = MaterialTheme.typography.bodyLarge,
                color = Gray500
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = Gray400
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onBack) {
                Text("返回")
            }
        }
    }
}

