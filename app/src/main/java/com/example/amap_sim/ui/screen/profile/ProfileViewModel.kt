package com.example.amap_sim.ui.screen.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amap_sim.data.local.UserDataManager
import com.example.amap_sim.di.ServiceLocator
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
     */
    private fun updateName(name: String) {
        viewModelScope.launch {
            try {
                val updatedProfile = _uiState.value.userProfile.copy(userName = name)
                userDataManager.updateUserProfile(updatedProfile)
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
     */
    private fun updateUserId(userId: String) {
        viewModelScope.launch {
            try {
                val updatedProfile = _uiState.value.userProfile.copy(userId = userId)
                userDataManager.updateUserProfile(updatedProfile)
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
     */
    private fun deleteRouteHistory(id: String) {
        viewModelScope.launch {
            try {
                userDataManager.deleteRouteHistory(id)
                val updatedHistory = _uiState.value.routeHistory.filter { it.id != id }
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
     */
    private fun changeTheme(theme: com.example.amap_sim.data.local.AppTheme) {
        viewModelScope.launch {
            try {
                userDataManager.saveTheme(theme)
                _uiState.update { it.copy(currentTheme = theme) }
            } catch (e: Exception) {
                Log.e(TAG, "切换主题失败", e)
                _uiState.update { it.copy(error = "切换主题失败: ${e.message}") }
            }
        }
    }
}
