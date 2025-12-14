package com.example.amap_sim.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.amap_sim.ui.theme.AmapSimTheme

/**
 * 空状态组件
 * 
 * 用于显示列表为空、搜索无结果等状态
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        if (message != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onAction) {
                Text(text = actionText)
            }
        }
    }
}

/**
 * 搜索无结果状态
 */
@Composable
fun SearchEmptyState(
    query: String,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Default.SearchOff,
        title = "未找到相关结果",
        message = "没有找到与 \"$query\" 相关的地点\n请尝试其他关键词",
        modifier = modifier
    )
}

/**
 * 错误状态组件
 */
@Composable
fun ErrorState(
    title: String = "出错了",
    message: String? = null,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Default.Warning,
        title = title,
        message = message,
        actionText = if (onRetry != null) "重试" else null,
        onAction = onRetry,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    AmapSimTheme {
        EmptyState(
            icon = Icons.Default.SearchOff,
            title = "没有收藏",
            message = "您还没有收藏任何地点",
            actionText = "去逛逛",
            onAction = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchEmptyStatePreview() {
    AmapSimTheme {
        SearchEmptyState(query = "不存在的地方")
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorStatePreview() {
    AmapSimTheme {
        ErrorState(
            title = "加载失败",
            message = "网络连接错误，请检查网络设置",
            onRetry = {}
        )
    }
}
