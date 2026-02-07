package com.example.amap_sim.ui.screen.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amap_sim.data.local.AgentDataManager
import com.example.amap_sim.data.local.UserDataManager
import com.example.amap_sim.di.ServiceLocator
import com.example.amap_sim.domain.model.RouteHistory
import com.example.amap_sim.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 个人资料 ViewModel
 */
class ProfileViewModel : ViewModel() {

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    private val userDataManager: UserDataManager = ServiceLocator.userDataManager
    private val agentDataManager: AgentDataManager = ServiceLocator.agentDataManager

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    /**
     * 处理事件
     */
    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.LoadData -> loadData()
            is ProfileEvent.ShowEditDialog -> showEditDialog(event.type)
            is ProfileEvent.HideEditDialog -> hideEditDialog()
            is ProfileEvent.UpdateAvatar -> updateAvatar(event.path)
            is ProfileEvent.UpdateName -> updateName(event.name)
            is ProfileEvent.UpdateUserId -> updateUserId(event.userId)
            is ProfileEvent.DeleteRouteHistory -> deleteRouteHistory(event.id)
            is ProfileEvent.ChangeTheme -> changeTheme(event.theme)
            is ProfileEvent.ClearError -> clearError()
        }
    }

    /**
     * 加载数据
     */
    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val profile = userDataManager.getUserProfile()
                val history = userDataManager.getRouteHistory()
                val favorites = userDataManager.getFavorites()
                val theme = userDataManager.getTheme()

                _uiState.update {
                    it.copy(
                        userProfile = profile,
                        routeHistory = history,
                        favorites = favorites,
                        currentTheme = theme,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载数据失败", e)
                _uiState.update {
                    it.copy(
                        error = "加载失败: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * 显示编辑对话框
     */
    private fun showEditDialog(type: EditType) {
        _uiState.update {
            it.copy(
                showEditDialog = true,
                editType = type
            )
        }
    }

    /**
     * 隐藏编辑对话框
     */
    private fun hideEditDialog() {
        _uiState.update {
            it.copy(
                showEditDialog = false,
                editType = null
            )
        }
    }

    /**
     * 更新头像
     */
    private fun updateAvatar(path: String) {
        viewModelScope.launch {
            try {
                val updatedProfile = _uiState.value.userProfile.copy(avatarPath = path)
                userDataManager.updateUserProfile(updatedProfile)
                _uiState.update {
                    it.copy(
                        userProfile = updatedProfile,
                        showEditDialog = false,
                        editType = null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "更新头像失败", e)
                _uiState.update { it.copy(error = "更新失败: ${e.message}") }
            }
        }
    }

    /**
     * 更新名字
     *
     * 同时更新：
     * 1. UserDataManager 中的用户资料
     * 2. AgentDataManager 的文件6（指令6：修改我的名字为123456）
     */
    private fun updateName(name: String) {
        viewModelScope.launch {
            try {
                // 更新用户资料
                val updatedProfile = _uiState.value.userProfile.copy(userName = name)
                userDataManager.updateUserProfile(updatedProfile)

                // 更新 Agent 数据文件6（指令6检测用）
                agentDataManager.updateFile6(name, true)
                Log.d(TAG, "已更新 Agent 文件6: userName=$name, modified=true")

                _uiState.update {
                    it.copy(
                        userProfile = updatedProfile,
                        showEditDialog = false,
                        editType = null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "更新名字失败", e)
                _uiState.update { it.copy(error = "更新失败: ${e.message}") }
            }
        }
    }

    /**
     * 更新用户 ID
     *
     * 同时更新：
     * 1. UserDataManager 中的用户资料
     * 2. AgentDataManager 的文件3（指令3：告诉我账号的名字和id）
     */
    private fun updateUserId(userId: String) {
        viewModelScope.launch {
            try {
                val updatedProfile = _uiState.value.userProfile.copy(userId = userId)
                userDataManager.updateUserProfile(updatedProfile)

                // 更新 Agent 数据文件3（指令3检测用）
                agentDataManager.updateFile3(userId, updatedProfile.userName)
                Log.d(TAG, "已更新 Agent 文件3: userId=$userId, userName=${updatedProfile.userName}")

                _uiState.update {
                    it.copy(
                        userProfile = updatedProfile,
                        showEditDialog = false,
                        editType = null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "更新用户 ID 失败", e)
                _uiState.update { it.copy(error = "更新失败: ${e.message}") }
            }
        }
    }

    /**
     * 删除历史路线
     *
     * 同时更新：
     * 1. UserDataManager 中删除历史路线
     * 2. AgentDataManager 的文件9（指令9：删除导航到M+购物中心的历史记录）
     */
    private fun deleteRouteHistory(id: String) {
        viewModelScope.launch {
            try {
                // 找到要删除的路线
                val routeToDelete = _uiState.value.routeHistory.find { it.id == id }

                userDataManager.deleteRouteHistory(id)
                val updatedHistory = _uiState.value.routeHistory.filter { it.id != id }

                // 只有当删除的是目的地为"M+购物中心"的历史记录时，才更新 Agent 数据文件9
                if (routeToDelete != null && routeToDelete.endName == "M+购物中心") {
                    agentDataManager.updateFile9(
                        deleted = true,
                        destinationName = routeToDelete.endName,
                        routeId = id,
                        timestamp = routeToDelete.timestamp
                    )
                    Log.d(TAG, "已更新 Agent 文件9（删除了到M+购物中心的记录）: deleted=true, destinationName=${routeToDelete.endName}, routeId=$id, timestamp=${routeToDelete.timestamp}")
                } else {
                    Log.d(TAG, "删除的不是到M+购物中心的记录，不更新 Agent 文件9: routeId=$id, endName=${routeToDelete?.endName}")
                }

                _uiState.update { it.copy(routeHistory = updatedHistory) }
            } catch (e: Exception) {
                Log.e(TAG, "删除历史路线失败", e)
                _uiState.update { it.copy(error = "删除失败: ${e.message}") }
            }
        }
    }

    /**
     * 清除错误
     */
    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * 切换主题
     *
     * 同时更新：
     * 1. UserDataManager 中保存主题设置
     * 2. AgentDataManager 的文件8（指令8：打开明亮模式/夜间模式）
     */
    private fun changeTheme(theme: com.example.amap_sim.data.local.AppTheme) {
        viewModelScope.launch {
            try {
                userDataManager.saveTheme(theme)

                // 更新 Agent 数据文件8（指令8检测用）
                // 映射主题到模式描述
                val modeDescription = when (theme) {
                    com.example.amap_sim.data.local.AppTheme.BRIGHT -> "明亮模式"
                    com.example.amap_sim.data.local.AppTheme.NIGHT -> "夜间模式"
                    com.example.amap_sim.data.local.AppTheme.EYE_CARE -> "护眼模式"
                }
                agentDataManager.updateFile8(modeDescription, true)
                Log.d(TAG, "已更新 Agent 文件8: mode=$modeDescription, opened=true")

                _uiState.update { it.copy(currentTheme = theme) }
            } catch (e: Exception) {
                Log.e(TAG, "切换主题失败", e)
                _uiState.update { it.copy(error = "切换主题失败: ${e.message}") }
            }
        }
    }
}
