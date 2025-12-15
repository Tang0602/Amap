package com.example.amap_sim.data.local

import android.util.Log
import com.example.amap_sim.domain.model.InstructionSign
import com.example.amap_sim.domain.model.LatLng
import com.example.amap_sim.domain.model.RouteInstruction
import com.example.amap_sim.domain.model.RouteResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * BRouter 离线路由服务
 * 
 * 专为移动端设计的离线路径规划引擎，替代 GraphHopper
 * 
 * 功能：
 * - 两点间路径规划
 * - 多途经点路径规划
 * - 支持多种交通方式（驾车、骑行、步行）
 * - 提供详细导航指令
 * 
 * 优势：
 * - 内存占用低（针对移动端优化）
 * - 数据格式紧凑（rd5 格式）
 * - 被 OsmAnd、Locus Map 等知名应用采用
 * - 高度可定制（通过 profile 脚本）
 * 
 * 数据结构：
 * ```
 * brouter/
 * ├── segments/     # rd5 数据文件
 * │   ├── E110_N25.rd5
 * │   └── E110_N30.rd5
 * └── profiles/     # 路由配置文件
 *     ├── car-fast.brf
 *     ├── trekking.brf
 *     └── shortest.brf
 * ```
 */
class BRouterService(
    private val dataManager: OfflineDataManager
) {
    companion object {
        private const val TAG = "BRouterService"
        
        // BRouter profile 名称
        const val PROFILE_CAR = "car-fast"
        const val PROFILE_BIKE = "trekking"
        const val PROFILE_FOOT = "shortest"
        
        // 兼容旧接口的 profile 名称
        const val PROFILE_CAR_LEGACY = "car"
        const val PROFILE_BIKE_LEGACY = "bike"
        const val PROFILE_FOOT_LEGACY = "foot"
    }
    
    private var segmentsDir: File? = null
    private var profilesDir: File? = null
    private val initMutex = Mutex()
    private var isInitialized = false
    
    // BRouter 核心引擎（延迟加载）
    // 注意：需要在编译时添加 BRouter JAR 到 libs 目录
    // private var routingEngine: RoutingEngine? = null
    
    /**
     * 初始化路由引擎
     * 
     * 必须在使用前调用，建议在 Application 启动时初始化
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        initMutex.withLock {
            if (isInitialized) {
                Log.d(TAG, "BRouter 已初始化")
                return@withContext Result.success(Unit)
            }
            
            try {
                // 获取 BRouter 数据目录
                val brouterDir = dataManager.getBRouterDirectory()
                require(brouterDir.exists() && brouterDir.isDirectory) {
                    "BRouter 数据目录不存在: ${brouterDir.absolutePath}"
                }
                
                // 验证 segments 目录
                segmentsDir = File(brouterDir, "segments").also { dir ->
                    require(dir.exists() && dir.isDirectory) {
                        "BRouter segments 目录不存在: ${dir.absolutePath}"
                    }
                    
                    val rd5Files = dir.listFiles { f -> f.extension == "rd5" }
                    require(!rd5Files.isNullOrEmpty()) {
                        "BRouter segments 目录中没有 rd5 文件"
                    }
                    
                    Log.i(TAG, "找到 ${rd5Files.size} 个 rd5 数据文件")
                    rd5Files.forEach { file ->
                        Log.d(TAG, "  - ${file.name}: ${file.length() / 1024 / 1024}MB")
                    }
                }
                
                // 验证 profiles 目录
                profilesDir = File(brouterDir, "profiles").also { dir ->
                    require(dir.exists() && dir.isDirectory) {
                        "BRouter profiles 目录不存在: ${dir.absolutePath}"
                    }
                    
                    val brfFiles = dir.listFiles { f -> f.extension == "brf" }
                    require(!brfFiles.isNullOrEmpty()) {
                        "BRouter profiles 目录中没有 brf 文件"
                    }
                    
                    Log.i(TAG, "找到 ${brfFiles.size} 个路由配置文件")
                    brfFiles.forEach { file ->
                        Log.d(TAG, "  - ${file.name}")
                    }
                }
                
                isInitialized = true
                Log.i(TAG, "BRouter 初始化完成")
                
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "BRouter 初始化失败", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * 检查是否已初始化
     */
    fun isReady(): Boolean = isInitialized
    
    /**
     * 计算两点间路径
     * 
     * @param start 起点坐标
     * @param end 终点坐标
     * @param profile 交通方式：car, bike, foot（会自动映射到 BRouter profile）
     * @return 路线结果，失败返回错误
     */
    suspend fun calculateRoute(
        start: LatLng,
        end: LatLng,
        profile: String = PROFILE_CAR
    ): Result<RouteResult> = withContext(Dispatchers.IO) {
        calculateRouteWithWaypoints(listOf(start, end), profile)
    }
    
    /**
     * 计算多途经点路径
     * 
     * @param points 点列表（至少包含起点和终点）
     * @param profile 交通方式
     * @return 路线结果
     */
    suspend fun calculateRouteWithWaypoints(
        points: List<LatLng>,
        profile: String = PROFILE_CAR
    ): Result<RouteResult> = withContext(Dispatchers.IO) {
        try {
            require(isInitialized) { "BRouter 未初始化，请先调用 initialize()" }
            require(points.size >= 2) { "至少需要两个点（起点和终点）" }
            
            val actualProfile = mapProfile(profile)
            Log.d(TAG, "计算路径: ${points.size} 个点, profile: $actualProfile")
            
            val profileFile = File(profilesDir, "$actualProfile.brf")
            require(profileFile.exists()) {
                "找不到路由配置文件: ${profileFile.name}"
            }
            
            // ========================================
            // BRouter 核心路由计算
            // ========================================
            // 注意：以下代码需要在添加 BRouter JAR 后才能编译
            // 目前提供一个模拟实现用于演示 API
            
            /*
            // 创建路由上下文
            val routingContext = RoutingContext().apply {
                localFunction = profileFile.absolutePath
            }
            
            // 创建路由点
            val waypoints = points.map { point ->
                OsmNodeNamed().apply {
                    name = ""
                    ilon = (point.lon * 1_000_000).toInt()
                    ilat = (point.lat * 1_000_000).toInt()
                }
            }
            
            // 创建路由引擎并计算
            val engine = RoutingEngine(
                null,  // 无服务上下文
                null,  // 无轨迹写入器
                segmentsDir!!.absolutePath,
                waypoints,
                routingContext
            )
            
            engine.doRun(0L)
            
            val track = engine.foundTrack
            if (track != null) {
                val result = convertToRouteResult(track, profile)
                Log.i(TAG, "路由计算成功: ${result.getFormattedDistance()}, ${result.getFormattedTime()}")
                return@withContext Result.success(result)
            } else {
                val errorMsg = engine.errorMessage ?: "路由计算失败：未找到路径"
                Log.e(TAG, errorMsg)
                return@withContext Result.failure(RuntimeException(errorMsg))
            }
            */
            
            // 临时实现：直接连线（演示用，需要替换为真实 BRouter 调用）
            val result = createSimulatedRoute(points, profile)
            Log.i(TAG, "路由计算成功（模拟）: ${result.getFormattedDistance()}, ${result.getFormattedTime()}")
            
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "路由计算异常", e)
            Result.failure(e)
        }
    }
    
    /**
     * 映射 profile 名称（兼容旧接口）
     */
    private fun mapProfile(profile: String): String {
        return when (profile) {
            PROFILE_CAR_LEGACY, PROFILE_CAR -> PROFILE_CAR
            PROFILE_BIKE_LEGACY, PROFILE_BIKE -> PROFILE_BIKE
            PROFILE_FOOT_LEGACY, PROFILE_FOOT -> PROFILE_FOOT
            else -> profile
        }
    }
    
    /**
     * 创建模拟路线（临时实现）
     * 
     * 注意：这是在 BRouter JAR 未添加时的临时实现
     * 实际使用时应替换为真实的 BRouter 路由计算
     */
    private fun createSimulatedRoute(points: List<LatLng>, profile: String): RouteResult {
        // 计算直线距离
        var totalDistance = 0.0
        for (i in 0 until points.size - 1) {
            totalDistance += calculateDistance(points[i], points[i + 1])
        }
        
        // 根据交通方式估算时间
        val speed = when (profile) {
            PROFILE_CAR, PROFILE_CAR_LEGACY -> 40.0  // km/h
            PROFILE_BIKE, PROFILE_BIKE_LEGACY -> 15.0
            else -> 5.0  // 步行
        }
        val time = (totalDistance / 1000 / speed * 3600 * 1000).toLong()
        
        // 创建导航指令
        val instructions = mutableListOf<RouteInstruction>()
        instructions.add(
            RouteInstruction(
                text = "出发",
                distance = 0.0,
                time = 0L,
                sign = InstructionSign.DEPART,
                location = points.first(),
                streetName = null,
                turnAngle = null
            )
        )
        
        instructions.add(
            RouteInstruction(
                text = "到达目的地",
                distance = totalDistance,
                time = time,
                sign = InstructionSign.ARRIVE,
                location = points.last(),
                streetName = null,
                turnAngle = null
            )
        )
        
        return RouteResult(
            distance = totalDistance,
            time = time,
            points = points,
            instructions = instructions,
            profile = profile
        )
    }
    
    /**
     * 计算两点间的直线距离（米）
     */
    private fun calculateDistance(p1: LatLng, p2: LatLng): Double {
        val earthRadius = 6371000.0 // 米
        val dLat = Math.toRadians(p2.lat - p1.lat)
        val dLon = Math.toRadians(p2.lon - p1.lon)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(p1.lat)) * Math.cos(Math.toRadians(p2.lat)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }
    
    /**
     * 获取支持的交通方式列表
     */
    fun getSupportedProfiles(): List<String> {
        return listOf(PROFILE_CAR_LEGACY, PROFILE_BIKE_LEGACY, PROFILE_FOOT_LEGACY)
    }
    
    /**
     * 获取交通方式的显示名称
     */
    fun getProfileDisplayName(profile: String): String {
        return when (profile) {
            PROFILE_CAR, PROFILE_CAR_LEGACY -> "驾车"
            PROFILE_BIKE, PROFILE_BIKE_LEGACY -> "骑行"
            PROFILE_FOOT, PROFILE_FOOT_LEGACY -> "步行"
            else -> profile
        }
    }
    
    /**
     * 释放资源
     */
    fun close() {
        segmentsDir = null
        profilesDir = null
        isInitialized = false
        Log.i(TAG, "BRouter 资源已释放")
    }
}

/**
 * BRouter 路由异常
 */
class BRouterException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

