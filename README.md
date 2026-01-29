# Amap-Sim (高德地图仿真应用)

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/License-未指定-lightgrey.svg)]()

**纯离线运行的 Android 地图应用**，仿真高德地图的核心功能。专为 AI 自动化测试、离线演示和教学研究场景设计。

## 核心特性

- **100% 纯离线** - 无需任何网络连接，完全离线运行
- **仿高德 UI** - 采用 Overlay 架构，提供流畅的用户体验
- **离线地图** - 基于 Mapsforge 的矢量地图渲染
- **离线路由** - 使用 BRouter 引擎的多模式路径规划（驾车/骑行/步行）
- **离线搜索** - SQLite FTS5 全文搜索，支持模糊匹配和附近搜索
- **实时导航** - 转向指导、距离提示、路线跟随
- **现代化架构** - MVVM + Clean Architecture + Jetpack Compose
- **开源数据** - 基于 OpenStreetMap 数据，可自由使用

## 技术栈

### 核心框架
- **Kotlin 2.0.21** - 100% Kotlin 实现
- **Jetpack Compose** - 声明式 UI，Material Design 3
- **MVVM + Clean Architecture** - 清晰的三层架构
- **Kotlin Coroutines** - 异步处理和并发管理

### 地图技术
- **Mapsforge 0.21.0** - 离线矢量地图渲染
- **BRouter 1.7.8** - 高性能离线路由引擎
- **SQLite FTS5** - 全文搜索和空间索引
- **OpenStreetMap** - 开源地图数据

### 开发工具
- **Android SDK 24-35** (Android 7.0 - Android 15)
- **JDK 17** - Java 目标版本
- **Gradle 8.13.2** - 构建系统

## 快速开始

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 24-35
- 至少 200MB 可用空间

### 克隆和构建

```bash
# 1. 克隆仓库
git clone <repository-url>
cd Amap

# 2. 准备离线数据（首次运行必需）
cd scripts
./prepare_all.sh --copy  # 自动生成武汉市数据并复制到 assets 目录

# 3. 返回项目根目录并构建
cd ..
./gradlew assembleDebug

# 4. 安装到设备
./gradlew installDebug
```

### 首次启动

首次启动应用时，会自动解压 assets 中的离线数据到内部存储，需要 10-30 秒。之后的启动将直接进入地图页面。

## 项目结构

```
Amap/
├── app/src/main/
│   ├── assets/map/              # 离线数据（70-120MB）
│   │   ├── wuhan.map            # Mapsforge 地图文件
│   │   ├── wuhan_poi.db         # POI 数据库
│   │   ├── theme.xml            # 地图渲染主题
│   │   └── brouter/             # BRouter 路由数据
│   │       ├── segments/        # 路由数据分片（.rd5）
│   │       └── profiles/        # 路由配置文件
│   │
│   └── java/com/example/amap_sim/
│       ├── data/                # 数据层
│       │   └── local/
│       │       ├── BRouterService.kt           # 路由服务
│       │       ├── OfflineSearchService.kt     # 搜索服务
│       │       └── OfflineDataManager.kt       # 数据管理
│       │
│       ├── domain/              # 领域层
│       │   └── model/           # 数据模型
│       │
│       ├── ui/                  # UI 层
│       │   ├── components/      # 可复用组件
│       │   │   └── map/MapsforgeMapView.kt    # 地图组件
│       │   ├── screen/
│       │   │   ├── mapcontainer/              # 地图容器（Overlay 架构）
│       │   │   │   ├── MapContainerScreen.kt
│       │   │   │   ├── MapContainerViewModel.kt
│       │   │   │   └── overlay/               # 各功能 Overlay
│       │   │   │       ├── home/              # 主页
│       │   │   │       ├── search/            # 搜索
│       │   │   │       ├── detail/            # POI 详情
│       │   │   │       └── route/             # 路线规划
│       │   │   └── navigation/                # 导航页面
│       │   └── theme/           # Material Design 主题
│       │
│       └── di/                  # 依赖注入
│           └── ServiceLocator.kt
│
├── docs/                        # 详细技术文档
│   ├── OFFLINE_MAP_SOLUTION.md  # 技术方案详解（900+ 行）
│   └── BROUTER_MIGRATION.md     # BRouter 集成指南（1000+ 行）
│
└── scripts/                     # 数据准备脚本
    ├── prepare_all.sh           # 一键生成所有数据
    ├── 01_download_osm.sh       # 下载 OSM 数据
    ├── 02_extract_region.sh     # 裁剪区域
    ├── 03_generate_map.sh       # 生成 Mapsforge 地图
    ├── 04_generate_brouter.sh   # 生成 BRouter 路由数据
    └── 05_generate_poi.sh       # 生成 POI 数据库
```

## 功能介绍

### 地图渲染
- 基于 Mapsforge 的离线矢量地图
- 支持缩放、平移、旋转
- 自定义渲染主题
- 标记点（起点、终点、POI）显示
- 路线绘制（折线）

### POI 搜索
- **关键词搜索** - 支持模糊匹配和全文搜索
- **附近搜索** - 基于当前位置搜索周边 POI
- **分类搜索** - 支持 14+ 大类（餐饮、购物、交通等）
- **热门推荐** - 预设热门 POI 列表

### 路径规划
- **多种交通方式** - 驾车、骑行、步行
- **途经点支持** - 多点路径规划
- **详细路线信息** - 总距离、预计时间、详细指令
- **路线优化** - 基于 BRouter profile 自动优化

### 实时导航
- **转向指令** - 当前指令和下一个指令
- **距离提示** - 剩余距离和剩余时间
- **路线跟随** - 地图自动跟随路线移动
- **导航控制** - 暂停、继续、退出

## 离线数据准备

### 数据文件说明

| 文件 | 格式 | 用途 | 体积（武汉市） |
|------|------|------|---------------|
| `wuhan.map` | Mapsforge | 地图渲染 | ~30-50 MB |
| `brouter/segments/*.rd5` | BRouter | 路由规划 | ~20-40 MB |
| `wuhan_poi.db` | SQLite FTS5 | POI 搜索 | ~5-10 MB |
| `theme.xml` | XML | 渲染主题 | ~50 KB |
| **总计** | | | **~70-120 MB** |

所有数据均来自 **OpenStreetMap (OSM)**，开源免费，可自由使用。

### 生成其他城市数据

```bash
cd scripts

# 生成北京市数据
./prepare_all.sh -c beijing -b 115.4,39.4,117.5,41.1

# 生成上海市数据
./prepare_all.sh -c shanghai -b 120.8,30.7,122.0,31.6

# 生成广州市数据
./prepare_all.sh -c guangzhou -b 112.9,22.6,114.0,23.6

# 查看完整使用说明
./prepare_all.sh --help
```

### 前置依赖

数据准备脚本需要以下工具：
- **osmium-tool** - OSM 数据处理
- **Java 17+** - 运行 Osmosis 和 Mapsforge Writer
- **Python 3 + osmium** - POI 提取
- **wget/curl** - 数据下载

详细说明请查看 `scripts/README.md`。

## 架构设计

### MVVM + Clean Architecture

```
┌─────────────────────────────────────────────┐
│              UI Layer                        │
│  Screen ◄─── ViewModel ◄─── StateFlow       │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────┴───────────────────────────┐
│           Domain Layer                       │
│  Models (LatLng, RouteResult, PoiResult)    │
│  Repository Interfaces (依赖倒置)            │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────┴───────────────────────────┐
│             Data Layer                       │
│  Repository Implementations                  │
│  Local Data Sources (BRouter, Search, etc)  │
└─────────────────────────────────────────────┘
```

### Overlay 架构

采用与高德地图一致的 **Overlay 架构**，提供流畅的用户体验：

```
地图容器 (MapContainer) ← 主入口
    │
    ├─ 主页 Overlay (Home)
    ├─ 搜索 Overlay (Search)
    ├─ POI 详情 Overlay (Detail)
    └─ 路线规划 Overlay (RoutePlanning)
         ↓ 开始导航
    导航页 (Navigation) ← 独立页面
```

**Overlay 模式优势**：
- 共享一个地图实例，内存优化
- 状态连续保持（缩放、位置）
- 页面切换流畅自然
- 始终可与地图交互

### MapStateController 接口

业务 ViewModel 通过统一接口控制地图：

```kotlin
interface MapStateController {
    fun moveTo(position: LatLng, zoomLevel: Int?)
    fun setMarkers(markers: List<MarkerData>)
    fun setRoute(route: RouteResult)
    fun clearRoute()
    fun fitBounds(...)
    // ...
}
```

**好处**：依赖倒置、易于测试、便于扩展。

## 技术亮点

### 1. 100% 纯离线
- 无需任何网络权限
- 适合内网、隔离环境
- AI 自动化测试友好

### 2. 无 Native 依赖
- 纯 Java/Kotlin 实现
- 支持所有架构（ARM、x86、模拟器）
- 无需 NDK 编译

### 3. 数据紧凑高效
- 武汉市数据仅 70-120MB
- BRouter 比 GraphHopper 节省 30-50% 存储
- 启动速度快

### 4. 高度可定制
- 地图渲染主题（XML）
- BRouter 路由策略（.brf 脚本）
- POI 分类和数据源

### 5. 完整的数据准备工具链
- 自动化脚本
- 支持任意城市
- 从 OSM 数据到应用数据全流程

## 相关文档

项目包含详尽的技术文档：

- **[OFFLINE_MAP_SOLUTION.md](docs/OFFLINE_MAP_SOLUTION.md)** (900+ 行)
  - 技术方案选型详解
  - Mapsforge、BRouter、GraphHopper 对比
  - Overlay 架构实现
  - 数据格式和工具链

- **[BROUTER_MIGRATION.md](docs/BROUTER_MIGRATION.md)** (1000+ 行)
  - 从 GraphHopper 迁移到 BRouter 的完整指南
  - BRouter 数据生成脚本
  - Android 集成步骤
  - 代码适配示例

- **[scripts/README.md](scripts/README.md)** (275 行)
  - 数据准备脚本使用说明
  - 常用城市边界框
  - 故障排查

## 已知限制

- 当前仅包含武汉市数据，需要其他城市数据需重新生成
- 首次启动需解压数据（10-30秒）
- 需要足够的内部存储空间（至少 150MB）
- 部分功能页面使用"开发中"占位

## 贡献指南

欢迎贡献！以下是一些建议方向：

1. **数据准备** - 为更多城市生成离线数据
2. **功能完善** - 实现标记为"开发中"的功能
3. **UI 美化** - 优化地图主题和 UI 组件
4. **性能优化** - 减少启动时间、内存占用
5. **测试覆盖** - 添加单元测试、集成测试
6. **文档翻译** - 将文档翻译为英文

## 许可证

**注意**：
- OSM 数据遵循 [ODbL 许可证](https://opendatacommons.org/licenses/odbl/)
- Mapsforge、BRouter 均为开源项目
- 本项目许可证待定，请确保遵守各依赖库的许可证

## 致谢

感谢以下开源项目：
- [OpenStreetMap](https://www.openstreetmap.org/) - 开源地图数据
- [Mapsforge](https://github.com/mapsforge/mapsforge) - 离线地图渲染
- [BRouter](https://github.com/abrensch/brouter) - 离线路由引擎
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - 现代化 UI 框架

---

**Amap-Sim** - 让离线地图应用开发更简单
