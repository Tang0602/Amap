package com.example.amap_sim.ui.screen.mapcontainer.overlay.waypoint.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.amap_sim.ui.screen.mapcontainer.overlay.route.LocationInput
import com.example.amap_sim.ui.theme.AmapGreen
import com.example.amap_sim.ui.theme.MapMarkerEnd

/**
 * 途径点输入字段组件
 *
 * @param index 字段索引（-1=起点, -2=终点, >=0=途径点索引）
 * @param location 位置输入
 * @param isEditing 是否正在编辑
 * @param searchKeyword 搜索关键词（编辑时显示）
 * @param onFieldClick 点击字段
 * @param onDeleteClick 删除按钮点击（仅途径点）
 * @param onSearchKeywordChange 搜索关键词变化
 */
@Composable
fun WaypointInputField(
    index: Int,
    location: LocationInput?,
    isEditing: Boolean,
    searchKeyword: String,
    onFieldClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    onSearchKeywordChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧容器：圆点 + 输入框 + drag handle
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧编号/图标（增大点击区域）
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onFieldClick),
                    contentAlignment = Alignment.Center
                ) {
                    when (index) {
                        -1 -> {
                            // 起点：绿色圆点
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(AmapGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = Color.White
                                )
                            }
                        }
                        -2 -> {
                            // 终点：红色圆点
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MapMarkerEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = Color.White
                                )
                            }
                        }
                        else -> {
                            // 途径点：显示编号
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // 输入框（使用 BasicTextField，类似 SearchBarInput）
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (!isEditing) {
                                Modifier.clickable(onClick = onFieldClick)
                            } else {
                                Modifier
                            }
                        )
                ) {
                    val focusRequester = remember { FocusRequester() }
                    val focusManager = LocalFocusManager.current
                    
                    val displayText = if (isEditing) searchKeyword else (location?.getDisplayName() ?: "")
                    val placeholderText = when (index) {
                        -1 -> "我的位置"
                        -2 -> "输入终点"
                        else -> "输入途经点"
                    }
                    
                    BasicTextField(
                        value = displayText,
                        onValueChange = if (isEditing) {
                            onSearchKeywordChange
                        } else {
                            { /* 非编辑模式不允许输入 */ }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isEditing) {
                                    Modifier.focusRequester(focusRequester)
                                } else {
                                    Modifier
                                }
                            ),
                        singleLine = true,
                        enabled = isEditing,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { 
                                /* TODO: 执行搜索 */
                                if (isEditing) {
                                    focusManager.clearFocus()
                                }
                            }
                        ),
                        decorationBox = { innerTextField ->
                            Box {
                                // 占位符（当文本为空时显示）
                                if (displayText.isEmpty()) {
                                    Text(
                                        text = placeholderText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    
                    // 编辑模式时自动获取焦点
                    if (isEditing) {
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }
                    }
                }
            }
        }

        // 右侧 close 图标按钮（始终显示，包括编辑模式）
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = { 
                // 仅途径点执行删除操作
                if (index >= 0) {
                    onDeleteClick?.invoke()
                }
            },
            modifier = Modifier.size(36.dp),
            enabled = index >= 0 && onDeleteClick != null
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = if (index >= 0) "删除" else null,
                tint = if (index >= 0) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                },
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

