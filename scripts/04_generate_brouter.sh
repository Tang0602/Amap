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

def get_segment_download_name(lon, lat):
    """计算下载服务器使用的分片名称（标准地理坐标）"""
    seg_lon = int(lon // 5) * 5
    seg_lat = int(lat // 5) * 5
    ew = 'E' if seg_lon >= 0 else 'W'
    ns = 'N' if seg_lat >= 0 else 'S'
    return f"{ew}{abs(seg_lon)}_{ns}{abs(seg_lat)}"

def get_segment_brouter_name(lon, lat):
    """计算 BRouter 内部使用的分片名称（偏移坐标）
    
    BRouter 内部使用从反子午线(180°)和南极(90°S)开始的偏移坐标系统：
    - lon = lonDegree - 180 - lonMod5
    - lat = latDegree - 90 - latMod5
    """
    lon_degree = int(lon)
    lat_degree = int(lat)
    lon_mod5 = lon_degree % 5
    lat_mod5 = lat_degree % 5
    
    internal_lon = lon_degree - 180 - lon_mod5
    internal_lat = lat_degree - 90 - lat_mod5
    
    slon = f"W{-internal_lon}" if internal_lon < 0 else f"E{internal_lon}"
    slat = f"S{-internal_lat}" if internal_lat < 0 else f"N{internal_lat}"
    
    return f"{slon}_{slat}"

segments = {}  # download_name -> brouter_name

# 遍历边界框覆盖的所有网格
lon = min_lon
while lon <= max_lon:
    lat = min_lat
    while lat <= max_lat:
        download_name = get_segment_download_name(lon, lat)
        brouter_name = get_segment_brouter_name(lon, lat)
        segments[download_name] = brouter_name
        lat += 5
    lon += 5

for download_name, brouter_name in sorted(segments.items()):
    print(f"{download_name}:{brouter_name}")
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
    log_info "需要下载 $segment_count 个分片"
    
    local downloaded=0
    local failed=0
    
    # 格式: download_name:brouter_name
    for segment_pair in $segments; do
        local download_name="${segment_pair%%:*}"
        local brouter_name="${segment_pair##*:}"
        
        # 使用 BRouter 内部名称保存文件
        local rd5_file="$output_dir/${brouter_name}.rd5"
        
        if [ -f "$rd5_file" ]; then
            local size
            size=$(get_file_size "$rd5_file")
            log_info "分片 ${brouter_name} 已存在 ($size)，跳过"
            ((downloaded++))
            continue
        fi
        
        log_info "下载分片: ${download_name} -> ${brouter_name}..."
        
        # 下载时使用服务器名称，保存时使用 BRouter 内部名称
        local temp_file="$output_dir/${download_name}.rd5.tmp"
        
        if download_with_retry \
            "${BROUTER_SEGMENTS_URL}/${download_name}.rd5" \
            "$temp_file" \
            "分片 ${download_name}"; then
            mv "$temp_file" "$rd5_file"
            log_info "  重命名为: ${brouter_name}.rd5"
            ((downloaded++))
        else
            rm -f "$temp_file"
            log_warn "分片 ${download_name} 下载失败（该区域可能没有数据）"
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
    log_info "已下载的分片（使用 BRouter 内部命名）:"
    ls -lh "$output_dir"/*.rd5 2>/dev/null | while read -r line; do
        echo "  $line"
    done
    
    return 0
}

# ============================================================================
# 设置路由配置文件（使用官方 BRouter profiles）
# ============================================================================

setup_profiles() {
    local profiles_dir="$OUTPUT_DIR/brouter/profiles"
    mkdir -p "$profiles_dir"
    
    log_info "设置路由配置文件..."
    
    # BRouter 源码中的官方 profiles 目录
    local brouter_profiles_src="$SCRIPT_DIR/../brouter/misc/profiles2"
    
    if [ ! -d "$brouter_profiles_src" ]; then
        log_error "找不到 BRouter 官方 profiles 目录: $brouter_profiles_src"
        log_error "请确保已克隆 BRouter 源码到 brouter/ 目录"
        return 1
    fi
    
    # 复制 lookups.dat（必需文件！）
    if [ -f "$brouter_profiles_src/lookups.dat" ]; then
        cp "$brouter_profiles_src/lookups.dat" "$profiles_dir/"
        log_info "  复制 lookups.dat（路由配置必需文件）"
    else
        log_error "找不到 lookups.dat 文件！BRouter 无法工作"
        return 1
    fi
    
    # 复制官方 profile 文件
    # 驾车：car-vario.brf
    if [ -f "$brouter_profiles_src/car-vario.brf" ]; then
        cp "$brouter_profiles_src/car-vario.brf" "$profiles_dir/"
        log_info "  复制 car-vario.brf（驾车配置）"
    fi
    
    # 骑行：trekking.brf
    if [ -f "$brouter_profiles_src/trekking.brf" ]; then
        cp "$brouter_profiles_src/trekking.brf" "$profiles_dir/"
        log_info "  复制 trekking.brf（骑行配置）"
    fi
    
    # 步行：shortest.brf
    if [ -f "$brouter_profiles_src/shortest.brf" ]; then
        cp "$brouter_profiles_src/shortest.brf" "$profiles_dir/"
        log_info "  复制 shortest.brf（步行配置）"
    fi
    
    # 可选：复制其他常用 profile
    for profile in fastbike.brf hiking-mountain.brf moped.brf; do
        if [ -f "$brouter_profiles_src/$profile" ]; then
            cp "$brouter_profiles_src/$profile" "$profiles_dir/"
            log_info "  复制 $profile"
        fi
    done
    
    log_success "路由配置文件设置完成"
    
    echo ""
    log_info "已复制的配置文件:"
    ls -lh "$profiles_dir"/ 2>/dev/null | grep -v "^total" | while read -r line; do
        echo "  $line"
    done
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

