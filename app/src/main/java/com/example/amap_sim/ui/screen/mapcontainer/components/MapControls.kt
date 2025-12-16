package com.example.amap_sim.ui.screen.mapcontainer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.amap_sim.ui.theme.AmapSimTheme

/**
 * 地图控制按钮组
 * 
 * 包含：
 * - 放大/缩小按钮
 * - 定位按钮
 */
@Composable
fun MapControls(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onLocate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .wrapContentSize()
            .padding(end = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 缩放控制
        Surface(
            modifier = Modifier
                .width(44.dp)
                .shadow(4.dp, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            color = Color.White
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(44.dp)
            ) {
                // 放大按钮
                IconButton(
                    onClick = onZoomIn,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "放大",
                        tint = Color(0xFF666666)
                    )
                }
                
                // 分隔线
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = Color(0xFFE0E0E0)
                )
                
                // 缩小按钮
                IconButton(
                    onClick = onZoomOut,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "缩小",
                        tint = Color(0xFF666666)
                    )
                }
            }
        }
        
        // 定位按钮
        Surface(
            modifier = Modifier
                .size(44.dp)
                .shadow(4.dp, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            color = Color.White
        ) {
            IconButton(
                onClick = onLocate,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = "定位",
                    tint = Color(0xFF666666)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE0E0E0)
@Composable
private fun MapControlsPreview() {
    AmapSimTheme {
        MapControls(
            onZoomIn = {},
            onZoomOut = {},
            onLocate = {}
        )
    }
}

