package com.example.amap_sim.ui.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amap_sim.data.local.AppTheme
import com.example.amap_sim.domain.model.RouteHistory
import com.example.amap_sim.ui.theme.AmapBlue
import java.text.SimpleDateFormat
import java.util.*

/**
 * 个人资料页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFavorites: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 用户信息区域
            item {
                UserInfoSection(
                    userProfile = uiState.userProfile,
                    onEditAvatar = { viewModel.onEvent(ProfileEvent.ShowEditDialog(EditType.AVATAR)) },
                    onEditName = { viewModel.onEvent(ProfileEvent.ShowEditDialog(EditType.NAME)) },
                    onEditUserId = { viewModel.onEvent(ProfileEvent.ShowEditDialog(EditType.USER_ID)) }
                )
            }

            item { Divider() }

            // 主题设置
            item {
                ListItem(
                    headlineContent = { Text("主题") },
                    leadingContent = {
                        Icon(Icons.Default.Palette, null, tint = AmapBlue)
                    }
                )
            }

            // 主题选项
            item {
                ThemeSelector(
                    currentTheme = uiState.currentTheme,
                    onThemeSelected = { theme ->
                        viewModel.onEvent(ProfileEvent.ChangeTheme(theme))
                    }
                )
            }

            item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }

            // 收藏夹
            item {
                ListItem(
                    headlineContent = { Text("收藏夹") },
                    leadingContent = {
                        Icon(Icons.Default.Favorite, null, tint = AmapBlue)
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${uiState.favorites.size}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(Icons.Default.KeyboardArrowRight, null)
                        }
                    },
                    modifier = Modifier.clickable { onNavigateToFavorites() }
                )
            }

            item { Divider() }

            // 历史路线标题
            item {
                ListItem(
                    headlineContent = { Text("历史路线") },
                    leadingContent = {
                        Icon(Icons.Default.History, null, tint = AmapBlue)
                    }
                )
            }

            // 历史路线列表
            if (uiState.routeHistory.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "暂无历史路线",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(uiState.routeHistory) { history ->
                    RouteHistoryItem(
                        history = history,
                        onDelete = { viewModel.onEvent(ProfileEvent.DeleteRouteHistory(history.id)) }
                    )
                }
            }
        }

        // 编辑对话框
        if (uiState.showEditDialog) {
            EditDialog(
                editType = uiState.editType!!,
                currentValue = when (uiState.editType) {
                    EditType.AVATAR -> uiState.userProfile.avatarPath
                    EditType.NAME -> uiState.userProfile.userName
                    EditType.USER_ID -> uiState.userProfile.userId
                    null -> ""
                },
                onDismiss = { viewModel.onEvent(ProfileEvent.HideEditDialog) },
                onConfirm = { value ->
                    when (uiState.editType) {
                        EditType.AVATAR -> viewModel.onEvent(ProfileEvent.UpdateAvatar(value))
                        EditType.NAME -> viewModel.onEvent(ProfileEvent.UpdateName(value))
                        EditType.USER_ID -> viewModel.onEvent(ProfileEvent.UpdateUserId(value))
                        null -> {}
                    }
                }
            )
        }

        // 错误提示
        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.onEvent(ProfileEvent.ClearError) }) {
                        Text("关闭")
                    }
                }
            ) {
                Text(error)
            }
        }
    }
}

/**
 * 用户信息区域
 */
@Composable
private fun UserInfoSection(
    userProfile: com.example.amap_sim.domain.model.UserProfile,
    onEditAvatar: () -> Unit,
    onEditName: () -> Unit,
    onEditUserId: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(AmapBlue.copy(alpha = 0.1f))
                    .clickable(onClick = onEditAvatar),
                contentAlignment = Alignment.Center
            ) {
                if (userProfile.avatarPath.isEmpty()) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = AmapBlue
                    )
                } else {
                    Text(
                        userProfile.avatarPath.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = AmapBlue
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 名字和 ID
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onEditName)
                ) {
                    Text(
                        text = userProfile.userName.ifEmpty { "点击设置名字" },
                        style = MaterialTheme.typography.titleLarge,
                        color = if (userProfile.userName.isEmpty())
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onEditUserId)
                ) {
                    Text(
                        text = if (userProfile.userId.isEmpty()) "点击设置 ID" else "ID: ${userProfile.userId}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 历史路线项
 */
@Composable
private fun RouteHistoryItem(
    history: RouteHistory,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }

    ListItem(
        headlineContent = {
            Text(
                "${history.startName} → ${history.endName}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                dateFormat.format(Date(history.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Icon(Icons.Default.Place, null, tint = AmapBlue)
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error)
            }
        },
        modifier = Modifier.clickable { }
    )
    Divider()
}

/**
 * 编辑对话框
 */
@Composable
private fun EditDialog(
    editType: EditType,
    currentValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (editType) {
                    EditType.AVATAR -> "编辑头像"
                    EditType.NAME -> "编辑名字"
                    EditType.USER_ID -> "编辑 ID"
                }
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = {
                    Text(
                        when (editType) {
                            EditType.AVATAR -> "头像文字"
                            EditType.NAME -> "名字"
                            EditType.USER_ID -> "用户 ID"
                        }
                    )
                },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(text)
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 主题选择器
 */
@Composable
private fun ThemeSelector(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 明亮模式
            ThemeOption(
                theme = AppTheme.BRIGHT,
                isSelected = currentTheme == AppTheme.BRIGHT,
                onClick = { onThemeSelected(AppTheme.BRIGHT) },
                modifier = Modifier.weight(1f)
            )

            // 夜间模式
            ThemeOption(
                theme = AppTheme.NIGHT,
                isSelected = currentTheme == AppTheme.NIGHT,
                onClick = { onThemeSelected(AppTheme.NIGHT) },
                modifier = Modifier.weight(1f)
            )

            // 护眼模式
            ThemeOption(
                theme = AppTheme.EYE_CARE,
                isSelected = currentTheme == AppTheme.EYE_CARE,
                onClick = { onThemeSelected(AppTheme.EYE_CARE) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 主题选项
 */
@Composable
private fun ThemeOption(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 主题图标
            Icon(
                imageVector = when (theme) {
                    AppTheme.BRIGHT -> Icons.Default.LightMode
                    AppTheme.NIGHT -> Icons.Default.DarkMode
                    AppTheme.EYE_CARE -> Icons.Default.RemoveRedEye
                },
                contentDescription = theme.displayName,
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 主题名称
            Text(
                text = theme.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 选中指示器
            if (isSelected) {
                Spacer(modifier = Modifier.height(4.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "已选中",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
