#!/bin/bash
#
# 离线地图数据准备 - 主脚本
# 按顺序执行所有步骤，或选择性执行部分步骤
#
# 使用方法：
#   ./prepare_all.sh                    # 执行所有步骤
#   ./prepare_all.sh -c beijing -b ...  # 指定城市
#   ./prepare_all.sh --skip-download    # 跳过下载
#   ./prepare_all.sh --parallel         # 并行生成 map/route/poi
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
离线地图数据准备 - 主脚本

用法: $0 [选项]

选项:
  -c, --city NAME      城市名称 (默认: $CITY_NAME)
  -b, --bbox BBOX      边界框 minLon,minLat,maxLon,maxLat
                       (默认: $BBOX)
  -o, --output DIR     输出目录 (默认: $OUTPUT_DIR)
  -s, --skip-download  跳过OSM下载，使用已有数据
  -p, --parallel       并行执行步骤3-5（需要足够内存）
  -f, --force          强制重新生成所有数据
  --copy               完成后复制到 Android 项目
  --clean              完成后清理临时文件
  -h, --help           显示此帮助

执行流程:
  1. 下载 OSM 数据    (01_download_osm.sh)
  2. 裁剪区域数据     (02_extract_region.sh)
  3. 生成地图文件     (03_generate_map.sh)    ┐
  4. 生成路由数据     (04_generate_route.sh)  ├─ 可并行
  5. 生成 POI 数据库  (05_generate_poi.sh)    ┘

单独执行某一步:
  ./01_download_osm.sh
  ./02_extract_region.sh -c $CITY_NAME
  ./03_generate_map.sh -c $CITY_NAME
  ./04_generate_route.sh -c $CITY_NAME
  ./05_generate_poi.sh -c $CITY_NAME

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
# 复制到 Android 项目
# ============================================================================

copy_to_android_project() {
    local android_assets_dir="$SCRIPT_DIR/../app/src/main/assets/map"
    
    echo ""
    log_info "复制数据到 Android 项目..."
    
    mkdir -p "$android_assets_dir"
    
    local copied=0
    
    if [ -f "$OUTPUT_DIR/${CITY_NAME}.map" ]; then
        cp "$OUTPUT_DIR/${CITY_NAME}.map" "$android_assets_dir/"
        log_success "已复制 ${CITY_NAME}.map"
        ((copied++))
    fi
    
    if [ -f "$OUTPUT_DIR/${CITY_NAME}_poi.db" ]; then
        cp "$OUTPUT_DIR/${CITY_NAME}_poi.db" "$android_assets_dir/"
        log_success "已复制 ${CITY_NAME}_poi.db"
        ((copied++))
    fi
    
    if [ -d "$OUTPUT_DIR/${CITY_NAME}-gh" ]; then
        cp -r "$OUTPUT_DIR/${CITY_NAME}-gh" "$android_assets_dir/"
        log_success "已复制 ${CITY_NAME}-gh/"
        ((copied++))
    fi
    
    if [ -f "$OUTPUT_DIR/theme.xml" ]; then
        cp "$OUTPUT_DIR/theme.xml" "$android_assets_dir/"
        log_success "已复制 theme.xml"
        ((copied++))
    fi
    
    if [ $copied -gt 0 ]; then
        log_success "共复制 $copied 个文件/目录到: $android_assets_dir"
    else
        log_warn "没有找到可复制的文件"
    fi
}

# ============================================================================
# 显示统计信息
# ============================================================================

show_summary() {
    echo ""
    echo "============================================================"
    echo -e "${GREEN}数据准备完成！${NC}"
    echo "============================================================"
    echo ""
    echo "城市: ${CITY_NAME}"
    echo "边界框: ${BBOX}"
    echo "输出目录: $OUTPUT_DIR"
    echo ""
    
    if [ -d "$OUTPUT_DIR" ]; then
        echo "文件列表："
        ls -lh "$OUTPUT_DIR" 2>/dev/null | tail -n +2 | while IFS= read -r line; do
            echo "  $line"
        done
        echo ""
        
        local total_size
        total_size=$(du -sh "$OUTPUT_DIR" | cut -f1)
        echo "总大小: $total_size"
    fi
    echo ""
}

# ============================================================================
# 主逻辑
# ============================================================================

SKIP_DOWNLOAD=false
PARALLEL=false
FORCE=false
DO_COPY=false
DO_CLEAN=false

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
        -s|--skip-download)
            SKIP_DOWNLOAD=true
            shift
            ;;
        -p|--parallel)
            PARALLEL=true
            shift
            ;;
        -f|--force)
            FORCE=true
            shift
            ;;
        --copy)
            DO_COPY=true
            shift
            ;;
        --clean)
            DO_CLEAN=true
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

# 构建通用参数
build_args() {
    local args="-c $CITY_NAME"
    if [ -n "$OUTPUT_DIR" ]; then
        args="$args -o $OUTPUT_DIR"
    fi
    if [ "$FORCE" = true ]; then
        args="$args -f"
    fi
    echo "$args"
}

main() {
    local start_time=$(date +%s)
    
    echo ""
    echo "============================================================"
    echo "  离线地图数据准备"
    echo "============================================================"
    show_config
    
    # 检查依赖
    check_dependencies || exit 1
    
    # 检查磁盘空间
    check_disk_space 5120 "$OUTPUT_DIR" || exit 1
    
    local common_args=$(build_args)
    
    # 步骤 1: 下载 OSM 数据
    if [ "$SKIP_DOWNLOAD" = true ]; then
        log_step "跳过步骤 1: 下载 OSM 数据"
    else
        echo ""
        log_step "步骤 1/5: 下载 OSM 数据"
        if [ "$FORCE" = true ]; then
            "$SCRIPT_DIR/01_download_osm.sh" --force
        else
            "$SCRIPT_DIR/01_download_osm.sh"
        fi
    fi
    
    # 步骤 2: 裁剪区域
    echo ""
    log_step "步骤 2/5: 裁剪区域数据"
    "$SCRIPT_DIR/02_extract_region.sh" $common_args -b "$BBOX"
    
    # 步骤 3-5: 生成数据
    if [ "$PARALLEL" = true ]; then
        echo ""
        log_step "步骤 3-5: 并行生成数据..."
        log_warn "并行模式需要较大内存 (建议 16GB+)"
        
        # 并行执行
        "$SCRIPT_DIR/03_generate_map.sh" $common_args -b "$BBOX" &
        local pid_map=$!
        
        "$SCRIPT_DIR/04_generate_route.sh" $common_args &
        local pid_route=$!
        
        "$SCRIPT_DIR/05_generate_poi.sh" $common_args &
        local pid_poi=$!
        
        # 等待所有任务完成
        local failed=0
        wait $pid_map || { log_error "地图生成失败"; ((failed++)); }
        wait $pid_route || { log_error "路由生成失败"; ((failed++)); }
        wait $pid_poi || { log_error "POI生成失败"; ((failed++)); }
        
        if [ $failed -gt 0 ]; then
            log_error "$failed 个任务失败"
            exit 1
        fi
    else
        # 顺序执行
        echo ""
        log_step "步骤 3/5: 生成地图文件"
        "$SCRIPT_DIR/03_generate_map.sh" $common_args -b "$BBOX"
        
        echo ""
        log_step "步骤 4/5: 生成路由数据"
        "$SCRIPT_DIR/04_generate_route.sh" $common_args
        
        echo ""
        log_step "步骤 5/5: 生成 POI 数据库"
        "$SCRIPT_DIR/05_generate_poi.sh" $common_args
    fi
    
    # 显示统计
    show_summary
    
    # 复制到项目
    if [ "$DO_COPY" = true ]; then
        copy_to_android_project
    else
        echo "提示: 使用 --copy 选项可自动复制到 Android 项目"
    fi
    
    # 清理临时文件
    if [ "$DO_CLEAN" = true ]; then
        echo ""
        log_info "清理临时文件..."
        rm -rf "$TEMP_DIR"
        log_success "临时文件已清理"
    else
        echo "提示: 使用 --clean 选项可清理临时文件 ($TEMP_DIR)"
    fi
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    local minutes=$((duration / 60))
    local seconds=$((duration % 60))
    
    echo ""
    echo "============================================================"
    log_success "全部完成！耗时: ${minutes}分${seconds}秒"
    echo "============================================================"
    echo ""
}

main
