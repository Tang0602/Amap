#!/bin/bash
#
# 步骤 3: 生成 Mapsforge 地图文件
#
# 使用方法：
#   ./03_generate_map.sh
#   ./03_generate_map.sh -c beijing -b 115.4,39.4,117.5,41.1
#

set -e

# 加载共享配置
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/common.sh"

# ============================================================================
# 使用帮助
# ============================================================================

usage() {
    cat << EOF
步骤 3: 生成 Mapsforge 地图文件

用法: $0 [选项]

选项:
  -c, --city NAME      城市名称 (默认: $CITY_NAME)
  -b, --bbox BBOX      边界框 (默认: $BBOX)
  -o, --output DIR     输出目录 (默认: $OUTPUT_DIR)
  -f, --force          强制重新生成
  -h, --help           显示此帮助

输出:
  \${OUTPUT_DIR}/\${CITY_NAME}.map  - Mapsforge 地图文件
  \${OUTPUT_DIR}/theme.xml         - 地图主题文件

EOF
    exit 0
}

# ============================================================================
# 下载 Mapsforge 工具
# ============================================================================

download_mapsforge_tools() {
    local osmosis_dir="$DOWNLOAD_DIR/osmosis"
    local writer_jar="$DOWNLOAD_DIR/mapsforge-map-writer-${MAPSFORGE_WRITER_VERSION}.jar"
    
    # 下载 Osmosis
    if [ ! -d "$osmosis_dir" ] || [ ! -f "$osmosis_dir/bin/osmosis" ]; then
        log_info "下载 Osmosis..."
        
        download_with_retry \
            "https://github.com/openstreetmap/osmosis/releases/download/0.49.2/osmosis-0.49.2.tar" \
            "$DOWNLOAD_DIR/osmosis-latest.tar" \
            "Osmosis" || return 1
        
        mkdir -p "$osmosis_dir"
        tar -xf "$DOWNLOAD_DIR/osmosis-latest.tar" -C "$osmosis_dir" --strip-components=1
        chmod +x "$osmosis_dir/bin/osmosis"
        
        log_success "Osmosis 下载完成"
    else
        log_info "Osmosis 已存在，跳过下载"
    fi
    
    # 下载 Mapsforge Writer 插件
    if [ ! -f "$writer_jar" ]; then
        log_info "下载 Mapsforge Map Writer 插件..."
        
        download_with_retry \
            "https://search.maven.org/remotecontent?filepath=org/mapsforge/mapsforge-map-writer/${MAPSFORGE_WRITER_VERSION}/mapsforge-map-writer-${MAPSFORGE_WRITER_VERSION}-jar-with-dependencies.jar" \
            "$writer_jar" \
            "Mapsforge Writer" || return 1
        
        log_success "Mapsforge Writer 下载完成"
    else
        log_info "Mapsforge Writer 已存在，跳过下载"
    fi
    
    # 将插件 jar 复制到 osmosis lib 目录
    local target_jar="$osmosis_dir/lib/mapsforge-map-writer-${MAPSFORGE_WRITER_VERSION}.jar"
    
    if [ ! -f "$target_jar" ]; then
        cp "$writer_jar" "$target_jar"
        log_success "Mapsforge Writer 插件已复制到 Osmosis lib 目录"
        
        # 修改 osmosis 启动脚本，添加插件到 CLASSPATH
        local osmosis_bin="$osmosis_dir/bin/osmosis"
        if [ -f "$osmosis_bin" ]; then
            # 在 CLASSPATH 行末尾添加插件 jar
            if ! grep -q "mapsforge-map-writer" "$osmosis_bin"; then
                sed -i.bak 's|^CLASSPATH=\(.*\)$|CLASSPATH=\1:$APP_HOME/lib/mapsforge-map-writer-'"${MAPSFORGE_WRITER_VERSION}"'.jar|' "$osmosis_bin"
                rm -f "${osmosis_bin}.bak"
                log_success "已将插件添加到 Osmosis CLASSPATH"
            fi
        fi
    fi
}

# ============================================================================
# 生成地图文件
# ============================================================================

generate_map_file() {
    local input_file="$TEMP_DIR/${CITY_NAME}.osm.pbf"
    local output_file="$OUTPUT_DIR/${CITY_NAME}.map"
    
    log_info "生成 Mapsforge 地图文件..."
    
    local osmosis_bin="$DOWNLOAD_DIR/osmosis/bin/osmosis"
    
    # 计算地图中心点
    local center
    center=$(get_bbox_center "$BBOX")
    log_info "地图中心点: $center"
    
    # 转换 bbox 格式：从 minLon,minLat,maxLon,maxLat 到 minLat,minLon,maxLat,maxLon
    # Mapsforge 要求纬度在前，经度在后
    local min_lon min_lat max_lon max_lat mapsforge_bbox
    IFS=',' read -r min_lon min_lat max_lon max_lat <<< "$BBOX"
    mapsforge_bbox="${min_lat},${min_lon},${max_lat},${max_lon}"
    log_info "Mapsforge bbox: $mapsforge_bbox"
    
    # 设置 Java 内存
    export JAVACMD_OPTIONS="-Xmx4G"
    
    # 使用 Osmosis 生成地图文件
    "$osmosis_bin" \
        --read-pbf file="$input_file" \
        --mapfile-writer \
        file="$output_file" \
        type=hd \
        bbox="$mapsforge_bbox" \
        map-start-position="$center" \
        map-start-zoom=12 \
        tag-conf-file="$DOWNLOAD_DIR/tag-mapping.xml" 2>/dev/null || {
            log_warn "未找到 tag-mapping.xml，使用默认配置"
            "$osmosis_bin" \
                --read-pbf file="$input_file" \
                --mapfile-writer \
                file="$output_file" \
                type=hd \
                bbox="$mapsforge_bbox" \
                map-start-position="$center" \
                map-start-zoom=12
        }
    
    if [ -f "$output_file" ]; then
        local size
        size=$(get_file_size "$output_file")
        log_success "地图文件生成完成: $output_file ($size)"
    else
        log_error "地图文件生成失败"
        return 1
    fi
}

# ============================================================================
# 创建主题文件
# ============================================================================

create_theme_file() {
    local output_file="$OUTPUT_DIR/theme.xml"
    
    if [ -f "$output_file" ]; then
        log_info "主题文件已存在，跳过创建"
        return 0
    fi
    
    log_info "创建地图主题文件..."
    
    cat > "$output_file" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<rendertheme xmlns="http://mapsforge.org/renderTheme" 
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://mapsforge.org/renderTheme https://raw.githubusercontent.com/mapsforge/mapsforge/master/resources/renderTheme.xsd"
             version="6" 
             map-background="#F8F8F8"
             map-background-outside="#DDDDDD">

    <!-- 仿高德地图配色主题 -->
    
    <!-- 水域 -->
    <rule e="way" k="natural" v="water|bay">
        <area fill="#B5D0D0" stroke="#6699CC" stroke-width="0.5"/>
    </rule>
    <rule e="way" k="waterway" v="*">
        <line stroke="#B5D0D0" stroke-width="1.5"/>
    </rule>
    
    <!-- 绿地 -->
    <rule e="way" k="landuse" v="forest|wood">
        <area fill="#C8FACC"/>
    </rule>
    <rule e="way" k="landuse" v="grass|meadow">
        <area fill="#DCEFB5"/>
    </rule>
    <rule e="way" k="leisure" v="park|garden">
        <area fill="#D0F0C0"/>
    </rule>
    
    <!-- 建筑 -->
    <rule e="way" k="building" v="*">
        <area fill="#E0E0E0" stroke="#C0C0C0" stroke-width="0.3"/>
    </rule>
    
    <!-- 道路 - 高速公路 -->
    <rule e="way" k="highway" v="motorway|motorway_link">
        <line stroke="#FFAA00" stroke-width="4" stroke-linecap="round"/>
        <line stroke="#FFCC33" stroke-width="3" stroke-linecap="round"/>
    </rule>
    
    <!-- 道路 - 国道/主干道 -->
    <rule e="way" k="highway" v="trunk|trunk_link|primary|primary_link">
        <line stroke="#FFA500" stroke-width="3" stroke-linecap="round"/>
        <line stroke="#FFCC66" stroke-width="2.5" stroke-linecap="round"/>
    </rule>
    
    <!-- 道路 - 次干道 -->
    <rule e="way" k="highway" v="secondary|secondary_link">
        <line stroke="#DDDDDD" stroke-width="2.5" stroke-linecap="round"/>
        <line stroke="#FFFFFF" stroke-width="2" stroke-linecap="round"/>
    </rule>
    
    <!-- 道路 - 一般道路 -->
    <rule e="way" k="highway" v="tertiary|tertiary_link|residential|living_street">
        <line stroke="#CCCCCC" stroke-width="2" stroke-linecap="round"/>
        <line stroke="#FFFFFF" stroke-width="1.5" stroke-linecap="round"/>
    </rule>
    
    <!-- 道路 - 小路 -->
    <rule e="way" k="highway" v="unclassified|service|footway|path">
        <line stroke="#DDDDDD" stroke-width="1" stroke-linecap="round"/>
    </rule>
    
    <!-- 铁路 -->
    <rule e="way" k="railway" v="rail">
        <line stroke="#666666" stroke-width="2" stroke-dasharray="8,4"/>
    </rule>
    
    <!-- 行政边界 -->
    <rule e="way" k="boundary" v="administrative">
        <line stroke="#FF6666" stroke-width="1" stroke-dasharray="10,5"/>
    </rule>
    
    <!-- POI 标注 - 医院 -->
    <rule e="node" k="amenity" v="hospital">
        <symbol src="file:/osm-symbols/hospital.svg" symbol-width="16"/>
        <caption k="name" font-size="10" fill="#CC0000" stroke="#FFFFFF" stroke-width="2"/>
    </rule>
    
    <!-- POI 标注 - 学校 -->
    <rule e="node" k="amenity" v="school|university|college">
        <symbol src="file:/osm-symbols/school.svg" symbol-width="14"/>
        <caption k="name" font-size="10" fill="#333399" stroke="#FFFFFF" stroke-width="2"/>
    </rule>
    
    <!-- POI 标注 - 购物 -->
    <rule e="node" k="shop" v="*">
        <caption k="name" font-size="9" fill="#666666" stroke="#FFFFFF" stroke-width="1.5"/>
    </rule>
    
    <!-- 道路名称标注 -->
    <rule e="way" k="highway" v="motorway|trunk|primary">
        <pathText k="name" font-size="12" fill="#333333" stroke="#FFFFFF" stroke-width="3"/>
    </rule>
    <rule e="way" k="highway" v="secondary|tertiary">
        <pathText k="name" font-size="10" fill="#666666" stroke="#FFFFFF" stroke-width="2"/>
    </rule>
    
</rendertheme>
EOF
    
    log_success "主题文件创建完成: $output_file"
}

# ============================================================================
# 主逻辑
# ============================================================================

FORCE=false

# 解析参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -c|--city)
            CITY_NAME="$2"
            shift 2
            ;;
        -b|--bbox)
            BBOX="$2"
            shift 2
            ;;
        -o|--output)
            OUTPUT_DIR="$2"
            shift 2
            ;;
        -f|--force)
            FORCE=true
            shift
            ;;
        -h|--help)
            usage
            ;;
        *)
            shift
            ;;
    esac
done

# 验证 BBOX
if ! [[ "$BBOX" =~ ^[0-9.]+,[0-9.]+,[0-9.]+,[0-9.]+$ ]]; then
    log_error "无效的边界框格式: $BBOX"
    exit 1
fi

main() {
    echo ""
    echo "============================================================"
    echo "  步骤 3: 生成 Mapsforge 地图文件"
    echo "============================================================"
    echo "  城市: ${CITY_NAME}"
    echo "  边界框: ${BBOX}"
    echo "  输出目录: ${OUTPUT_DIR}"
    echo "============================================================"
    echo ""
    
    # 检查 Java
    if ! command -v java &> /dev/null; then
        log_error "未找到 Java"
        log_info "请安装: brew install openjdk@17"
        exit 1
    fi
    
    # 准备目录
    prepare_directories
    
    local input_file="$TEMP_DIR/${CITY_NAME}.osm.pbf"
    local output_file="$OUTPUT_DIR/${CITY_NAME}.map"
    
    # 检查输入文件
    if [ ! -f "$input_file" ]; then
        log_error "未找到输入文件: $input_file"
        log_info "请先运行: ./02_extract_region.sh -c $CITY_NAME"
        exit 1
    fi
    
    # 强制模式下删除已有文件
    if [ "$FORCE" = true ] && [ -f "$output_file" ]; then
        log_warn "强制模式: 删除已有文件"
        rm -f "$output_file"
    fi
    
    if [ -f "$output_file" ]; then
        local size
        size=$(get_file_size "$output_file")
        log_info "地图文件已存在: $output_file ($size)"
        log_warn "如需重新生成，请使用 --force 选项"
    else
        # 下载工具
        download_mapsforge_tools || exit 1
        
        # 生成地图
        generate_map_file || exit 1
    fi
    
    # 创建主题文件
    create_theme_file
    
    echo ""
    log_success "地图生成完成！"
    echo "  地图文件: $OUTPUT_DIR/${CITY_NAME}.map"
    echo "  主题文件: $OUTPUT_DIR/theme.xml"
    echo ""
}

main
