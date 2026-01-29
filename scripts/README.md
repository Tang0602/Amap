# 离线地图数据准备脚本

本目录包含用于准备离线地图数据的脚本，可从 OpenStreetMap 数据生成 Mapsforge 地图、BRouter 路由和 POI 数据库。

## 脚本列表

| 脚本 | 功能 | 依赖 |
|------|------|------|
| `01_download_osm.sh` | 下载中国 OSM 数据 | wget/curl |
| `02_extract_region.sh` | 裁剪指定区域 | osmium-tool |
| `03_generate_map.sh` | 生成 Mapsforge 地图 | Java 11+, Osmosis |
| `04_generate_brouter.sh` | 生成 BRouter 路由数据 ⭐ | wget/curl |
| `04_generate_route.sh` | 生成 GraphHopper 路由（已弃用） | Java 11+ |
| `05_generate_poi.sh` | 生成 POI 数据库 | Python3, osmium |
| `prepare_all.sh` | 执行所有步骤 | 以上所有 |
| `common.sh` | 共享配置和工具函数 | - |
| `extract_poi.py` | POI 提取 Python 脚本 | Python3, osmium |

> ⚠️ **注意**：GraphHopper 从 2.0 版本起不再官方支持 Android 离线路由，已迁移到 **BRouter**。

## 快速开始

### 1. 安装依赖

**macOS:**
```bash
brew install osmium-tool openjdk@17 wget
pip3 install osmium
```

**Ubuntu/Debian:**
```bash
sudo apt install osmium-tool openjdk-17-jdk wget python3-pip
pip3 install osmium
```

### 2. 一键执行

```bash
cd scripts

./prepare_all.sh
```

默认生成武汉市的离线地图数据。

### 3. 自定义城市

```bash
# 北京
./prepare_all.sh -c beijing -b 115.4,39.4,117.5,41.1

# 上海
./prepare_all.sh -c shanghai -b 120.8,30.7,122.2,31.9

# 广州
./prepare_all.sh -c guangzhou -b 112.9,22.5,114.1,23.9

# 深圳
./prepare_all.sh -c shenzhen -b 113.7,22.4,114.7,22.9
```

## 分步执行（推荐）

分步执行的优势：
- 失败后只需重跑失败的步骤
- 步骤 3-5 可以并行执行
- 更容易调试

### 顺序执行

```bash
# 步骤 1: 下载 OSM 数据（约 1GB，只需下载一次）
./01_download_osm.sh

# 步骤 2: 裁剪区域
./02_extract_region.sh -c wuhan -b 113.7,29.9,115.1,31.4

# 步骤 3: 生成地图文件
./03_generate_map.sh -c wuhan -b 113.7,29.9,115.1,31.4

# 步骤 4: 生成路由数据（BRouter，推荐）
./04_generate_brouter.sh -c wuhan -b 113.7,29.9,115.1,31.4

# 步骤 5: 生成 POI 数据库
./05_generate_poi.sh -c wuhan
```

### 并行执行（节省时间）

```bash
# 先完成步骤 1-2
./01_download_osm.sh
./02_extract_region.sh -c wuhan -b 113.7,29.9,115.1,31.4

# 步骤 3-5 并行执行
./03_generate_map.sh -c wuhan -b 113.7,29.9,115.1,31.4 &
./04_generate_brouter.sh -c wuhan -b 113.7,29.9,115.1,31.4 &
./05_generate_poi.sh -c wuhan &
wait

echo "全部完成！"
```

或者使用主脚本的并行选项：

```bash
./prepare_all.sh -c wuhan --parallel
```

## 命令行选项

### 通用选项

| 选项 | 说明 | 示例 |
|------|------|------|
| `-c, --city` | 城市名称 | `-c beijing` |
| `-b, --bbox` | 边界框 (minLon,minLat,maxLon,maxLat) | `-b 115.4,39.4,117.5,41.1` |
| `-o, --output` | 输出目录 | `-o ./output` |
| `-f, --force` | 强制重新生成 | `--force` |
| `-h, --help` | 显示帮助 | `--help` |

### prepare_all.sh 专用选项

| 选项 | 说明 |
|------|------|
| `-s, --skip-download` | 跳过 OSM 下载 |
| `-p, --parallel` | 并行执行步骤 3-5 |
| `--copy` | 完成后复制到 Android 项目 |
| `--clean` | 完成后清理临时文件 |
| `--brouter` | 使用 BRouter 路由引擎（默认） |
| `--graphhopper` | 使用 GraphHopper 路由引擎（已弃用） |

## 输出文件

执行完成后，在 `map_data/` 目录下生成：

```
map_data/
├── wuhan.map        # Mapsforge 地图文件
├── brouter/         # BRouter 路由数据（推荐）
│   ├── segments/    # rd5 数据文件
│   │   ├── E110_N25.rd5
│   │   └── E110_N30.rd5
│   └── profiles/    # 路由配置文件
│       ├── car-fast.brf    # 驾车
│       ├── trekking.brf    # 骑行
│       └── shortest.brf    # 步行
├── wuhan_poi.db     # SQLite FTS5 POI 数据库
└── theme.xml        # 地图渲染主题
```

> 如果使用 `--graphhopper` 选项，则生成 `wuhan-gh/` 目录（已弃用）。

## 目录结构

```
scripts/
├── downloads/       # 下载的原始数据和工具
│   ├── china-latest.osm.pbf
│   ├── osmosis/
│   └── mapsforge-map-writer-*.jar
├── temp/            # 临时文件（裁剪后的区域数据）
│   └── wuhan.osm.pbf
└── map_data/        # 最终输出
    ├── wuhan.map
    ├── brouter/     # BRouter 路由数据
    │   ├── segments/
    │   └── profiles/
    ├── wuhan_poi.db
    └── theme.xml
```

## 复制到 Android 项目

### 自动复制

```bash
./prepare_all.sh --copy
```

### 手动复制

```bash
cp map_data/wuhan.map ../app/src/main/assets/map/
cp map_data/wuhan_poi.db ../app/src/main/assets/map/
cp -r map_data/brouter ../app/src/main/assets/map/
cp map_data/theme.xml ../app/src/main/assets/map/
```

## 常见问题

### Q: 下载 OSM 数据很慢怎么办？

可以手动下载后放到 `downloads/` 目录：
```bash
# 从镜像下载
wget -O downloads/china-latest.osm.pbf \
  https://download.geofabrik.de/asia/china-latest.osm.pbf
```

### Q: Java 内存不足？

编辑 `common.sh`，增加内存设置：
```bash
export JAVACMD_OPTIONS="-Xmx8G"  # 改为 8GB
```

### Q: 如何只重新生成某个文件？

使用 `--force` 选项：
```bash
./03_generate_map.sh -c wuhan --force  # 重新生成地图
./05_generate_poi.sh -c wuhan --force  # 重新生成 POI
```

### Q: 如何查看各脚本的帮助？

```bash
./01_download_osm.sh --help
./02_extract_region.sh --help
./03_generate_map.sh --help
./04_generate_brouter.sh --help
./05_generate_poi.sh --help
./prepare_all.sh --help
```

### Q: BRouter 数据下载失败？

BRouter rd5 数据从官方服务器下载，如果失败可以：

1. 手动下载后放到 `map_data/brouter/segments/` 目录
2. 访问 https://brouter.de/brouter/segments4/ 下载所需分片
3. 武汉市需要的分片：`E110_N25.rd5`, `E110_N30.rd5`

### Q: 为什么不再使用 GraphHopper？

GraphHopper 从 2.0 版本起官方移除了 Android 模块支持。BRouter 是专为移动端设计的路由引擎，具有以下优势：
- 内存占用更低
- 数据格式更紧凑
- 被 OsmAnd、Locus Map 等知名应用采用

## 常用城市边界框

| 城市 | 边界框 (minLon,minLat,maxLon,maxLat) |
|------|--------------------------------------|
| 武汉 | 113.7,29.9,115.1,31.4 |
| 北京 | 115.4,39.4,117.5,41.1 |
| 上海 | 120.8,30.7,122.2,31.9 |
| 广州 | 112.9,22.5,114.1,23.9 |
| 深圳 | 113.7,22.4,114.7,22.9 |
| 成都 | 103.0,30.1,104.9,31.4 |
| 杭州 | 119.1,29.7,120.8,30.6 |
| 南京 | 118.3,31.2,119.3,32.6 |
| 西安 | 108.0,33.7,109.8,34.8 |
| 重庆 | 105.3,28.1,110.2,32.2 |

## 技术说明

- **Mapsforge**: 开源离线地图渲染库，生成 `.map` 格式
- **BRouter**: 专为移动端设计的开源路由引擎，使用 `.rd5` 数据格式 ⭐
- **GraphHopper**: 开源路由引擎（已弃用，从 2.0 版本起不支持 Android）
- **SQLite FTS5**: 全文搜索扩展，用于 POI 快速检索
- **R-Tree**: 空间索引，用于附近搜索

## 相关文档

- [离线地图技术方案](../docs/OFFLINE_MAP_SOLUTION.md)
- [BRouter 迁移方案](../docs/BROUTER_MIGRATION.md)

## 许可证

脚本代码遵循项目许可证。

OSM 数据遵循 [ODbL](https://www.openstreetmap.org/copyright) 许可证。
