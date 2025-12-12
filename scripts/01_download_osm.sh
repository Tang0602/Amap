#!/bin/bash
#
# 步骤 1: 下载 OSM 数据
#
# 使用方法：
#   ./01_download_osm.sh
#   ./01_download_osm.sh --force  # 强制重新下载
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
步骤 1: 下载 OSM 数据

用法: $0 [选项]

选项:
  -f, --force    强制重新下载（删除已有文件）
  -h, --help     显示此帮助

说明:
  从 Geofabrik 下载中国区 OSM 数据（约 1GB）
  文件保存到: $DOWNLOAD_DIR/china-latest.osm.pbf

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
    echo "  步骤 1: 下载 OSM 数据"
    echo "============================================================"
    echo ""
    
    # 准备目录
    prepare_directories
    
    local osm_file="$DOWNLOAD_DIR/china-latest.osm.pbf"
    
    # 强制模式下删除已有文件
    if [ "$FORCE" = true ] && [ -f "$osm_file" ]; then
        log_warn "强制模式: 删除已有文件"
        rm -f "$osm_file"
    fi
    
    if [ -f "$osm_file" ]; then
        local size
        size=$(get_file_size "$osm_file")
        log_info "OSM 数据文件已存在: $osm_file ($size)"
        log_warn "如需重新下载，请使用 --force 选项"
        return 0
    fi
    
    # 检查磁盘空间（需要约 2GB）
    check_disk_space 2048 "$DOWNLOAD_DIR" || exit 1
    
    log_info "下载中国 OSM 数据（约 1GB，可能需要较长时间）..."
    
    download_with_retry \
        "https://download.geofabrik.de/asia/china-latest.osm.pbf" \
        "$osm_file" \
        "中国 OSM 数据" || exit 1
    
    local size
    size=$(get_file_size "$osm_file")
    
    echo ""
    log_success "下载完成！"
    echo "  文件: $osm_file"
    echo "  大小: $size"
    echo ""
    echo "下一步: 运行 ./02_extract_region.sh 裁剪区域数据"
    echo ""
}

main
