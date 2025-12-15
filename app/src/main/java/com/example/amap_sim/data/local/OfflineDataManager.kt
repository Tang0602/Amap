package com.example.amap_sim.data.local

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 离线数据管理器
 * 
 * 负责：
 * - 从 assets 复制地图数据到内部存储
 * - 数据版本管理
 * - 提供数据文件路径
 * 
 * 数据文件说明：
 * - wuhan.map: Mapsforge 地图文件，用于地图渲染
 * - wuhan-gh/: GraphHopper 路由数据目录
 * - wuhan_poi.db: SQLite FTS5 POI 搜索数据库
 * - theme.xml: 地图渲染主题
 */
class OfflineDataManager(private val context: Context) {
    
    companion object {
        private const val TAG = "OfflineDataManager"
        
        // 数据版本号（当 assets 中的数据更新时，增加此版本号）
        private const val DATA_VERSION = 1
        private const val PREFS_NAME = "offline_data_prefs"
        private const val KEY_DATA_VERSION = "data_version"
        private const val KEY_INIT_COMPLETE = "init_complete"
        
        // Assets 目录
        private const val ASSETS_MAP_DIR = "map"
        
        // 数据文件名
        const val MAP_FILE = "wuhan.map"
        const val POI_DB_FILE = "wuhan_poi.db"
        const val ROUTE_DIR = "wuhan-gh"         // GraphHopper（已弃用）
        const val BROUTER_DIR = "brouter"         // BRouter（推荐）
        const val THEME_FILE = "theme.xml"
        
        @Volatile
        private var instance: OfflineDataManager? = null
        
        fun getInstance(context: Context): OfflineDataManager {
            return instance ?: synchronized(this) {
                instance ?: OfflineDataManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
    
    // 内部存储的地图数据目录
    private val mapDataDir: File by lazy {
        File(context.filesDir, "map_data").also { 
            if (!it.exists()) it.mkdirs() 
        }
    }
    
    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 初始化状态
     */
    sealed class InitState {
        object NotStarted : InitState()
        data class InProgress(val progress: Int, val message: String) : InitState()
        object Completed : InitState()
        data class Error(val message: String, val cause: Throwable?) : InitState()
    }
    
    /**
     * 检查是否需要初始化数据
     */
    fun needsInitialization(): Boolean {
        val savedVersion = prefs.getInt(KEY_DATA_VERSION, 0)
        val initComplete = prefs.getBoolean(KEY_INIT_COMPLETE, false)
        return !initComplete || savedVersion < DATA_VERSION
    }
    
    /**
     * 初始化离线数据
     * 将 assets 中的数据复制到内部存储
     * 
     * @param onProgress 进度回调 (progress: 0-100, message: String)
     */
    suspend fun initializeData(
        onProgress: ((Int, String) -> Unit)? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "开始初始化离线数据...")
            onProgress?.invoke(0, "正在准备...")
            
            // 检查是否需要更新
            val savedVersion = prefs.getInt(KEY_DATA_VERSION, 0)
            if (savedVersion >= DATA_VERSION && prefs.getBoolean(KEY_INIT_COMPLETE, false)) {
                Log.i(TAG, "数据已是最新版本 ($DATA_VERSION)")
                onProgress?.invoke(100, "数据已准备就绪")
                return@withContext Result.success(Unit)
            }
            
            // 如果版本更新，清理旧数据
            if (savedVersion < DATA_VERSION && mapDataDir.exists()) {
                Log.i(TAG, "清理旧版本数据...")
                onProgress?.invoke(5, "清理旧版本数据...")
                mapDataDir.deleteRecursively()
                mapDataDir.mkdirs()
            }
            
            // 复制地图文件 (20%)
            onProgress?.invoke(10, "正在复制地图数据...")
            copyAssetFile("$ASSETS_MAP_DIR/$MAP_FILE", File(mapDataDir, MAP_FILE))
            Log.i(TAG, "地图文件复制完成")
            
            // 复制 POI 数据库 (40%)
            onProgress?.invoke(30, "正在复制 POI 数据...")
            copyAssetFile("$ASSETS_MAP_DIR/$POI_DB_FILE", File(mapDataDir, POI_DB_FILE))
            Log.i(TAG, "POI 数据库复制完成")
            
            // 复制主题文件 (45%)
            onProgress?.invoke(40, "正在复制主题文件...")
            copyAssetFile("$ASSETS_MAP_DIR/$THEME_FILE", File(mapDataDir, THEME_FILE))
            Log.i(TAG, "主题文件复制完成")
            
            // 复制路由数据目录 (50%-90%)
            // 优先使用 BRouter，如果不存在则使用 GraphHopper
            onProgress?.invoke(50, "正在复制路由数据...")
            val hasBRouter = try {
                context.assets.list("$ASSETS_MAP_DIR/$BROUTER_DIR")?.isNotEmpty() == true
            } catch (e: Exception) {
                false
            }
            
            if (hasBRouter) {
                Log.i(TAG, "使用 BRouter 路由数据")
                copyAssetDirectory("$ASSETS_MAP_DIR/$BROUTER_DIR", File(mapDataDir, BROUTER_DIR)) { progress ->
                    onProgress?.invoke(50 + (progress * 0.4).toInt(), "正在复制 BRouter 路由数据...")
                }
            } else {
                Log.i(TAG, "使用 GraphHopper 路由数据")
                copyAssetDirectory("$ASSETS_MAP_DIR/$ROUTE_DIR", File(mapDataDir, ROUTE_DIR)) { progress ->
                    onProgress?.invoke(50 + (progress * 0.4).toInt(), "正在复制 GraphHopper 路由数据...")
                }
            }
            Log.i(TAG, "路由数据复制完成")
            
            // 验证数据完整性
            onProgress?.invoke(95, "正在验证数据...")
            validateData()
            
            // 保存版本信息
            prefs.edit()
                .putInt(KEY_DATA_VERSION, DATA_VERSION)
                .putBoolean(KEY_INIT_COMPLETE, true)
                .apply()
            
            onProgress?.invoke(100, "初始化完成")
            Log.i(TAG, "离线数据初始化完成")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "初始化失败", e)
            // 标记初始化未完成
            prefs.edit().putBoolean(KEY_INIT_COMPLETE, false).apply()
            Result.failure(e)
        }
    }
    
    /**
     * 从 assets 复制单个文件
     */
    private fun copyAssetFile(assetPath: String, destFile: File) {
        destFile.parentFile?.mkdirs()
        
        context.assets.open(assetPath).use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
    }
    
    /**
     * 从 assets 复制整个目录
     */
    private fun copyAssetDirectory(
        assetPath: String, 
        destDir: File,
        onProgress: ((Int) -> Unit)? = null
    ) {
        destDir.mkdirs()
        
        val files = context.assets.list(assetPath) ?: return
        val totalFiles = files.size
        
        files.forEachIndexed { index, fileName ->
            val assetFilePath = "$assetPath/$fileName"
            val destFile = File(destDir, fileName)
            
            // 尝试打开为文件，如果失败则为目录
            try {
                context.assets.open(assetFilePath).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: IOException) {
                // 可能是目录，递归复制
                copyAssetDirectory(assetFilePath, destFile, null)
            }
            
            onProgress?.invoke(((index + 1) * 100) / totalFiles)
        }
    }
    
    /**
     * 验证数据完整性
     */
    private fun validateData() {
        val mapFile = getMapFile()
        val poiDbFile = getPoiDbFile()
        val themeFile = getThemeFile()
        
        require(mapFile.exists() && mapFile.length() > 0) {
            "地图文件无效: ${mapFile.absolutePath}"
        }
        
        require(poiDbFile.exists() && poiDbFile.length() > 0) {
            "POI 数据库无效: ${poiDbFile.absolutePath}"
        }
        
        // 验证路由数据（BRouter 或 GraphHopper）
        val brouterDir = getBRouterDirectory()
        val graphHopperDir = getRouteDirectory()
        
        if (brouterDir.exists() && brouterDir.isDirectory) {
            // 验证 BRouter 数据
            val segmentsDir = File(brouterDir, "segments")
            val profilesDir = File(brouterDir, "profiles")
            
            require(segmentsDir.exists() && segmentsDir.isDirectory) {
                "BRouter segments 目录无效"
            }
            
            val rd5Files = segmentsDir.listFiles { f -> f.extension == "rd5" }
            require(!rd5Files.isNullOrEmpty()) {
                "BRouter segments 目录中没有 rd5 文件"
            }
            
            require(profilesDir.exists() && profilesDir.isDirectory) {
                "BRouter profiles 目录无效"
            }
            
            Log.i(TAG, "BRouter 数据验证通过: ${rd5Files.size} 个分片")
        } else if (graphHopperDir.exists() && graphHopperDir.isDirectory) {
            // 验证 GraphHopper 数据
            val requiredRouteFiles = listOf("nodes", "edges", "geometry", "properties")
            for (fileName in requiredRouteFiles) {
                val file = File(graphHopperDir, fileName)
                require(file.exists()) {
                    "GraphHopper 路由数据文件缺失: $fileName"
                }
            }
            Log.i(TAG, "GraphHopper 数据验证通过")
        } else {
            throw IllegalStateException("未找到路由数据（BRouter 或 GraphHopper）")
        }
        
        require(themeFile.exists()) {
            "主题文件无效: ${themeFile.absolutePath}"
        }
        
        Log.i(TAG, "数据验证通过")
    }
    
    // ============== 数据文件路径访问 ==============
    
    /**
     * 获取 Mapsforge 地图文件
     */
    fun getMapFile(): File = File(mapDataDir, MAP_FILE)
    
    /**
     * 获取 POI 数据库文件
     */
    fun getPoiDbFile(): File = File(mapDataDir, POI_DB_FILE)
    
    /**
     * 获取 GraphHopper 路由数据目录（已弃用）
     * @deprecated 请使用 getBRouterDirectory()
     */
    @Deprecated("请使用 getBRouterDirectory()", ReplaceWith("getBRouterDirectory()"))
    fun getRouteDirectory(): File = File(mapDataDir, ROUTE_DIR)
    
    /**
     * 获取 BRouter 数据目录
     */
    fun getBRouterDirectory(): File = File(mapDataDir, BROUTER_DIR)
    
    /**
     * 获取 BRouter segments 目录
     */
    fun getBRouterSegmentsDirectory(): File = File(getBRouterDirectory(), "segments")
    
    /**
     * 获取 BRouter profiles 目录
     */
    fun getBRouterProfilesDirectory(): File = File(getBRouterDirectory(), "profiles")
    
    /**
     * 检查是否使用 BRouter
     */
    fun isBRouterAvailable(): Boolean {
        val brouterDir = getBRouterDirectory()
        val segmentsDir = File(brouterDir, "segments")
        return segmentsDir.exists() && 
               segmentsDir.listFiles { f -> f.extension == "rd5" }?.isNotEmpty() == true
    }
    
    /**
     * 获取地图主题文件
     */
    fun getThemeFile(): File = File(mapDataDir, THEME_FILE)
    
    /**
     * 获取数据目录
     */
    fun getDataDirectory(): File = mapDataDir
    
    /**
     * 检查数据是否已初始化
     */
    fun isInitialized(): Boolean {
        if (!prefs.getBoolean(KEY_INIT_COMPLETE, false)) return false
        if (!getMapFile().exists()) return false
        if (!getPoiDbFile().exists()) return false
        
        // 检查路由数据（BRouter 或 GraphHopper）
        @Suppress("DEPRECATION")
        return isBRouterAvailable() || getRouteDirectory().exists()
    }
    
    /**
     * 获取当前数据版本
     */
    fun getDataVersion(): Int = DATA_VERSION
    
    /**
     * 获取数据总大小（MB）
     */
    fun getDataSizeMB(): Double {
        return if (mapDataDir.exists()) {
            mapDataDir.walkTopDown()
                .filter { it.isFile }
                .sumOf { it.length() }
                .toDouble() / (1024 * 1024)
        } else {
            0.0
        }
    }
    
    /**
     * 清除所有数据（用于调试或重置）
     */
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        Log.w(TAG, "清除所有离线数据")
        mapDataDir.deleteRecursively()
        prefs.edit()
            .remove(KEY_DATA_VERSION)
            .remove(KEY_INIT_COMPLETE)
            .apply()
    }
}
