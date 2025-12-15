# BRouter 迁移方案

## 背景

GraphHopper 从 2.0 版本开始移除了官方 Android 模块支持。虽然 `graphhopper-core` 仍可在 Android 上运行，但存在以下问题：

- 内存占用大（针对服务器优化）
- 缺少移动端优化
- 官方不再维护 Android 支持

**BRouter** 是专为移动端设计的开源离线路由引擎，被 OsmAnd、Locus Map 等知名应用采用，是更适合本项目的选择。

---

## 迁移工作清单

### 📋 工作概览

| 阶段 | 任务 | 预估时间 |
|------|------|----------|
| **阶段1** | 数据准备工具 | 2-3小时 |
| **阶段2** | Android 集成 | 4-6小时 |
| **阶段3** | 代码适配 | 2-3小时 |
| **阶段4** | 测试验证 | 2-3小时 |
| **总计** | | **10-15小时** |

---

## 阶段1：数据准备工具

### 1.1 BRouter 数据格式说明

BRouter 使用 `.rd5` 格式的分片数据文件：

```
数据分片示例（5°x5° 网格）:
├── E110_N25.rd5   # 经度 110-115°, 纬度 25-30°
├── E110_N30.rd5   # 经度 110-115°, 纬度 30-35°
├── E115_N25.rd5   # 经度 115-120°, 纬度 25-30°
└── E115_N30.rd5   # 经度 115-120°, 纬度 30-35°
```

武汉市（113.7°E-115.1°E, 29.9°N-31.4°N）需要的分片：
- `E110_N25.rd5`（覆盖 110-115°E, 25-30°N）
- `E110_N30.rd5`（覆盖 110-115°E, 30-35°N）

### 1.2 创建数据生成脚本

创建 `scripts/04_generate_brouter.sh`：

```bash
#!/bin/bash
#
# 步骤 4: 生成 BRouter 路由数据
#
# 使用方法：
#   ./04_generate_brouter.sh
#   ./04_generate_brouter.sh -c beijing
#

set -e
set -o pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/common.sh"

# BRouter 版本
BROUTER_VERSION="1.7.5"

usage() {
    cat << EOF
步骤 4: 生成 BRouter 路由数据

用法: $0 [选项]

选项:
  -c, --city NAME      城市名称 (默认: $CITY_NAME)
  -b, --bbox BBOX      边界框 (默认: $BBOX)
  -o, --output DIR     输出目录 (默认: $OUTPUT_DIR)
  -f, --force          强制重新生成
  -h, --help           显示此帮助

输出:
  \${OUTPUT_DIR}/brouter/segments/  - rd5 数据文件
  \${OUTPUT_DIR}/brouter/profiles/  - 路由配置文件

EOF
    exit 0
}

# 下载 BRouter 工具
download_brouter_tools() {
    local brouter_dir="$DOWNLOAD_DIR/brouter"
    local brouter_jar="$brouter_dir/brouter-${BROUTER_VERSION}.jar"
    
    if [ -f "$brouter_jar" ]; then
        log_info "BRouter 工具已存在，跳过下载"
        return 0
    fi
    
    log_info "下载 BRouter 工具..."
    mkdir -p "$brouter_dir"
    
    # 从 GitHub Releases 下载
    download_with_retry \
        "https://github.com/abrensch/brouter/releases/download/v${BROUTER_VERSION}/brouter-${BROUTER_VERSION}.zip" \
        "$brouter_dir/brouter.zip" \
        "BRouter" || return 1
    
    # 解压
    unzip -o "$brouter_dir/brouter.zip" -d "$brouter_dir"
    
    log_success "BRouter 工具下载完成"
}

# 计算需要的 rd5 分片
calculate_segments() {
    local bbox="$1"
    local min_lon min_lat max_lon max_lat
    
    IFS=',' read -r min_lon min_lat max_lon max_lat <<< "$bbox"
    
    # 计算分片范围（5度网格）
    python3 << EOF
import math

min_lon, min_lat = $min_lon, $min_lat
max_lon, max_lat = $max_lon, $max_lat

# BRouter 使用 5 度网格
def get_segment(lon, lat):
    seg_lon = int(lon // 5) * 5
    seg_lat = int(lat // 5) * 5
    ew = 'E' if seg_lon >= 0 else 'W'
    ns = 'N' if seg_lat >= 0 else 'S'
    return f"{ew}{abs(seg_lon):03d}_{ns}{abs(seg_lat):02d}"

segments = set()
for lon in range(int(min_lon), int(max_lon) + 5, 5):
    for lat in range(int(min_lat), int(max_lat) + 5, 5):
        segments.add(get_segment(lon, lat))

for seg in sorted(segments):
    print(seg)
EOF
}

# 下载 rd5 分片数据
download_segments() {
    local output_dir="$OUTPUT_DIR/brouter/segments"
    mkdir -p "$output_dir"
    
    log_info "计算需要下载的数据分片..."
    local segments=$(calculate_segments "$BBOX")
    
    log_info "需要下载的分片: $segments"
    
    for segment in $segments; do
        local rd5_file="$output_dir/${segment}.rd5"
        
        if [ -f "$rd5_file" ]; then
            log_info "分片 ${segment} 已存在，跳过"
            continue
        fi
        
        log_info "下载分片: ${segment}..."
        
        # 从 BRouter 官方服务器下载
        download_with_retry \
            "https://brouter.de/brouter/segments4/${segment}.rd5" \
            "$rd5_file" \
            "分片 ${segment}" || {
                log_warn "无法从官方服务器下载，尝试备用源..."
                # 备用源：从 OSM 数据生成（需要更多时间）
                generate_segment_from_osm "$segment" "$rd5_file"
            }
    done
    
    log_success "数据分片下载完成"
}

# 从 OSM 数据生成分片（备用方案）
generate_segment_from_osm() {
    local segment="$1"
    local output_file="$2"
    
    log_warn "从 OSM 数据生成分片 ${segment}（这可能需要较长时间）..."
    
    local brouter_dir="$DOWNLOAD_DIR/brouter"
    local osm_file="$TEMP_DIR/${CITY_NAME}.osm.pbf"
    
    if [ ! -f "$osm_file" ]; then
        log_error "未找到 OSM 数据文件: $osm_file"
        return 1
    fi
    
    # 使用 BRouter 的 mapcreator 工具
    java -Xmx4G -jar "$brouter_dir/brouter.jar" \
        segments "$osm_file" "$output_file" \
        || return 1
    
    log_success "分片 ${segment} 生成完成"
}

# 复制路由配置文件
setup_profiles() {
    local profiles_dir="$OUTPUT_DIR/brouter/profiles"
    mkdir -p "$profiles_dir"
    
    log_info "设置路由配置文件..."
    
    # 从 BRouter 工具包复制配置文件
    local brouter_dir="$DOWNLOAD_DIR/brouter"
    
    if [ -d "$brouter_dir/profiles2" ]; then
        cp "$brouter_dir/profiles2/"*.brf "$profiles_dir/" 2>/dev/null || true
    fi
    
    # 创建简化的配置文件（如果不存在）
    create_car_profile "$profiles_dir"
    create_bike_profile "$profiles_dir"
    create_foot_profile "$profiles_dir"
    
    log_success "路由配置文件设置完成"
}

# 创建驾车配置
create_car_profile() {
    local dir="$1"
    local file="$dir/car-fast.brf"
    
    if [ -f "$file" ]; then
        return 0
    fi
    
    cat > "$file" << 'PROFILE'
---context:global
assign processUnusedTags = false
assign turnInstructionMode = 1
assign turnInstructionCatchingRange = 40
assign turnInstructionRoundabouts = true

---context:way
assign turncost = 0
assign initialcost = 0

assign costfactor
  switch highway=motorway       1
  switch highway=motorway_link  1.1
  switch highway=trunk          1.1
  switch highway=trunk_link     1.2
  switch highway=primary        1.2
  switch highway=primary_link   1.3
  switch highway=secondary      1.3
  switch highway=secondary_link 1.4
  switch highway=tertiary       1.4
  switch highway=tertiary_link  1.5
  switch highway=unclassified   1.6
  switch highway=residential    1.8
  switch highway=living_street  2.5
  switch highway=service        2.0
  10000

---context:node
assign initialcost = 0
PROFILE
}

# 创建骑行配置
create_bike_profile() {
    local dir="$1"
    local file="$dir/trekking.brf"
    
    if [ -f "$file" ]; then
        return 0
    fi
    
    cat > "$file" << 'PROFILE'
---context:global
assign processUnusedTags = false
assign turnInstructionMode = 1
assign turnInstructionCatchingRange = 40
assign turnInstructionRoundabouts = true

---context:way
assign turncost = 0
assign initialcost = 0

assign costfactor
  switch highway=cycleway       1
  switch highway=path           1.2
  switch highway=footway        1.5
  switch highway=pedestrian     1.5
  switch highway=residential    1.3
  switch highway=living_street  1.2
  switch highway=service        1.5
  switch highway=tertiary       1.5
  switch highway=secondary      2.0
  switch highway=primary        3.0
  switch highway=trunk          10000
  switch highway=motorway       10000
  10000

---context:node
assign initialcost = 0
PROFILE
}

# 创建步行配置
create_foot_profile() {
    local dir="$1"
    local file="$dir/shortest.brf"
    
    if [ -f "$file" ]; then
        return 0
    fi
    
    cat > "$file" << 'PROFILE'
---context:global
assign processUnusedTags = false
assign turnInstructionMode = 1
assign turnInstructionCatchingRange = 20
assign turnInstructionRoundabouts = false

---context:way
assign turncost = 0
assign initialcost = 0

assign costfactor
  switch highway=footway        1
  switch highway=pedestrian     1
  switch highway=path           1
  switch highway=steps          1.5
  switch highway=cycleway       1.2
  switch highway=living_street  1.1
  switch highway=residential    1.2
  switch highway=service        1.3
  switch highway=unclassified   1.5
  switch highway=tertiary       2.0
  switch highway=secondary      3.0
  switch highway=primary        5.0
  switch highway=trunk          10000
  switch highway=motorway       10000
  10000

---context:node
assign initialcost = 0
PROFILE
}

# 主逻辑
FORCE=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -c|--city) CITY_NAME="$2"; shift 2 ;;
        -b|--bbox) BBOX="$2"; shift 2 ;;
        -o|--output) OUTPUT_DIR="$2"; shift 2 ;;
        -f|--force) FORCE=true; shift ;;
        -h|--help) usage ;;
        *) shift ;;
    esac
done

main() {
    echo ""
    echo "============================================================"
    echo "  步骤 4: 生成 BRouter 路由数据"
    echo "============================================================"
    echo "  城市: ${CITY_NAME}"
    echo "  边界框: ${BBOX}"
    echo "  输出目录: ${OUTPUT_DIR}"
    echo "============================================================"
    echo ""
    
    # 准备目录
    prepare_directories
    
    local output_dir="$OUTPUT_DIR/brouter"
    
    # 强制模式下删除已有目录
    if [ "$FORCE" = true ] && [ -d "$output_dir" ]; then
        log_warn "强制模式: 删除已有目录"
        rm -rf "$output_dir"
    fi
    
    # 下载数据分片
    download_segments || exit 1
    
    # 设置配置文件
    setup_profiles || exit 1
    
    echo ""
    log_success "BRouter 路由数据准备完成！"
    echo "  数据目录: $output_dir/segments"
    echo "  配置目录: $output_dir/profiles"
    echo ""
}

main
```

### 1.3 修改 common.sh 添加 BRouter 配置

在 `common.sh` 中添加：

```bash
# BRouter 版本
BROUTER_VERSION="1.7.5"

# BRouter 数据下载镜像
BROUTER_SEGMENTS_URL="https://brouter.de/brouter/segments4"
```

---

## 阶段2：Android 集成

### 2.1 添加 BRouter 依赖

BRouter 没有官方 Maven 发布，需要从源码编译或直接包含 JAR。

#### 方案 A：使用预编译 JAR（推荐）

1. 下载 BRouter 源码并编译：

```bash
git clone https://github.com/abrensch/brouter.git
cd brouter
./gradlew :brouter-core:jar
```

2. 复制 JAR 到项目：

```bash
mkdir -p app/libs
cp brouter/brouter-core/build/libs/brouter-core-*.jar app/libs/
```

3. 修改 `app/build.gradle.kts`：

```kotlin
dependencies {
    // 移除 GraphHopper
    // implementation(libs.graphhopper.core)
    
    // 添加 BRouter（从 libs 目录加载）
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    
    // 保留 SLF4J（BRouter 也需要）
    implementation(libs.slf4j.android)
}
```

#### 方案 B：使用 JitPack

修改根目录 `build.gradle.kts`：

```kotlin
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

修改 `libs.versions.toml`：

```toml
[versions]
brouter = "1.7.5"

[libraries]
brouter-core = { group = "com.github.abrensch.brouter", name = "brouter-core", version.ref = "brouter" }
```

### 2.2 创建 BRouterService

创建 `app/src/main/java/com/example/amap_sim/data/local/BRouterService.kt`：

```kotlin
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
 * 专为移动端设计的离线路径规划引擎
 * 
 * 功能：
 * - 两点间路径规划
 * - 多途经点路径规划
 * - 支持多种交通方式（驾车、骑行、步行）
 * - 提供详细导航指令
 * 
 * 优势：
 * - 内存占用低
 * - 专为移动端优化
 * - 数据格式紧凑
 */
class BRouterService(
    private val dataManager: OfflineDataManager
) {
    companion object {
        private const val TAG = "BRouterService"
        
        // 交通方式对应的 BRouter profile
        const val PROFILE_CAR = "car-fast"
        const val PROFILE_BIKE = "trekking"
        const val PROFILE_FOOT = "shortest"
        
        // 兼容旧接口
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
                // 获取 BRouter 数据目录
                val brouterDir = dataManager.getBRouterDirectory()
                require(brouterDir.exists() && brouterDir.isDirectory) {
                    "BRouter 数据目录不存在: ${brouterDir.absolutePath}"
                }
                
                segmentsDir = File(brouterDir, "segments").also {
                    require(it.exists() && it.isDirectory) {
                        "BRouter segments 目录不存在: ${it.absolutePath}"
                    }
                    // 检查是否有 rd5 文件
                    val rd5Files = it.listFiles { f -> f.extension == "rd5" }
                    require(!rd5Files.isNullOrEmpty()) {
                        "BRouter segments 目录中没有 rd5 文件"
                    }
                    Log.i(TAG, "找到 ${rd5Files.size} 个 rd5 数据文件")
                }
                
                profilesDir = File(brouterDir, "profiles").also {
                    require(it.exists() && it.isDirectory) {
                        "BRouter profiles 目录不存在: ${it.absolutePath}"
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
            
            // 创建路由上下文
            val routingContext = RoutingContext().apply {
                localFunction = File(profilesDir, "$actualProfile.brf").absolutePath
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
                Result.success(result)
            } else {
                val errorMsg = engine.errorMessage ?: "路由计算失败：未找到路径"
                Log.e(TAG, errorMsg)
                Result.failure(RuntimeException(errorMsg))
            }
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
     * 将 BRouter 结果转换为领域模型
     */
    private fun convertToRouteResult(track: OsmTrack, profile: String): RouteResult {
        // 提取路线点
        val routePoints = track.nodes.map { node ->
            LatLng(
                lat = node.ilat / 1_000_000.0,
                lon = node.ilon / 1_000_000.0
            )
        }
        
        // 提取导航指令
        val instructions = extractInstructions(track, routePoints)
        
        return RouteResult(
            distance = track.distance.toDouble(),
            time = (track.getTotalSeconds() * 1000).toLong(),
            points = routePoints,
            instructions = instructions,
            profile = profile
        )
    }
    
    /**
     * 从轨迹中提取导航指令
     */
    private fun extractInstructions(track: OsmTrack, points: List<LatLng>): List<RouteInstruction> {
        val instructions = mutableListOf<RouteInstruction>()
        
        // BRouter 的 VoiceHints 包含导航指令
        val voiceHints = track.voiceHints ?: return instructions
        
        for (hint in voiceHints) {
            val index = hint.indexInTrack.coerceIn(0, points.size - 1)
            val location = points[index]
            
            instructions.add(
                RouteInstruction(
                    text = translateCommand(hint.command, hint.arg),
                    distance = hint.distanceToNext.toDouble(),
                    time = (hint.distanceToNext / 10 * 1000).toLong(), // 估算时间
                    sign = mapCommandToSign(hint.command),
                    location = location,
                    streetName = hint.arg.takeIf { it.isNotBlank() },
                    turnAngle = null
                )
            )
        }
        
        // 添加到达指令
        if (points.isNotEmpty()) {
            instructions.add(
                RouteInstruction(
                    text = "到达目的地",
                    distance = 0.0,
                    time = 0L,
                    sign = InstructionSign.FINISH,
                    location = points.last(),
                    streetName = null,
                    turnAngle = null
                )
            )
        }
        
        return instructions
    }
    
    /**
     * 翻译导航指令
     */
    private fun translateCommand(command: Int, streetName: String?): String {
        val action = when (command) {
            1 -> "直行"
            2 -> "稍向左转"
            3 -> "左转"
            4 -> "向左急转"
            5 -> "稍向右转"
            6 -> "右转"
            7 -> "向右急转"
            8 -> "掉头"
            9 -> "进入环岛"
            10 -> "驶出环岛"
            11 -> "靠左行驶"
            12 -> "靠右行驶"
            else -> "继续前行"
        }
        
        return if (!streetName.isNullOrBlank()) {
            when (command) {
                in 2..7 -> "${action}进入$streetName"
                else -> "${action}，沿$streetName"
            }
        } else {
            action
        }
    }
    
    /**
     * 映射 BRouter 指令到领域模型
     */
    private fun mapCommandToSign(command: Int): InstructionSign {
        return when (command) {
            1 -> InstructionSign.CONTINUE_ON_STREET
            2 -> InstructionSign.TURN_SLIGHT_LEFT
            3 -> InstructionSign.TURN_LEFT
            4 -> InstructionSign.TURN_SHARP_LEFT
            5 -> InstructionSign.TURN_SLIGHT_RIGHT
            6 -> InstructionSign.TURN_RIGHT
            7 -> InstructionSign.TURN_SHARP_RIGHT
            8 -> InstructionSign.U_TURN_UNKNOWN
            9 -> InstructionSign.USE_ROUNDABOUT
            10 -> InstructionSign.LEAVE_ROUNDABOUT
            11 -> InstructionSign.KEEP_LEFT
            12 -> InstructionSign.KEEP_RIGHT
            else -> InstructionSign.UNKNOWN
        }
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
```

### 2.3 更新 OfflineDataManager

修改 `OfflineDataManager.kt`，添加 BRouter 目录支持：

```kotlin
/**
 * 获取 BRouter 数据目录
 */
fun getBRouterDirectory(): File {
    return File(getMapDataDirectory(), "brouter")
}

/**
 * 获取 BRouter segments 目录
 */
fun getBRouterSegmentsDirectory(): File {
    return File(getBRouterDirectory(), "segments")
}

/**
 * 获取 BRouter profiles 目录
 */
fun getBRouterProfilesDirectory(): File {
    return File(getBRouterDirectory(), "profiles")
}

/**
 * 检查 BRouter 数据是否就绪
 */
fun isBRouterDataReady(): Boolean {
    val segmentsDir = getBRouterSegmentsDirectory()
    val profilesDir = getBRouterProfilesDirectory()
    
    if (!segmentsDir.exists() || !profilesDir.exists()) {
        return false
    }
    
    // 检查是否有 rd5 文件
    val rd5Files = segmentsDir.listFiles { f -> f.extension == "rd5" }
    if (rd5Files.isNullOrEmpty()) {
        return false
    }
    
    // 检查是否有 profile 文件
    val brfFiles = profilesDir.listFiles { f -> f.extension == "brf" }
    if (brfFiles.isNullOrEmpty()) {
        return false
    }
    
    return true
}
```

---

## 阶段3：代码适配

### 3.1 创建统一路由接口

创建 `domain/repository/RoutingService.kt`：

```kotlin
package com.example.amap_sim.domain.repository

import com.example.amap_sim.domain.model.LatLng
import com.example.amap_sim.domain.model.RouteResult

/**
 * 路由服务接口
 * 
 * 抽象路由引擎实现，支持 GraphHopper/BRouter 切换
 */
interface RoutingService {
    suspend fun initialize(): Result<Unit>
    fun isReady(): Boolean
    suspend fun calculateRoute(start: LatLng, end: LatLng, profile: String): Result<RouteResult>
    suspend fun calculateRouteWithWaypoints(points: List<LatLng>, profile: String): Result<RouteResult>
    fun getSupportedProfiles(): List<String>
    fun getProfileDisplayName(profile: String): String
    fun close()
    
    companion object {
        const val PROFILE_CAR = "car"
        const val PROFILE_BIKE = "bike"
        const val PROFILE_FOOT = "foot"
    }
}
```

### 3.2 更新 ServiceLocator

修改 `di/ServiceLocator.kt`：

```kotlin
// 路由服务（使用 BRouter 替代 GraphHopper）
val routingService: BRouterService by lazy {
    BRouterService(offlineDataManager)
}

// 如需切换回 GraphHopper，取消注释：
// val routingService: OfflineRoutingService by lazy {
//     OfflineRoutingService(offlineDataManager)
// }
```

### 3.3 更新 RoutePlanningViewModel

```kotlin
// 原来的代码
private val routingService = ServiceLocator.routingService  // 现在指向 BRouterService

// 接口调用保持不变，因为方法签名相同
viewModelScope.launch {
    routingService.calculateRoute(start, end, profile)
        .onSuccess { result ->
            _uiState.update { it.copy(routeResult = result) }
        }
        .onFailure { error ->
            _uiState.update { it.copy(error = error.message) }
        }
}
```

---

## 阶段4：数据目录结构

迁移后的 assets 目录结构：

```
app/src/main/assets/map/
├── wuhan.map                    # Mapsforge 地图文件
├── wuhan_poi.db                 # POI 数据库
├── theme.xml                    # 地图主题
└── brouter/                     # BRouter 数据（替代 wuhan-gh）
    ├── segments/                # rd5 数据文件
    │   ├── E110_N25.rd5
    │   └── E110_N30.rd5
    └── profiles/                # 路由配置文件
        ├── car-fast.brf
        ├── trekking.brf
        └── shortest.brf
```

---

## 迁移检查清单

### 开发阶段

- [ ] 编译 BRouter JAR 或配置 JitPack
- [ ] 创建 `04_generate_brouter.sh` 脚本
- [ ] 生成武汉市 rd5 数据
- [ ] 创建 BRouterService 类
- [ ] 更新 OfflineDataManager
- [ ] 更新 ServiceLocator
- [ ] 移除 GraphHopper 依赖

### 测试阶段

- [ ] 单元测试：路由计算
- [ ] 集成测试：完整流程
- [ ] 真机测试：内存占用
- [ ] 模拟器测试：兼容性

### 发布前

- [ ] 更新文档
- [ ] 清理无用代码
- [ ] 更新 ProGuard 规则

---

## ProGuard 配置

```proguard
# BRouter
-keep class btools.** { *; }
-dontwarn btools.**

# 移除 GraphHopper 规则
# -keep class com.graphhopper.** { *; }
```

---

## FAQ

### Q: BRouter 和 GraphHopper 哪个更快？

A: 对于移动端，BRouter 通常更快，因为：
- 数据格式更紧凑
- 内存管理针对移动端优化
- 启动时间更短

### Q: 可以同时保留两个引擎吗？

A: 可以，通过 `RoutingService` 接口抽象，运行时动态切换。

### Q: rd5 数据和 GraphHopper 数据可以共存吗？

A: 可以，它们使用不同的目录，不会冲突。

---

**文档版本**：1.0  
**创建日期**：2024年12月  
**作者**：AI Assistant

