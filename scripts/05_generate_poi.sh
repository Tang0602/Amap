#!/bin/bash
#
# 步骤 5: 生成 POI 数据库
#
# 使用方法：
#   ./05_generate_poi.sh
#   ./05_generate_poi.sh -c beijing
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
步骤 5: 生成 POI 数据库

用法: $0 [选项]

选项:
  -c, --city NAME      城市名称 (默认: $CITY_NAME)
  -o, --output DIR     输出目录 (默认: $OUTPUT_DIR)
  -f, --force          强制重新生成
  -h, --help           显示此帮助

输出:
  \${OUTPUT_DIR}/\${CITY_NAME}_poi.db  - SQLite FTS5 POI 数据库

说明:
  使用 Python osmium 从 OSM 数据中提取 POI:
  - 餐饮、购物、交通、医疗、教育等分类
  - 支持 FTS5 全文搜索
  - 支持 R-Tree 空间索引（附近搜索）

EOF
    exit 0
}

# ============================================================================
# 生成 POI 数据库
# ============================================================================

generate_poi_database() {
    local input_file="$TEMP_DIR/${CITY_NAME}.osm.pbf"
    local output_file="$OUTPUT_DIR/${CITY_NAME}_poi.db"
    
    log_info "生成 SQLite FTS5 POI 数据库..."
    
    # 检查 Python osmium 模块
    if ! python3 -c "import osmium" 2>/dev/null; then
        log_warn "Python osmium 模块未安装，尝试安装..."
        pip3 install osmium || {
            log_error "无法安装 osmium 模块"
            log_info "请手动运行: pip3 install osmium"
            return 1
        }
    fi
    
    # 查找 POI 提取脚本
    local extract_script="$SCRIPT_DIR/extract_poi.py"
    
    if [ ! -f "$extract_script" ]; then
        log_error "未找到 POI 提取脚本: $extract_script"
        return 1
    fi
    
    # 运行 POI 提取脚本
    if ! python3 "$extract_script" \
        --input "$input_file" \
        --output "$output_file"; then
        log_error "POI 提取脚本执行失败"
        return 1
    fi
    
    if [ -f "$output_file" ]; then
        local size
        size=$(get_file_size "$output_file")
        log_success "POI 数据库生成完成: $output_file ($size)"
        return 0
    else
        log_error "POI 数据库生成失败"
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
    echo "  步骤 5: 生成 POI 数据库"
    echo "============================================================"
    echo "  城市: ${CITY_NAME}"
    echo "  输出目录: ${OUTPUT_DIR}"
    echo "============================================================"
    echo ""
    
    # 检查 Python
    if ! command -v python3 &> /dev/null; then
        log_error "未找到 Python3"
        exit 1
    fi
    
    # 准备目录
    prepare_directories
    
    local input_file="$TEMP_DIR/${CITY_NAME}.osm.pbf"
    local output_file="$OUTPUT_DIR/${CITY_NAME}_poi.db"
    
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
        log_info "POI 数据库已存在: $output_file ($size)"
        log_warn "如需重新生成，请使用 --force 选项"
        return 0
    fi
    
    # 生成 POI 数据库
    generate_poi_database || exit 1
    
    echo ""
    log_success "POI 数据库生成完成！"
    echo "  数据库文件: $output_file"
    echo ""
}

main
