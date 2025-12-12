#!/bin/bash
#
# 步骤 2: 裁剪区域数据
#
# 使用方法：
#   ./02_extract_region.sh
#   ./02_extract_region.sh -c beijing -b 115.4,39.4,117.5,41.1
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
步骤 2: 裁剪区域数据

用法: $0 [选项]

选项:
  -c, --city NAME      城市名称 (默认: $CITY_NAME)
  -b, --bbox BBOX      边界框 minLon,minLat,maxLon,maxLat
                       (默认: $BBOX)
  -f, --force          强制重新裁剪
  -h, --help           显示此帮助

常用城市边界框:
  武汉: 113.7,29.9,115.1,31.4
  北京: 115.4,39.4,117.5,41.1
  上海: 120.8,30.7,122.2,31.9
  广州: 112.9,22.5,114.1,23.9
  深圳: 113.7,22.4,114.7,22.9

EOF
    exit 0
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
    echo "  步骤 2: 裁剪区域数据"
    echo "============================================================"
    echo "  城市: ${CITY_NAME}"
    echo "  边界框: ${BBOX}"
    echo "============================================================"
    echo ""
    
    # 检查 osmium
    if ! command -v osmium &> /dev/null; then
        log_error "未找到 osmium 工具"
        log_info "请安装: brew install osmium-tool 或 apt install osmium-tool"
        exit 1
    fi
    
    # 准备目录
    prepare_directories
    
    local input_file="$DOWNLOAD_DIR/china-latest.osm.pbf"
    local output_file="$TEMP_DIR/${CITY_NAME}.osm.pbf"
    
    # 检查输入文件
    if [ ! -f "$input_file" ]; then
        log_error "未找到输入文件: $input_file"
        log_info "请先运行: ./01_download_osm.sh"
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
        log_info "区域数据已存在: $output_file ($size)"
        log_warn "如需重新裁剪，请使用 --force 选项"
        return 0
    fi
    
    log_info "裁剪 ${CITY_NAME} 区域数据..."
    
    osmium extract \
        --bbox "$BBOX" \
        --strategy=smart \
        --output="$output_file" \
        "$input_file"
    
    local size
    size=$(get_file_size "$output_file")
    
    echo ""
    log_success "裁剪完成！"
    echo "  文件: $output_file"
    echo "  大小: $size"
    echo ""
    echo "下一步可以并行运行:"
    echo "  ./03_generate_map.sh    # 生成地图文件"
    echo "  ./04_generate_route.sh  # 生成路由数据"
    echo "  ./05_generate_poi.sh    # 生成POI数据库"
    echo ""
}

main
