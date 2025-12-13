#!/bin/bash
#
# 步骤 4: 生成 GraphHopper 路由数据
#
# 使用方法：
#   ./04_generate_route.sh
#   ./04_generate_route.sh -c beijing
#

set -e
set -o pipefail  # 确保管道中的错误能被捕获

# 加载共享配置
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/common.sh"

# ============================================================================
# 使用帮助
# ============================================================================

usage() {
    cat << EOF
步骤 4: 生成 GraphHopper 路由数据

用法: $0 [选项]

选项:
  -c, --city NAME      城市名称 (默认: $CITY_NAME)
  -o, --output DIR     输出目录 (默认: $OUTPUT_DIR)
  -f, --force          强制重新生成
  -h, --help           显示此帮助

输出:
  \${OUTPUT_DIR}/\${CITY_NAME}-gh/  - GraphHopper 路由数据目录

说明:
  生成的路由数据支持以下出行方式:
  - car: 驾车导航
  - bike: 骑行导航
  - foot: 步行导航

EOF
    exit 0
}

# ============================================================================
# 下载 GraphHopper
# ============================================================================

download_graphhopper() {
    local gh_dir="$DOWNLOAD_DIR/graphhopper"
    local gh_jar="$gh_dir/graphhopper-web-${GRAPHHOPPER_VERSION}.jar"
    
    if [ -f "$gh_jar" ]; then
        log_info "GraphHopper 已存在，跳过下载"
        return 0
    fi
    
    log_info "下载 GraphHopper..."
    
    mkdir -p "$gh_dir"
    
    download_with_retry \
        "https://repo1.maven.org/maven2/com/graphhopper/graphhopper-web/${GRAPHHOPPER_VERSION}/graphhopper-web-${GRAPHHOPPER_VERSION}.jar" \
        "$gh_jar" \
        "GraphHopper" || return 1
    
    log_success "GraphHopper 下载完成"
}

# ============================================================================
# 生成路由数据
# ============================================================================

generate_graphhopper_data() {
    local input_file="$TEMP_DIR/${CITY_NAME}.osm.pbf"
    local output_dir="$OUTPUT_DIR/${CITY_NAME}-gh"
    
    log_info "生成 GraphHopper 路由数据（可能需要几分钟）..."
    
    mkdir -p "$output_dir"
    
    local gh_jar="$DOWNLOAD_DIR/graphhopper/graphhopper-web-${GRAPHHOPPER_VERSION}.jar"
    
    # 创建 GraphHopper 配置文件 (GraphHopper 9.x 格式)
    local config_file="$TEMP_DIR/graphhopper-config.yml"
    cat > "$config_file" << EOF
graphhopper:
  datareader.file: $input_file
  graph.location: $output_dir
  
  # GraphHopper 9.x 必须参数：指定忽略的道路类型（空字符串表示不忽略）
  import.osm.ignored_highways: ""

  # 编码值：支持 car, bike, foot 的必要属性
  graph.encoded_values: car_access, car_average_speed, bike_access, bike_average_speed, bike_priority, foot_access, foot_average_speed, foot_priority, road_class, road_environment, max_speed, road_access, roundabout, hike_rating

  # GraphHopper 9.x: 使用内置的 custom_model 文件
  profiles:
    - name: car
      custom_model_files: [car.json]
    - name: bike
      custom_model_files: [bike.json]
    - name: foot
      custom_model_files: [foot.json]
      
  # Contraction Hierarchies 预处理 - 加速路由计算
  profiles_ch:
    - profile: car
    - profile: bike
    - profile: foot
EOF
    
    log_info "正在导入 OSM 数据..."
    
    # 使用临时文件存储输出，以便正确检查退出状态
    local log_file="$TEMP_DIR/graphhopper-import.log"
    local exit_code=0
    
    java -Xmx4G \
        -Ddw.graphhopper.datareader.file="$input_file" \
        -Ddw.graphhopper.graph.location="$output_dir" \
        -jar "$gh_jar" \
        import "$config_file" > "$log_file" 2>&1 || exit_code=$?
    
    # 显示输出日志
    if [ -f "$log_file" ]; then
        while IFS= read -r line; do
            if [[ -n "$line" && ! "$line" =~ ^[[:space:]]*$ ]]; then
                echo "  $line"
            fi
        done < "$log_file"
    fi
    
    # 检查退出状态和结果目录
    if [ $exit_code -ne 0 ]; then
        log_error "GraphHopper 导入命令失败 (退出码: $exit_code)"
        return 1
    fi
    
    if [ -d "$output_dir" ] && [ "$(ls -A "$output_dir" 2>/dev/null)" ]; then
        local size
        size=$(get_file_size "$output_dir")
        log_success "GraphHopper 路由数据生成完成: $output_dir ($size)"
        return 0
    else
        log_error "GraphHopper 路由数据生成失败：输出目录为空"
        return 1
    fi
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

main() {
    echo ""
    echo "============================================================"
    echo "  步骤 4: 生成 GraphHopper 路由数据"
    echo "============================================================"
    echo "  城市: ${CITY_NAME}"
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
    local output_dir="$OUTPUT_DIR/${CITY_NAME}-gh"
    
    # 检查输入文件
    if [ ! -f "$input_file" ]; then
        log_error "未找到输入文件: $input_file"
        log_info "请先运行: ./02_extract_region.sh -c $CITY_NAME"
        exit 1
    fi
    
    # 强制模式下删除已有目录
    if [ "$FORCE" = true ] && [ -d "$output_dir" ]; then
        log_warn "强制模式: 删除已有目录"
        rm -rf "$output_dir"
    fi
    
    if [ -d "$output_dir" ] && [ "$(ls -A "$output_dir" 2>/dev/null)" ]; then
        local size
        size=$(get_file_size "$output_dir")
        log_info "路由数据已存在: $output_dir ($size)"
        log_warn "如需重新生成，请使用 --force 选项"
        return 0
    fi
    
    # 下载 GraphHopper
    download_graphhopper || exit 1
    
    # 生成路由数据
    generate_graphhopper_data || exit 1
    
    echo ""
    log_success "路由数据生成完成！"
    echo "  路由目录: $output_dir"
    echo "  支持模式: car, bike, foot"
    echo ""
}

main
