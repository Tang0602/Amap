package com.example.amap_sim.data.local

import android.util.Log
import com.example.amap_sim.domain.model.InstructionSign
import com.example.amap_sim.domain.model.LatLng
import com.example.amap_sim.domain.model.RouteInstruction
import com.example.amap_sim.domain.model.RouteResult
import com.graphhopper.GHRequest
import com.graphhopper.GraphHopper
import com.graphhopper.config.CHProfile
import com.graphhopper.config.Profile
import com.graphhopper.util.Instruction
import com.graphhopper.util.Parameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * 离线路由服务
 * 
 * 基于 GraphHopper 实现的离线路径规划服务
 * 
 * 功能：
 * - 两点间路径规划
 * - 多途经点路径规划
 * - 支持多种交通方式（驾车、骑行、步行）
 * - 提供详细导航指令
 */
class OfflineRoutingService(
    private val dataManager: OfflineDataManager
) {
    companion object {
        private const val TAG = "OfflineRoutingService"
        
        // 交通方式配置
        const val PROFILE_CAR = "car"
        const val PROFILE_BIKE = "bike"
        const val PROFILE_FOOT = "foot"
    }
    
    private var graphHopper: GraphHopper? = null
    private val initMutex = Mutex()
    private var isInitialized = false
    
    /**
     * 初始化路由引擎
     * 
     * 必须在使用前调用，建议在 Application 启动时初始化
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        initMutex.withLock {
            if (isInitialized && graphHopper != null) {
                Log.d(TAG, "路由引擎已初始化")
                return@withContext Result.success(Unit)
            }
            
            try {
                val routeDir = dataManager.getRouteDirectory()
                require(routeDir.exists() && routeDir.isDirectory) {
                    "路由数据目录不存在: ${routeDir.absolutePath}"
                }
                
                Log.i(TAG, "开始初始化 GraphHopper，数据目录: ${routeDir.absolutePath}")
                
                graphHopper = GraphHopper().apply {
                    // 设置数据目录
                    graphHopperLocation = routeDir.absolutePath
                    
                    // 不需要 OSM 文件（数据已预处理）
                    setAllowWrites(false)
                    
                    // 加载已有数据
                    load()
                }
                
                isInitialized = true
                Log.i(TAG, "GraphHopper 初始化完成")
                
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "GraphHopper 初始化失败", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * 检查是否已初始化
     */
    fun isReady(): Boolean = isInitialized && graphHopper != null
    
    /**
     * 计算两点间路径
     * 
     * @param start 起点坐标
     * @param end 终点坐标
     * @param profile 交通方式：car, bike, foot
     * @return 路线结果，失败返回 null
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
            val hopper = graphHopper
            requireNotNull(hopper) { "路由引擎未初始化，请先调用 initialize()" }
            require(points.size >= 2) { "至少需要两个点（起点和终点）" }
            
            Log.d(TAG, "计算路径: ${points.size} 个点, 模式: $profile")
            
            // 创建路由请求
            val request = GHRequest().apply {
                // 添加所有点
                points.forEach { point ->
                    addPoint(com.graphhopper.util.shapes.GHPoint(point.lat, point.lon))
                }
                
                // 设置交通方式
                setProfile(profile)
                
                // 设置语言（影响导航指令）
                locale = Locale.CHINESE
                
                // 启用导航指令
                putHint(Parameters.Routing.INSTRUCTIONS, true)
                
                // 使用 CH 加速（如果可用）
                algorithm = Parameters.Algorithms.DIJKSTRA_BI
            }
            
            // 执行路由计算
            val response = hopper.route(request)
            
            // 检查错误
            if (response.hasErrors()) {
                val errors = response.errors.joinToString(", ") { it.message ?: "未知错误" }
                Log.e(TAG, "路由计算失败: $errors")
                return@withContext Result.failure(RuntimeException("路由计算失败: $errors"))
            }
            
            // 获取最佳路径
            val path = response.best
            
            // 提取路线点
            val routePoints = mutableListOf<LatLng>()
            val pointList = path.points
            for (i in 0 until pointList.size()) {
                routePoints.add(LatLng(pointList.getLat(i), pointList.getLon(i)))
            }
            
            // 提取导航指令
            val instructions = mutableListOf<RouteInstruction>()
            val instructionList = path.instructions
            for (instruction in instructionList) {
                val firstPoint = instruction.points
                val location = if (firstPoint.size() > 0) {
                    LatLng(firstPoint.getLat(0), firstPoint.getLon(0))
                } else {
                    points.first()
                }
                
                instructions.add(
                    RouteInstruction(
                        text = translateInstruction(instruction),
                        distance = instruction.distance,
                        time = instruction.time,
                        sign = InstructionSign.fromGraphHopperSign(instruction.sign),
                        location = location,
                        streetName = instruction.name.takeIf { it.isNotBlank() },
                        turnAngle = null // GraphHopper 8.x 需要单独计算
                    )
                )
            }
            
            val result = RouteResult(
                distance = path.distance,
                time = path.time,
                points = routePoints,
                instructions = instructions,
                profile = profile
            )
            
            Log.i(TAG, "路由计算成功: ${result.getFormattedDistance()}, ${result.getFormattedTime()}")
            
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "路由计算异常", e)
            Result.failure(e)
        }
    }
    
    /**
     * 翻译导航指令为中文
     */
    private fun translateInstruction(instruction: Instruction): String {
        val streetName = instruction.name.takeIf { it.isNotBlank() }
        val sign = instruction.sign
        
        val action = when (sign) {
            Instruction.CONTINUE_ON_STREET -> "继续沿"
            Instruction.TURN_SLIGHT_LEFT -> "稍向左转"
            Instruction.TURN_LEFT -> "左转"
            Instruction.TURN_SHARP_LEFT -> "向左急转"
            Instruction.TURN_SLIGHT_RIGHT -> "稍向右转"
            Instruction.TURN_RIGHT -> "右转"
            Instruction.TURN_SHARP_RIGHT -> "向右急转"
            Instruction.FINISH -> "到达目的地"
            Instruction.REACHED_VIA -> "到达途经点"
            Instruction.USE_ROUNDABOUT -> "进入环岛"
            Instruction.LEAVE_ROUNDABOUT -> "驶出环岛"
            Instruction.KEEP_LEFT -> "靠左行驶"
            Instruction.KEEP_RIGHT -> "靠右行驶"
            Instruction.U_TURN_LEFT -> "向左掉头"
            Instruction.U_TURN_RIGHT -> "向右掉头"
            Instruction.U_TURN_UNKNOWN -> "掉头"
            else -> "继续前行"
        }
        
        return if (streetName != null && sign != Instruction.FINISH) {
            when (sign) {
                Instruction.CONTINUE_ON_STREET -> "${action}${streetName}行驶"
                Instruction.TURN_SLIGHT_LEFT, Instruction.TURN_LEFT, Instruction.TURN_SHARP_LEFT,
                Instruction.TURN_SLIGHT_RIGHT, Instruction.TURN_RIGHT, Instruction.TURN_SHARP_RIGHT -> 
                    "${action}进入${streetName}"
                else -> "${action}，沿${streetName}"
            }
        } else {
            action
        }
    }
    
    /**
     * 获取支持的交通方式列表
     */
    fun getSupportedProfiles(): List<String> {
        return listOf(PROFILE_CAR, PROFILE_BIKE, PROFILE_FOOT)
    }
    
    /**
     * 获取交通方式的显示名称
     */
    fun getProfileDisplayName(profile: String): String {
        return when (profile) {
            PROFILE_CAR -> "驾车"
            PROFILE_BIKE -> "骑行"
            PROFILE_FOOT -> "步行"
            else -> profile
        }
    }
    
    /**
     * 释放资源
     */
    fun close() {
        try {
            graphHopper?.close()
            graphHopper = null
            isInitialized = false
            Log.i(TAG, "GraphHopper 资源已释放")
        } catch (e: Exception) {
            Log.e(TAG, "释放 GraphHopper 资源失败", e)
        }
    }
}

/**
 * 路由异常
 */
class RoutingException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
