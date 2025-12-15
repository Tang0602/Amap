#!/bin/bash
#
# 步骤 4: 生成 BRouter 路由数据
#
# 使用方法：
#   ./04_generate_brouter.sh
#   ./04_generate_brouter.sh -c beijing
#
# BRouter 是专为移动端设计的离线路由引擎，替代 GraphHopper
#

set -e
set -o pipefail

# 加载共享配置
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/common.sh"

# BRouter 配置
BROUTER_VERSION="1.7.5"
BROUTER_SEGMENTS_URL="https://brouter.de/brouter/segments4"

# ============================================================================
# 使用帮助
# ============================================================================

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

BRouter 优势:
  - 专为移动端设计，内存占用低
  - 数据格式紧凑，体积小
  - 被 OsmAnd、Locus Map 等应用采用
  - 支持自定义路由策略

EOF
    exit 0
}

# ============================================================================
# 计算需要下载的 rd5 分片
# ============================================================================

calculate_segments() {
    local bbox="$1"
    
    python3 << EOF
import math

bbox = "$bbox".split(',')
min_lon, min_lat = float(bbox[0]), float(bbox[1])
max_lon, max_lat = float(bbox[2]), float(bbox[3])

def get_segment(lon, lat):
    """计算给定坐标所在的 5 度网格分片名称"""
    seg_lon = int(lon // 5) * 5
    seg_lat = int(lat // 5) * 5
    ew = 'E' if seg_lon >= 0 else 'W'
    ns = 'N' if seg_lat >= 0 else 'S'
    return f"{ew}{abs(seg_lon):03d}_{ns}{abs(seg_lat):02d}"

segments = set()

# 遍历边界框覆盖的所有网格
lon = min_lon
while lon <= max_lon:
    lat = min_lat
    while lat <= max_lat:
        segments.add(get_segment(lon, lat))
        lat += 5
    lon += 5

for seg in sorted(segments):
    print(seg)
EOF
}

# ============================================================================
# 下载 rd5 分片数据
# ============================================================================

download_segments() {
    local output_dir="$OUTPUT_DIR/brouter/segments"
    mkdir -p "$output_dir"
    
    log_info "计算需要下载的数据分片..."
    local segments
    segments=$(calculate_segments "$BBOX")
    
    local segment_count
    segment_count=$(echo "$segments" | wc -l | tr -d ' ')
    log_info "需要下载 $segment_count 个分片: $(echo $segments | tr '\n' ' ')"
    
    local downloaded=0
    local failed=0
    
    for segment in $segments; do
        local rd5_file="$output_dir/${segment}.rd5"
        
        if [ -f "$rd5_file" ]; then
            local size
            size=$(get_file_size "$rd5_file")
            log_info "分片 ${segment} 已存在 ($size)，跳过"
            ((downloaded++))
            continue
        fi
        
        log_info "下载分片: ${segment}..."
        
        if download_with_retry \
            "${BROUTER_SEGMENTS_URL}/${segment}.rd5" \
            "$rd5_file" \
            "分片 ${segment}"; then
            ((downloaded++))
        else
            log_warn "分片 ${segment} 下载失败（该区域可能没有数据）"
            ((failed++))
        fi
    done
    
    if [ $downloaded -eq 0 ]; then
        log_error "没有成功下载任何分片"
        return 1
    fi
    
    log_success "分片下载完成: 成功 $downloaded, 失败 $failed"
    
    # 显示下载的文件
    echo ""
    log_info "已下载的分片:"
    ls -lh "$output_dir"/*.rd5 2>/dev/null | while read -r line; do
        echo "  $line"
    done
    
    return 0
}

# ============================================================================
# 创建路由配置文件
# ============================================================================

setup_profiles() {
    local profiles_dir="$OUTPUT_DIR/brouter/profiles"
    mkdir -p "$profiles_dir"
    
    log_info "设置路由配置文件..."
    
    # 创建驾车配置
    create_car_profile "$profiles_dir"
    
    # 创建骑行配置
    create_bike_profile "$profiles_dir"
    
    # 创建步行配置
    create_foot_profile "$profiles_dir"
    
    log_success "路由配置文件设置完成"
    
    echo ""
    log_info "已创建的配置文件:"
    ls -lh "$profiles_dir"/*.brf 2>/dev/null | while read -r line; do
        echo "  $line"
    done
}

# 创建驾车配置
create_car_profile() {
    local dir="$1"
    local file="$dir/car-fast.brf"
    
    log_info "  创建驾车配置: car-fast.brf"
    
    cat > "$file" << 'PROFILE'
# BRouter 驾车路由配置
# 优先选择快速道路

---context:global

assign processUnusedTags = false
assign turnInstructionMode = 1
assign turnInstructionCatchingRange = 40
assign turnInstructionRoundabouts = true

# 速度设置 (km/h)
assign validForCars = true

---context:way

assign turncost = 0
assign initialcost = 0

# 道路类型成本因子（越小越优先）
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
  switch highway=track          5.0
  switch highway=path           10000
  switch highway=footway        10000
  switch highway=pedestrian     10000
  switch highway=cycleway       10000
  switch highway=steps          10000
  switch route=ferry            3.0
  10000

# 禁止反向行驶的单行道
assign onewaypenalty
  switch oneway=yes         10000
  switch oneway=-1          10000
  0

---context:node

assign initialcost = 0

# 交通信号灯惩罚
assign nodeaccessgranted
  switch highway=traffic_signals 30
  0
PROFILE
}

# 创建骑行配置
create_bike_profile() {
    local dir="$1"
    local file="$dir/trekking.brf"
    
    log_info "  创建骑行配置: trekking.brf"
    
    cat > "$file" << 'PROFILE'
# BRouter 骑行路由配置
# 适合日常通勤骑行

---context:global

assign processUnusedTags = false
assign turnInstructionMode = 1
assign turnInstructionCatchingRange = 40
assign turnInstructionRoundabouts = true

assign validForBikes = true

---context:way

assign turncost = 0
assign initialcost = 0

# 道路类型成本因子
assign costfactor
  switch highway=cycleway       1
  switch highway=path           1.2
  switch highway=footway        1.5
  switch highway=pedestrian     1.5
  switch highway=living_street  1.2
  switch highway=residential    1.3
  switch highway=service        1.5
  switch highway=unclassified   1.5
  switch highway=tertiary       1.6
  switch highway=tertiary_link  1.7
  switch highway=secondary      2.0
  switch highway=secondary_link 2.2
  switch highway=primary        3.0
  switch highway=primary_link   3.5
  switch highway=trunk          10000
  switch highway=trunk_link     10000
  switch highway=motorway       10000
  switch highway=motorway_link  10000
  switch highway=steps          3.0
  switch route=ferry            5.0
  10000

---context:node

assign initialcost = 0
PROFILE
}

# 创建步行配置
create_foot_profile() {
    local dir="$1"
    local file="$dir/shortest.brf"
    
    log_info "  创建步行配置: shortest.brf"
    
    cat > "$file" << 'PROFILE'
# BRouter 步行路由配置
# 寻找最短步行路径

---context:global

assign processUnusedTags = false
assign turnInstructionMode = 1
assign turnInstructionCatchingRange = 20
assign turnInstructionRoundabouts = false

assign validForFoot = true

---context:way

assign turncost = 0
assign initialcost = 0

# 道路类型成本因子（步行优先人行道）
assign costfactor
  switch highway=footway        1
  switch highway=pedestrian     1
  switch highway=path           1
  switch highway=steps          1.2
  switch highway=living_street  1.1
  switch highway=cycleway       1.2
  switch highway=residential    1.3
  switch highway=service        1.4
  switch highway=unclassified   1.5
  switch highway=tertiary       1.8
  switch highway=tertiary_link  1.9
  switch highway=secondary      2.5
  switch highway=secondary_link 2.8
  switch highway=primary        3.5
  switch highway=primary_link   4.0
  switch highway=trunk          10000
  switch highway=trunk_link     10000
  switch highway=motorway       10000
  switch highway=motorway_link  10000
  switch route=ferry            3.0
  10000

---context:node

assign initialcost = 0
PROFILE
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
    
    local brouter_dir="$OUTPUT_DIR/brouter"
    
    # 强制模式下删除已有目录
    if [ "$FORCE" = true ] && [ -d "$brouter_dir" ]; then
        log_warn "强制模式: 删除已有目录"
        rm -rf "$brouter_dir"
    fi
    
    # 检查是否已存在
    if [ -d "$brouter_dir/segments" ] && [ -d "$brouter_dir/profiles" ]; then
        local rd5_count
        rd5_count=$(find "$brouter_dir/segments" -name "*.rd5" 2>/dev/null | wc -l | tr -d ' ')
        local brf_count
        brf_count=$(find "$brouter_dir/profiles" -name "*.brf" 2>/dev/null | wc -l | tr -d ' ')
        
        if [ "$rd5_count" -gt 0 ] && [ "$brf_count" -gt 0 ]; then
            local size
            size=$(get_file_size "$brouter_dir")
            log_info "BRouter 数据已存在: $brouter_dir ($size)"
            log_info "  - 分片数据: $rd5_count 个 rd5 文件"
            log_info "  - 配置文件: $brf_count 个 brf 文件"
            log_warn "如需重新生成，请使用 --force 选项"
            return 0
        fi
    fi
    
    # 下载分片数据
    download_segments || exit 1
    
    # 设置配置文件
    setup_profiles || exit 1
    
    # 显示结果
    local total_size
    total_size=$(get_file_size "$brouter_dir")
    
    echo ""
    log_success "BRouter 路由数据准备完成！"
    echo ""
    echo "  数据目录: $brouter_dir"
    echo "  总大小: $total_size"
    echo ""
    echo "  分片目录: $brouter_dir/segments"
    echo "  配置目录: $brouter_dir/profiles"
    echo ""
    echo "支持的路由模式:"
    echo "  - car-fast.brf   : 驾车（优先快速道路）"
    echo "  - trekking.brf   : 骑行（日常通勤）"
    echo "  - shortest.brf   : 步行（最短路径）"
    echo ""
}

main

