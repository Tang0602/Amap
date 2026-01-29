package com.example.amap_sim.ui.screen.profile

import com.example.amap_sim.data.local.AppTheme
import com.example.amap_sim.domain.model.RouteHistory
import com.example.amap_sim.domain.model.UserProfile

/**
 * 个人资料页面 UI 状态
 */
data class ProfileUiState(
    /** 用户资料 */
    val userProfile: UserProfile = UserProfile(),
    /** 历史路线 */
    val routeHistory: List<RouteHistory> = emptyList(),
    /** 收藏夹 POI ID 列表 */
    val favorites: List<String> = emptyList(),
    /** 当前主题 */
    val currentTheme: AppTheme = AppTheme.BRIGHT,
    /** 是否正在加载 */
    val isLoading: Boolean = false,
    /** 错误信息 */
    val error: String? = null,
    /** 是否显示编辑对话框 */
    val showEditDialog: Boolean = false,
    /** 编辑类型 */
    val editType: EditType? = null
)

/**
 * 编辑类型
 */
enum class EditType {
    AVATAR,
    NAME,
    USER_ID
}

/**
 * 个人资料事件
 */
sealed class ProfileEvent {
    /** 加载数据 */
    data object LoadData : ProfileEvent()
    /** 显示编辑对话框 */
    data class ShowEditDialog(val type: EditType) : ProfileEvent()
    /** 隐藏编辑对话框 */
    data object HideEditDialog : ProfileEvent()
    /** 更新头像 */
    data class UpdateAvatar(val path: String) : ProfileEvent()
    /** 更新名字 */
    data class UpdateName(val name: String) : ProfileEvent()
    /** 更新用户 ID */
    data class UpdateUserId(val userId: String) : ProfileEvent()
    /** 删除历史路线 */
    data class DeleteRouteHistory(val id: String) : ProfileEvent()
    /** 切换主题 */
    data class ChangeTheme(val theme: AppTheme) : ProfileEvent()
    /** 清除错误 */
    data object ClearError : ProfileEvent()
}
