package com.example.amap_sim.data.local

import android.util.Log
import btools.router.OsmNodeNamed
import btools.router.OsmTrack
import btools.router.RoutingContext
import btools.router.RoutingEngine
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
 */
class BRouterService(
    private val dataManager: OfflineDataManager
) {
    companion object {
        private const val TAG = "BRouterService"
        
        // BRouter profile 名称（使用官方 profile）
        const val PROFILE_CAR = "car-vario"      // 官方驾车配置
        const val PROFILE_BIKE = "trekking"      // 官方骑行配置
        const val PROFILE_FOOT = "shortest"      // 官方步行配置
        
        // 兼容旧接口的 profile 名称
        const val PROFILE_CAR_LEGACY = "car"
        const val PROFILE_BIKE_LEGACY = "bike"
        const val PROFILE_FOOT_LEGACY = "foot"
    }
    
    private var segmentsDir: File? = null
    private var profilesDir: File? = null
    private val initMutex = Mutex()
    private var isInitialized = false
    
    /**
     * 初始化路由引擎
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        initMutex.withLock {
            if (isInitialized) {
                Log.d(TAG, "BRouter 已初始化")
                return@withContext Result.success(Unit)
            }
            
            try {
                val brouterDir = dataManager.getBRouterDirectory()
                require(brouterDir.exists() && brouterDir.isDirectory) {
                    "BRouter 数据目录不存在: ${brouterDir.absolutePath}"
                }
                
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
            
            // 创建路由上下文
            val routingContext = RoutingContext().apply {
                localFunction = profileFile.absolutePath
                // 强制启用语音提示处理（解决 detourMap 为空时无法生成指令的问题）
                hasDirectRouting = true
            }
            
            // 创建路由点
            val waypoints = points.mapIndexed { index, point ->
                Log.d(TAG, "路由点[$index]: lat=${point.lat}, lon=${point.lon}")
                OsmNodeNamed().apply {
                    name = when (index) {
                        0 -> "from"
                        points.size - 1 -> "to"
                        else -> "via$index"
                    }
                    ilon = (point.lon * 1_000_000).toInt()
                    ilat = (point.lat * 1_000_000).toInt()
                    Log.d(TAG, "  转换后: ilon=$ilon, ilat=$ilat")
                }
            }
            
            Log.d(TAG, "创建路由引擎，segments目录: ${segmentsDir?.absolutePath}")
            
            // 列出 segments 目录中的文件
            segmentsDir?.listFiles()?.forEach { file ->
                Log.d(TAG, "  segment文件: ${file.name} (${file.length()} bytes)")
            }
            
            // 创建路由引擎
            val engine = RoutingEngine(
                null,  // outfileBase
                null,  // logfileBase  
                segmentsDir,  // segmentDir
                waypoints,  // waypoints
                routingContext  // routingContext
            )
            
            // 静默模式
            engine.quite = true
            
            // 执行路由计算
            Log.d(TAG, "开始路由计算...")
            engine.run()
            
            // 检查结果
            val track = engine.foundTrack
            val errorMessage = engine.errorMessage
            
            if (errorMessage != null) {
                Log.e(TAG, "路由计算失败: $errorMessage")
                return@withContext Result.failure(RuntimeException(errorMessage))
            }
            
            if (track == null || track.nodes.isEmpty()) {
                Log.e(TAG, "路由计算失败：未找到路径")
                return@withContext Result.failure(RuntimeException("未找到路径"))
            }
            
            // 转换结果
            val result = convertToRouteResult(track, profile)
            Log.i(TAG, "路由计算成功: ${result.getFormattedDistance()}, ${result.getFormattedTime()}, ${result.instructions.size} 条指令")
            
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "路由计算异常", e)
            Result.failure(e)
        }
    }
    
    /**
     * 映射 profile 名称
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
     * 将 BRouter 结果转换为领域模型
     */
    private fun convertToRouteResult(track: OsmTrack, profile: String): RouteResult {
        // 提取路线点（使用 public getter 方法）
        val routePoints = track.nodes.map { node ->
            LatLng(
                lat = node.iLat / 1_000_000.0,
                lon = node.iLon / 1_000_000.0
            )
        }
        
        // 计算总距离和时间
        var totalDistance = 0.0
        var totalTime = 0L
        
        for (i in 0 until track.nodes.size - 1) {
            val p1 = track.nodes[i]
            val p2 = track.nodes[i + 1]
            totalDistance += calculateDistance(
                p1.iLat / 1_000_000.0, p1.iLon / 1_000_000.0,
                p2.iLat / 1_000_000.0, p2.iLon / 1_000_000.0
            )
        }
        
        // 从 track 获取总时间（秒）
        totalTime = track.totalSeconds.toLong() * 1000
        
        // 如果 totalTime 为 0，根据距离和速度估算
        if (totalTime == 0L) {
            val speed = when (profile) {
                PROFILE_CAR, PROFILE_CAR_LEGACY -> 40.0  // km/h
                PROFILE_BIKE, PROFILE_BIKE_LEGACY -> 15.0
                else -> 5.0
            }
            totalTime = (totalDistance / 1000 / speed * 3600 * 1000).toLong()
        }
        
        // 提取导航指令
        val instructions = extractInstructions(track, routePoints)
        
        return RouteResult(
            distance = totalDistance,
            time = totalTime,
            points = routePoints,
            instructions = instructions,
            profile = profile
        )
    }
    
    /**
     * 从 VoiceHints 提取导航指令
     */
    private fun extractInstructions(track: OsmTrack, points: List<LatLng>): List<RouteInstruction> {
        val instructions = mutableListOf<RouteInstruction>()
        
        // 添加出发指令
        if (points.isNotEmpty()) {
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
        }
        
        // 从 VoiceHints 提取中间指令
        try {
            val voiceHints = track.voiceHints
            Log.d(TAG, "VoiceHints 对象: $voiceHints")
            
            if (voiceHints != null) {
                // 检查 turnInstructionMode
                val timField = voiceHints.javaClass.getDeclaredField("turnInstructionMode")
                timField.isAccessible = true
                val turnInstructionMode = timField.getInt(voiceHints)
                Log.d(TAG, "turnInstructionMode = $turnInstructionMode")
                
                // 使用反射安全访问 list 字段（因为是 package-private）
                val listField = voiceHints.javaClass.getDeclaredField("list")
                listField.isAccessible = true
                @Suppress("UNCHECKED_CAST")
                val hintList = listField.get(voiceHints) as? List<btools.router.VoiceHint>
                
                if (hintList != null) {
                    Log.d(TAG, "发现 ${hintList.size} 个语音提示")
                    
                    for ((idx, hint) in hintList.withIndex()) {
                        // 获取 indexInTrack（package-private）
                        val indexField = hint.javaClass.getDeclaredField("indexInTrack")
                        indexField.isAccessible = true
                        val indexInTrack = indexField.getInt(hint)
                        
                        // 获取 cmd（package-private）
                        val cmdField = hint.javaClass.getDeclaredField("cmd")
                        cmdField.isAccessible = true
                        val cmd = cmdField.getInt(hint)
                        
                        Log.d(TAG, "语音提示[$idx]: cmd=$cmd, indexInTrack=$indexInTrack")
                        
                        val index = indexInTrack.coerceIn(0, points.size - 1)
                        val location = points.getOrElse(index) { points.first() }
                        
                        // 跳过终点指令（稍后添加），END = 100
                        if (cmd == 100) continue
                        
                        val (text, sign) = translateVoiceHintByReflection(hint)
                        
                        // 获取 distanceToNext
                        val distField = hint.javaClass.getDeclaredField("distanceToNext")
                        distField.isAccessible = true
                        val distanceToNext = distField.getDouble(hint)
                        
                        Log.d(TAG, "  指令: $text, 距离: ${distanceToNext}m")
                        
                        instructions.add(
                            RouteInstruction(
                                text = text,
                                distance = distanceToNext,
                                time = (hint.time * 1000).toLong(),
                                sign = sign,
                                location = location,
                                streetName = getStreetNameFromHint(hint),
                                turnAngle = getAngleFromHint(hint)
                            )
                        )
                    }
                } else {
                    Log.w(TAG, "VoiceHints.list 为 null")
                }
            } else {
                Log.w(TAG, "track.voiceHints 为 null - 可能是 detourMap 和 hasDirectRouting 都为 false")
            }
        } catch (e: Exception) {
            Log.e(TAG, "提取导航指令失败: ${e.message}", e)
        }
        
        // 添加到达指令
        if (points.isNotEmpty()) {
            val totalDistance = if (instructions.isNotEmpty()) {
                instructions.sumOf { it.distance }
            } else {
                0.0
            }
            
            instructions.add(
                RouteInstruction(
                    text = "到达目的地",
                    distance = totalDistance,
                    time = 0L,
                    sign = InstructionSign.ARRIVE,
                    location = points.last(),
                    streetName = null,
                    turnAngle = null
                )
            )
        }
        
        return instructions
    }
    
    /**
     * 通过反射获取街道名称
     */
    private fun getStreetNameFromHint(hint: btools.router.VoiceHint): String? {
        return try {
            val goodWayField = hint.javaClass.getDeclaredField("goodWay")
            goodWayField.isAccessible = true
            val goodWay = goodWayField.get(hint) ?: return null
            
            // MessageData.wayKeyValues 包含道路标签
            val wayKeyValuesField = goodWay.javaClass.getDeclaredField("wayKeyValues")
            wayKeyValuesField.isAccessible = true
            val wayKeyValues = wayKeyValuesField.get(goodWay) as? String
            
            // 从标签中提取道路名称
            wayKeyValues?.let { tags ->
                // 尝试提取 name 标签
                val nameMatch = Regex("name=([^\\s]+)").find(tags)
                nameMatch?.groupValues?.getOrNull(1)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 通过反射获取转弯角度
     */
    private fun getAngleFromHint(hint: btools.router.VoiceHint): Double? {
        return try {
            val angleField = hint.javaClass.getDeclaredField("angle")
            angleField.isAccessible = true
            val angle = angleField.getFloat(hint)
            if (angle != Float.MAX_VALUE) angle.toDouble() else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 通过反射翻译 BRouter VoiceHint 为中文指令
     * 
     * VoiceHint cmd 常量值：
     * C=1(直行), TL=2(左转), TSLL=3(稍左), TSHL=4(急左),
     * TR=5(右转), TSLR=6(稍右), TSHR=7(急右),
     * KL=8(靠左), KR=9(靠右), TLU=10(左掉头), TRU=11(右掉头),
     * OFFR=12(偏离), RNDB=13(环岛), RNLB=14(左环岛),
     * TU=15(掉头), BL=16(直线), EL=17(左出), ER=18(右出),
     * END=100(终点)
     */
    private fun translateVoiceHintByReflection(hint: btools.router.VoiceHint): Pair<String, InstructionSign> {
        // 通过反射获取 cmd
        val cmd = try {
            val cmdField = hint.javaClass.getDeclaredField("cmd")
            cmdField.isAccessible = true
            cmdField.getInt(hint)
        } catch (e: Exception) {
            0
        }
        
        val streetName = getStreetNameFromHint(hint)
        
        // VoiceHint 常量值
        val C = 1; val TL = 2; val TSLL = 3; val TSHL = 4
        val TR = 5; val TSLR = 6; val TSHR = 7
        val KL = 8; val KR = 9; val TLU = 10; val TRU = 11
        val RNDB = 13; val RNLB = 14; val TU = 15
        val EL = 17; val ER = 18; val END = 100
        
        val exitNumber = hint.exitNumber
        
        val (action, sign) = when (cmd) {
            C -> "继续直行" to InstructionSign.CONTINUE
            TL -> "左转" to InstructionSign.LEFT
            TSLL -> "稍向左转" to InstructionSign.SLIGHT_LEFT
            TSHL -> "向左急转" to InstructionSign.SHARP_LEFT
            TR -> "右转" to InstructionSign.RIGHT
            TSLR -> "稍向右转" to InstructionSign.SLIGHT_RIGHT
            TSHR -> "向右急转" to InstructionSign.SHARP_RIGHT
            KL -> "靠左行驶" to InstructionSign.KEEP_LEFT
            KR -> "靠右行驶" to InstructionSign.KEEP_RIGHT
            TLU, TU -> "掉头" to InstructionSign.U_TURN
            TRU -> "向右掉头" to InstructionSign.U_TURN_RIGHT
            RNDB -> "进入环岛，第${exitNumber}出口驶出" to InstructionSign.ROUNDABOUT
            RNLB -> "进入环岛（左转），第${exitNumber}出口驶出" to InstructionSign.ROUNDABOUT
            EL -> "从左侧驶出" to InstructionSign.KEEP_LEFT
            ER -> "从右侧驶出" to InstructionSign.KEEP_RIGHT
            END -> "到达目的地" to InstructionSign.ARRIVE
            else -> "继续前行" to InstructionSign.UNKNOWN
        }
        
        val text = if (!streetName.isNullOrBlank() && cmd != END) {
            when (cmd) {
                C -> "沿${streetName}继续直行"
                TL, TSLL, TSHL -> "${action}进入${streetName}"
                TR, TSLR, TSHR -> "${action}进入${streetName}"
                else -> "${action}，沿${streetName}"
            }
        } else {
            action
        }
        
        return text to sign
    }
    
    /**
     * 计算两点间的直线距离（米）
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
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
