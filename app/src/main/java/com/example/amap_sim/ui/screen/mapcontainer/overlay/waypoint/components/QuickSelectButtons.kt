package com.example.amap_sim.ui.screen.mapcontainer.overlay.waypoint.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * 快捷选择按钮组
 */
@Composable
fun QuickSelectButtons(
    onMyLocationClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onMapSelectClick: () -> Unit,
    onHomeClick: () -> Unit,
    onCompanyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
    ) {
        QuickSelectButton(
            icon = Icons.Default.MyLocation,
            label = "我的位置",
            onClick = onMyLocationClick
        )
        QuickSelectButton(
            icon = Icons.Default.Favorite,
            label = "收藏的点",
            onClick = onFavoritesClick
        )
        QuickSelectButton(
            icon = Icons.Default.LocationOn,
            label = "地图选点",
            onClick = onMapSelectClick
        )
        QuickSelectButton(
            icon = Icons.Default.Home,
            label = "家",
            onClick = onHomeClick
        )
        QuickSelectButton(
            icon = Icons.Default.Business,
            label = "公司",
            onClick = onCompanyClick
        )
    }
}

/**
 * 单个快捷选择按钮
 */
@Composable
private fun QuickSelectButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

