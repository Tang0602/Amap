package com.example.amap_sim.ui.screen.mapcontainer.overlay.search.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.amap_sim.domain.model.PoiResult
import com.example.amap_sim.ui.theme.AmapSimTheme
import com.example.amap_sim.ui.theme.AmapBlue
import com.example.amap_sim.ui.theme.Gray400
import com.example.amap_sim.ui.theme.Gray500

/**
 * POI 列表项
 * 
 * 仿高德地图搜索结果样式
 */
@Composable
fun PoiListItem(
    poi: PoiResult,
    onClick: () -> Unit,
    onNavigateClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 位置图标
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = AmapBlue,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // POI 信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 名称
                Text(
                    text = poi.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 分类和距离
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 分类
                    Text(
                        text = poi.getCategoryDisplayName(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500
                    )
                    
                    // 距离
                    poi.getFormattedDistance()?.let { distance ->
                        Text(
                            text = "| $distance",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray400
                        )
                    }
                }
                
                // 地址
                poi.address?.let { address ->
                    if (address.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = address,
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray400,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 导航按钮或箭头
            if (onNavigateClick != null) {
                IconButton(
                    onClick = onNavigateClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "导航",
                        tint = AmapBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Gray400,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // 分割线
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 52.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * POI 卡片样式
 * 
 * 用于热门推荐等场景
 */
@Composable
fun PoiCard(
    poi: PoiResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 名称
            Text(
                text = poi.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 分类
            Text(
                text = poi.getCategoryDisplayName(),
                style = MaterialTheme.typography.bodySmall,
                color = Gray500
            )
            
            // 距离
            poi.getFormattedDistance()?.let { distance ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = distance,
                    style = MaterialTheme.typography.bodySmall,
                    color = AmapBlue
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PoiListItemPreview() {
    AmapSimTheme {
        Surface {
            Column {
                PoiListItem(
                    poi = PoiResult(
                        id = 1,
                        name = "武汉大学",
                        category = "university",
                        lat = 30.5355,
                        lon = 114.3645,
                        address = "湖北省武汉市武昌区八一路299号",
                        distance = 1500.0
                    ),
                    onClick = {},
                    onNavigateClick = {}
                )
                
                PoiListItem(
                    poi = PoiResult(
                        id = 2,
                        name = "光谷广场",
                        category = "subway_station",
                        lat = 30.5089,
                        lon = 114.4001,
                        address = "武汉市洪山区珞瑜路",
                        distance = 3200.0
                    ),
                    onClick = {},
                    showDivider = false
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PoiCardPreview() {
    AmapSimTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            PoiCard(
                poi = PoiResult(
                    id = 1,
                    name = "星巴克咖啡",
                    category = "cafe",
                    lat = 30.5355,
                    lon = 114.3645,
                    distance = 500.0
                ),
                onClick = {},
                modifier = Modifier.width(120.dp)
            )
        }
    }
}

