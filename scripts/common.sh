#!/bin/bash
#
# 共享配置和工具函数
# 被其他脚本 source 引用
#

# ============================================================================
# 默认配置参数
# ============================================================================

# 脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 武汉市边界框 (minLon,minLat,maxLon,maxLat)
: "${BBOX:=113.7,29.9,115.1,31.4}"

# 城市名称（用于文件命名）
: "${CITY_NAME:=wuhan}"

# 数据输出目录
: "${OUTPUT_DIR:=$SCRIPT_DIR/map_data}"

# 临时文件目录
: "${TEMP_DIR:=$SCRIPT_DIR/temp}"

# 下载的源数据目录
: "${DOWNLOAD_DIR:=$SCRIPT_DIR/downloads}"

# Mapsforge writer 插件版本
MAPSFORGE_WRITER_VERSION="0.21.0"

# GraphHopper 版本
GRAPHHOPPER_VERSION="9.1"

# 下载重试次数
MAX_RETRIES=3

# Java 内存设置（可根据机器配置调整）
: "${JAVACMD_OPTIONS:=-Xmx4G}"
export JAVACMD_OPTIONS

# ============================================================================
# 颜色输出
# ============================================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_step() {
    echo -e "${CYAN}[STEP]${NC} $1"
}

# ============================================================================
# 通用工具函数
# ============================================================================

# 显示当前配置
show_config() {
    echo "============================================================"
    echo "  当前配置"
    echo "============================================================"
    echo "  城市名称: ${CITY_NAME}"
    echo "  边界框: ${BBOX}"
    echo "  输出目录: ${OUTPUT_DIR}"
    echo "  临时目录: ${TEMP_DIR}"
    echo "  下载目录: ${DOWNLOAD_DIR}"
    echo "============================================================"
    echo ""
}

# 检查依赖
check_dependencies() {
    log_info "检查依赖工具..."
    
    local missing_deps=()
    
    # 检查 osmium
    if ! command -v osmium &> /dev/null; then
        missing_deps+=("osmium-tool (brew install osmium-tool 或 apt install osmium-tool)")
    fi
    
    # 检查 Java
    if ! command -v java &> /dev/null; then
        missing_deps+=("Java 11+ (brew install openjdk@17 或 apt install openjdk-17-jdk)")
    else
        local java_version
        java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [[ "$java_version" -lt 11 ]]; then
            missing_deps+=("Java 11+ (当前版本: $java_version)")
        fi
    fi
    
    # 检查 Python
    if ! command -v python3 &> /dev/null; then
        missing_deps+=("Python3")
    fi
    
    # 检查 wget 或 curl
    if ! command -v wget &> /dev/null && ! command -v curl &> /dev/null; then
        missing_deps+=("wget 或 curl")
    fi
    
    if [ ${#missing_deps[@]} -ne 0 ]; then
        log_error "缺少以下依赖："
        for dep in "${missing_deps[@]}"; do
            echo "  - $dep"
        done
        return 1
    fi
    
    log_success "所有依赖检查通过"
    return 0
}

# 检查磁盘空间
check_disk_space() {
    local required_mb=$1
    local target_dir=$2
    
    mkdir -p "$target_dir"
    
    local available_mb
    if [[ "$OSTYPE" == "darwin"* ]]; then
        available_mb=$(df -m "$target_dir" | tail -1 | awk '{print $4}')
    else
        available_mb=$(df -m "$target_dir" | tail -1 | awk '{print $4}')
    fi
    
    if [ "$available_mb" -lt "$required_mb" ]; then
        log_error "磁盘空间不足: 需要 ${required_mb}MB，可用 ${available_mb}MB"
        return 1
    fi
    
    log_info "磁盘空间检查通过: 可用 ${available_mb}MB (需要 ${required_mb}MB)"
    return 0
}

# 准备目录
prepare_directories() {
    log_info "准备目录结构..."
    mkdir -p "$OUTPUT_DIR"
    mkdir -p "$TEMP_DIR"
    mkdir -p "$DOWNLOAD_DIR"
    log_success "目录结构准备完成"
}

# 带重试的下载函数
download_with_retry() {
    local url="$1"
    local output="$2"
    local description="$3"
    local retry=0
    
    while [ $retry -lt $MAX_RETRIES ]; do
        log_info "下载 $description (尝试 $((retry+1))/$MAX_RETRIES)..."
        
        if command -v wget &> /dev/null; then
            if wget -c --timeout=60 --tries=1 -O "$output" "$url" 2>/dev/null; then
                local size
                if [[ "$OSTYPE" == "darwin"* ]]; then
                    size=$(stat -f%z "$output" 2>/dev/null || echo 0)
                else
                    size=$(stat -c%s "$output" 2>/dev/null || echo 0)
                fi
                
                if [ "$size" -gt 1000 ]; then
                    log_success "$description 下载完成"
                    return 0
                fi
            fi
        elif command -v curl &> /dev/null; then
            if curl -L -C - --connect-timeout 60 -o "$output" "$url" 2>/dev/null; then
                local size
                if [[ "$OSTYPE" == "darwin"* ]]; then
                    size=$(stat -f%z "$output" 2>/dev/null || echo 0)
                else
                    size=$(stat -c%s "$output" 2>/dev/null || echo 0)
                fi
                
                if [ "$size" -gt 1000 ]; then
                    log_success "$description 下载完成"
                    return 0
                fi
            fi
        fi
        
        ((retry++))
        if [ $retry -lt $MAX_RETRIES ]; then
            log_warn "下载失败，${retry}秒后重试..."
            sleep $retry
        fi
    done
    
    log_error "下载失败: $description"
    return 1
}

# 计算边界框中心点
get_bbox_center() {
    local bbox="$1"
    local min_lon min_lat max_lon max_lat
    
    IFS=',' read -r min_lon min_lat max_lon max_lat <<< "$bbox"
    
    local center_lat center_lon
    if command -v bc &> /dev/null; then
        center_lon=$(echo "scale=6; ($min_lon + $max_lon) / 2" | bc)
        center_lat=$(echo "scale=6; ($min_lat + $max_lat) / 2" | bc)
    else
        # 使用 Python 计算
        read -r center_lat center_lon <<< $(python3 -c "
bbox = '$bbox'.split(',')
center_lon = (float(bbox[0]) + float(bbox[2])) / 2
center_lat = (float(bbox[1]) + float(bbox[3])) / 2
print(f'{center_lat} {center_lon}')
")
    fi
    
    echo "$center_lat,$center_lon"
}

# 解析通用命令行参数
parse_common_args() {
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
            -h|--help)
                return 1  # 调用方应显示帮助
                ;;
            *)
                shift
                ;;
        esac
    done
    
    # 验证 BBOX 格式
    if ! [[ "$BBOX" =~ ^[0-9.]+,[0-9.]+,[0-9.]+,[0-9.]+$ ]]; then
        log_error "无效的边界框格式: $BBOX"
        log_info "正确格式: minLon,minLat,maxLon,maxLat (例如: 113.7,29.9,115.1,31.4)"
        return 1
    fi
    
    return 0
}

# 获取文件大小（人类可读）
get_file_size() {
    local file="$1"
    if [ -f "$file" ]; then
        du -h "$file" | cut -f1
    elif [ -d "$file" ]; then
        du -sh "$file" | cut -f1
    else
        echo "N/A"
    fi
}
