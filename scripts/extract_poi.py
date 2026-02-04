#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
POI 提取脚本
从 OpenStreetMap PBF 文件中提取 POI 数据，并存入 SQLite FTS5 数据库

使用方法：
    python3 extract_poi.py --input wuhan.osm.pbf --output wuhan_poi.db

依赖：
    pip install osmium
"""

import argparse
import sqlite3
import sys
import os
from typing import Optional, Dict, List, Tuple
from datetime import datetime

try:
    import osmium
    from osmium import geom
except ImportError:
    print("错误: 请先安装 osmium 模块")
    print("  pip install osmium")
    sys.exit(1)


# ============================================================================
# POI 分类映射表
# 将 OSM 标签映射到中文分类
# ============================================================================

POI_CATEGORIES = {
    # 餐饮
    "amenity=restaurant": "餐饮|餐厅",
    "amenity=fast_food": "餐饮|快餐",
    "amenity=cafe": "餐饮|咖啡厅",
    "amenity=bar": "餐饮|酒吧",
    "amenity=pub": "餐饮|酒吧",
    "amenity=food_court": "餐饮|美食城",
    
    # 购物
    "shop=supermarket": "购物|超市",
    "shop=convenience": "购物|便利店",
    "shop=mall": "购物|商场",
    "shop=department_store": "购物|百货",
    "shop=clothes": "购物|服装",
    "shop=electronics": "购物|电子产品",
    "shop=mobile_phone": "购物|手机店",
    "shop=bakery": "购物|烘焙店",
    "shop=butcher": "购物|肉店",
    "shop=greengrocer": "购物|果蔬店",
    "shop=pharmacy": "购物|药店",
    "shop=books": "购物|书店",
    "shop=furniture": "购物|家具店",
    "shop=hardware": "购物|五金店",
    "shop=*": "购物|商店",
    
    # 交通
    "highway=bus_stop": "交通|公交站",
    "railway=station": "交通|火车站",
    "railway=subway_entrance": "交通|地铁站",
    "station=subway": "交通|地铁站",
    "public_transport=station": "交通|公交站",
    "aeroway=aerodrome": "交通|机场",
    "aeroway=terminal": "交通|航站楼",
    "amenity=bus_station": "交通|汽车站",
    "amenity=ferry_terminal": "交通|渡口",
    "amenity=parking": "交通|停车场",
    "amenity=fuel": "交通|加油站",
    "amenity=charging_station": "交通|充电站",
    "amenity=bicycle_rental": "交通|自行车租赁",
    "amenity=taxi": "交通|出租车站",
    
    # 住宿
    "tourism=hotel": "住宿|酒店",
    "tourism=motel": "住宿|汽车旅馆",
    "tourism=guest_house": "住宿|民宿",
    "tourism=hostel": "住宿|青年旅社",
    
    # 医疗
    "amenity=hospital": "医疗|医院",
    "amenity=clinic": "医疗|诊所",
    "amenity=pharmacy": "医疗|药店",
    "amenity=dentist": "医疗|牙科",
    "amenity=doctors": "医疗|诊所",
    "amenity=veterinary": "医疗|宠物医院",
    
    # 教育
    "amenity=school": "教育|学校",
    "amenity=university": "教育|大学",
    "amenity=college": "教育|学院",
    "amenity=kindergarten": "教育|幼儿园",
    "amenity=library": "教育|图书馆",
    
    # 金融
    "amenity=bank": "金融|银行",
    "amenity=atm": "金融|ATM",
    
    # 政务
    "amenity=townhall": "政务|政府机关",
    "amenity=police": "政务|公安局",
    "amenity=fire_station": "政务|消防站",
    "amenity=post_office": "政务|邮局",
    "amenity=courthouse": "政务|法院",
    
    # 休闲娱乐
    "leisure=park": "休闲|公园",
    "leisure=playground": "休闲|游乐场",
    "leisure=sports_centre": "休闲|体育中心",
    "leisure=stadium": "休闲|体育场",
    "leisure=swimming_pool": "休闲|游泳馆",
    "leisure=fitness_centre": "休闲|健身房",
    "amenity=cinema": "休闲|电影院",
    "amenity=theatre": "休闲|剧院",
    "amenity=nightclub": "休闲|夜店",
    "amenity=casino": "休闲|赌场",
    
    # 旅游景点
    "tourism=attraction": "景点|旅游景点",
    "tourism=museum": "景点|博物馆",
    "tourism=gallery": "景点|美术馆",
    "tourism=zoo": "景点|动物园",
    "tourism=theme_park": "景点|主题公园",
    "tourism=viewpoint": "景点|观景点",
    "historic=monument": "景点|纪念碑",
    "historic=memorial": "景点|纪念馆",
    "historic=castle": "景点|城堡",
    "historic=ruins": "景点|遗址",
    
    # 宗教场所
    "amenity=place_of_worship": "宗教|宗教场所",
    
    # 生活服务
    "amenity=toilets": "生活服务|公共厕所",
    "amenity=drinking_water": "生活服务|饮水点",
    "amenity=recycling": "生活服务|回收站",
    "amenity=car_wash": "生活服务|洗车",
    "amenity=laundry": "生活服务|洗衣店",
    "shop=hairdresser": "生活服务|理发店",
    "shop=beauty": "生活服务|美容店",
    
    # 住宅/小区
    "landuse=residential": "住宅|居民区",
    "building=apartments": "住宅|公寓楼",
    "building=residential": "住宅|住宅楼",
    
    # 办公/公司
    "office=company": "办公|公司",
    "office=government": "政务|政府机关",
    "building=office": "办公|办公楼",
    "building=commercial": "办公|商业楼",
}


class POIHandler(osmium.SimpleHandler):
    """
    OSM POI 数据处理器
    继承 osmium.SimpleHandler 来处理 OSM 数据
    """
    
    def __init__(self, db_conn: sqlite3.Connection = None):
        super().__init__()
        self.db_conn = db_conn
        self.pois: List[Dict] = []
        self.node_count = 0
        self.way_count = 0
        self.relation_count = 0
        self.poi_count = 0
        self.batch_size = 1000
        # 用于计算 way 中心点
        self.wkb_factory = geom.WKBFactory()
    
    def _flush_pois(self):
        """批量写入 POI 到数据库，避免内存溢出"""
        if self.db_conn and len(self.pois) >= self.batch_size:
            insert_pois_batch(self.db_conn, self.pois)
            self.pois = []
    
    def _get_category(self, tags: Dict[str, str]) -> Optional[str]:
        """
        根据 OSM 标签获取 POI 分类
        """
        # 按优先级检查标签
        for tag_key, tag_value in tags.items():
            # 精确匹配
            key = f"{tag_key}={tag_value}"
            if key in POI_CATEGORIES:
                return POI_CATEGORIES[key]
            
            # 通配符匹配
            wildcard_key = f"{tag_key}=*"
            if wildcard_key in POI_CATEGORIES:
                return POI_CATEGORIES[wildcard_key]
        
        return None
    
    def _extract_poi_info(self, osm_id: int, tags: Dict[str, str], 
                          lat: float, lon: float, obj_type: str) -> Optional[Dict]:
        """
        从 OSM 对象中提取 POI 信息
        """
        # 获取名称，按优先级尝试：name > name:zh > name:en
        name = None
        for name_key in ['name', 'name:zh', 'name:en']:
            val = tags.get(name_key)
            if val and val.strip():
                name = val.strip()
                break
        
        # 没有名称的不处理
        if not name:
            return None
        
        # 获取分类
        category = self._get_category(tags)
        if not category:
            return None
        
        # 解析分类
        category_parts = category.split('|')
        main_category = category_parts[0] if len(category_parts) > 0 else "其他"
        sub_category = category_parts[1] if len(category_parts) > 1 else ""
        
        # 提取地址信息
        address_parts = []
        if tags.get('addr:province'):
            address_parts.append(tags.get('addr:province'))
        if tags.get('addr:city'):
            address_parts.append(tags.get('addr:city'))
        if tags.get('addr:district'):
            address_parts.append(tags.get('addr:district'))
        if tags.get('addr:street'):
            address_parts.append(tags.get('addr:street'))
        if tags.get('addr:housenumber'):
            address_parts.append(tags.get('addr:housenumber') + '号')
        
        address = ''.join(address_parts) if address_parts else None
        
        # 提取其他有用信息
        phone = tags.get('phone') or tags.get('contact:phone')
        website = tags.get('website') or tags.get('contact:website')
        opening_hours = tags.get('opening_hours')
        description = tags.get('description')

        # 提取评分信息（从OSM标签）
        rating = None
        if tags.get('stars'):
            try:
                rating = float(tags.get('stars'))
            except:
                pass
        elif tags.get('rating'):
            try:
                rating = float(tags.get('rating'))
            except:
                pass

        return {
            'osm_id': osm_id,
            'osm_type': obj_type,
            'name': name,
            'name_en': tags.get('name:en', ''),
            'main_category': main_category,
            'sub_category': sub_category,
            'lat': lat,
            'lon': lon,
            'address': address,
            'phone': phone,
            'website': website,
            'opening_hours': opening_hours,
            'description': description,
            'rating': rating,
            'tags': str(dict(tags))[:500],  # 保存原始标签（限制长度）
        }
    
    def node(self, n):
        """处理节点"""
        self.node_count += 1
        
        if self.node_count % 100000 == 0:
            print(f"  已处理 {self.node_count} 个节点, {self.way_count} 条路径, 提取 {self.poi_count} 个 POI")
        
        tags = dict(n.tags)
        if not tags:
            return
        
        try:
            poi = self._extract_poi_info(
                osm_id=n.id,
                tags=tags,
                lat=n.location.lat,
                lon=n.location.lon,
                obj_type='node'
            )
            
            if poi:
                self.pois.append(poi)
                self.poi_count += 1
                self._flush_pois()
        except Exception:
            pass  # 跳过无效节点
    
    def way(self, w):
        """处理路径（用于面状 POI，如公园、商场等）"""
        self.way_count += 1
        
        tags = dict(w.tags)
        if not tags:
            return
        
        # 先检查是否是 POI 分类
        category = self._get_category(tags)
        if not category:
            return
        
        # 计算中心点：取所有节点坐标的平均值
        try:
            lats = []
            lons = []
            for node in w.nodes:
                if node.location.valid():
                    lats.append(node.location.lat)
                    lons.append(node.location.lon)
            
            if not lats or not lons:
                return
            
            center_lat = sum(lats) / len(lats)
            center_lon = sum(lons) / len(lons)
            
            poi = self._extract_poi_info(
                osm_id=w.id,
                tags=tags,
                lat=center_lat,
                lon=center_lon,
                obj_type='way'
            )
            
            if poi:
                self.pois.append(poi)
                self.poi_count += 1
                self._flush_pois()
        except Exception:
            pass  # 跳过无效的 way
    
    def area(self, a):
        """处理面（封闭的 way 或 relation 形成的区域）"""
        tags = dict(a.tags)
        if not tags:
            return
        
        category = self._get_category(tags)
        if not category:
            return
        
        try:
            # 使用 osmium 的几何工厂计算中心点
            # 对于 area，尝试获取外环的节点来计算中心
            if a.from_way():
                # 这是从 way 转换的 area，在 way() 中已处理
                return
            
            # 对于 relation 形成的 area，跳过（在 relation 中处理）
        except Exception:
            pass
    
    def relation(self, r):
        """处理关系（如大型建筑群、校园等）"""
        self.relation_count += 1
        
        tags = dict(r.tags)
        if not tags:
            return
        
        # 只处理有名称的 relation
        category = self._get_category(tags)
        if not category:
            return
        
        # 获取名称
        name = None
        for name_key in ['name', 'name:zh', 'name:en']:
            val = tags.get(name_key)
            if val and val.strip():
                name = val.strip()
                break
        
        if not name:
            return
        
        # 对于 relation，尝试从成员中获取第一个有效的位置作为代表点
        try:
            for member in r.members:
                if member.type == 'n':  # node 成员
                    # 无法直接获取节点位置，跳过
                    continue
            # relation 的位置难以确定，此处暂时跳过
            # 可以后续使用更复杂的逻辑处理
        except Exception:
            pass


def insert_pois_batch(conn: sqlite3.Connection, pois: List[Dict]) -> int:
    """
    批量插入 POI 数据（内部辅助函数，用于流式写入）
    """
    if not pois:
        return 0
    
    cursor = conn.cursor()
    
    insert_sql = '''
        INSERT INTO poi (
            osm_id, osm_type, name, name_en, main_category, sub_category,
            lat, lon, address, phone, website, opening_hours, description, travel_time, rating, tags
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    '''

    data = [
        (
            poi['osm_id'],
            poi['osm_type'],
            poi['name'],
            poi['name_en'],
            poi['main_category'],
            poi['sub_category'],
            poi['lat'],
            poi['lon'],
            poi['address'],
            poi['phone'],
            poi['website'],
            poi['opening_hours'],
            poi.get('description'),
            poi.get('travel_time'),
            poi.get('rating'),
            poi['tags'],
        )
        for poi in pois
    ]
    
    # 在插入前获取当前最大 ID（executemany 后 lastrowid 可能为 None）
    cursor.execute("SELECT COALESCE(MAX(id), 0) FROM poi")
    max_id_before = cursor.fetchone()[0]
    
    cursor.executemany(insert_sql, data)
    
    # 同时插入 R-Tree 索引
    # 计算新插入记录的 ID 范围
    first_id = max_id_before + 1
    
    rtree_sql = '''
        INSERT INTO poi_rtree (id, min_lat, max_lat, min_lon, max_lon)
        VALUES (?, ?, ?, ?, ?)
    '''
    rtree_data = [
        (first_id + i, poi['lat'], poi['lat'], poi['lon'], poi['lon'])
        for i, poi in enumerate(pois)
    ]
    cursor.executemany(rtree_sql, rtree_data)
    
    conn.commit()
    return len(pois)


def create_database(db_path: str) -> sqlite3.Connection:
    """
    创建 SQLite 数据库和表结构
    """
    # 删除已存在的数据库
    if os.path.exists(db_path):
        os.remove(db_path)
    
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    # 创建主表
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS poi (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            osm_id INTEGER NOT NULL,
            osm_type TEXT NOT NULL,
            name TEXT NOT NULL,
            name_en TEXT,
            main_category TEXT NOT NULL,
            sub_category TEXT,
            lat REAL NOT NULL,
            lon REAL NOT NULL,
            address TEXT,
            phone TEXT,
            website TEXT,
            opening_hours TEXT,
            description TEXT,
            travel_time TEXT,
            rating REAL,
            tags TEXT,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    
    # 创建 FTS5 虚拟表用于全文搜索
    cursor.execute('''
        CREATE VIRTUAL TABLE IF NOT EXISTS poi_fts USING fts5(
            name,
            name_en,
            main_category,
            sub_category,
            address,
            content='poi',
            content_rowid='id',
            tokenize='unicode61'
        )
    ''')
    
    # 创建触发器以保持 FTS 表同步
    cursor.execute('''
        CREATE TRIGGER poi_ai AFTER INSERT ON poi BEGIN
            INSERT INTO poi_fts(rowid, name, name_en, main_category, sub_category, address)
            VALUES (new.id, new.name, new.name_en, new.main_category, new.sub_category, new.address);
        END
    ''')
    
    cursor.execute('''
        CREATE TRIGGER poi_ad AFTER DELETE ON poi BEGIN
            INSERT INTO poi_fts(poi_fts, rowid, name, name_en, main_category, sub_category, address)
            VALUES('delete', old.id, old.name, old.name_en, old.main_category, old.sub_category, old.address);
        END
    ''')
    
    cursor.execute('''
        CREATE TRIGGER poi_au AFTER UPDATE ON poi BEGIN
            INSERT INTO poi_fts(poi_fts, rowid, name, name_en, main_category, sub_category, address)
            VALUES('delete', old.id, old.name, old.name_en, old.main_category, old.sub_category, old.address);
            INSERT INTO poi_fts(rowid, name, name_en, main_category, sub_category, address)
            VALUES (new.id, new.name, new.name_en, new.main_category, new.sub_category, new.address);
        END
    ''')
    
    # 创建 R-Tree 空间索引（用于高效的范围查询和附近搜索）
    cursor.execute('''
        CREATE VIRTUAL TABLE IF NOT EXISTS poi_rtree USING rtree(
            id,
            min_lat, max_lat,
            min_lon, max_lon
        )
    ''')
    
    # 创建普通索引
    cursor.execute('CREATE INDEX idx_poi_category ON poi(main_category, sub_category)')
    cursor.execute('CREATE INDEX idx_poi_name ON poi(name)')
    
    # 创建分类统计表
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS category_stats (
            main_category TEXT NOT NULL,
            sub_category TEXT,
            count INTEGER DEFAULT 0,
            PRIMARY KEY (main_category, sub_category)
        )
    ''')
    
    # 创建元数据表
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS metadata (
            key TEXT PRIMARY KEY,
            value TEXT
        )
    ''')
    
    conn.commit()
    return conn


def insert_pois(conn: sqlite3.Connection, pois: List[Dict]) -> int:
    """
    批量插入 POI 数据（处理剩余的 POI，主要数据已在解析时流式写入）
    """
    if not pois:
        return 0
    
    cursor = conn.cursor()
    
    insert_sql = '''
        INSERT INTO poi (
            osm_id, osm_type, name, name_en, main_category, sub_category,
            lat, lon, address, phone, website, opening_hours, tags
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    '''
    
    batch_size = 1000
    total = len(pois)
    inserted = 0
    
    for i in range(0, total, batch_size):
        batch = pois[i:i+batch_size]
        
        # 在插入前获取当前最大 ID（executemany 后 lastrowid 可能为 None）
        cursor.execute("SELECT COALESCE(MAX(id), 0) FROM poi")
        max_id_before = cursor.fetchone()[0]
        
        data = [
            (
                poi['osm_id'],
                poi['osm_type'],
                poi['name'],
                poi['name_en'],
                poi['main_category'],
                poi['sub_category'],
                poi['lat'],
                poi['lon'],
                poi['address'],
                poi['phone'],
                poi['website'],
                poi['opening_hours'],
                poi['tags'],
            )
            for poi in batch
        ]
        
        cursor.executemany(insert_sql, data)
        
        # 计算新插入记录的 ID 范围用于 R-Tree
        first_id = max_id_before + 1
        
        # 插入 R-Tree 索引
        rtree_sql = '''
            INSERT INTO poi_rtree (id, min_lat, max_lat, min_lon, max_lon)
            VALUES (?, ?, ?, ?, ?)
        '''
        rtree_data = [
            (first_id + j, poi['lat'], poi['lat'], poi['lon'], poi['lon'])
            for j, poi in enumerate(batch)
        ]
        cursor.executemany(rtree_sql, rtree_data)
        
        inserted += len(batch)
        
        if inserted % 5000 == 0:
            print(f"  已插入 {inserted}/{total} 条记录")
    
    conn.commit()
    return inserted


def update_category_stats(conn: sqlite3.Connection):
    """
    更新分类统计信息
    """
    cursor = conn.cursor()
    
    cursor.execute('''
        INSERT OR REPLACE INTO category_stats (main_category, sub_category, count)
        SELECT main_category, sub_category, COUNT(*) as count
        FROM poi
        GROUP BY main_category, sub_category
    ''')
    
    conn.commit()


def update_metadata(conn: sqlite3.Connection, input_file: str, poi_count: int):
    """
    更新元数据信息
    """
    cursor = conn.cursor()
    
    metadata = [
        ('version', '1.0'),
        ('created_at', datetime.now().isoformat()),
        ('source_file', os.path.basename(input_file)),
        ('poi_count', str(poi_count)),
        ('generator', 'extract_poi.py'),
    ]
    
    cursor.executemany(
        'INSERT OR REPLACE INTO metadata (key, value) VALUES (?, ?)',
        metadata
    )
    
    conn.commit()


def print_stats(conn: sqlite3.Connection):
    """
    打印统计信息
    """
    cursor = conn.cursor()
    
    print("\n" + "=" * 60)
    print("POI 分类统计")
    print("=" * 60)
    
    cursor.execute('''
        SELECT main_category, SUM(count) as total
        FROM category_stats
        GROUP BY main_category
        ORDER BY total DESC
    ''')
    
    for row in cursor.fetchall():
        print(f"  {row[0]}: {row[1]} 条")
    
    cursor.execute('SELECT COUNT(*) FROM poi')
    total = cursor.fetchone()[0]
    print(f"\n总计: {total} 条 POI 记录")


def main():
    parser = argparse.ArgumentParser(
        description='从 OSM 数据中提取 POI 到 SQLite FTS5 数据库',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog='''
示例:
    python3 extract_poi.py --input wuhan.osm.pbf --output wuhan_poi.db
    python3 extract_poi.py -i china.osm.pbf -o china_poi.db --verbose
        '''
    )
    
    parser.add_argument(
        '-i', '--input',
        required=True,
        help='输入的 OSM PBF 文件路径'
    )
    
    parser.add_argument(
        '-o', '--output',
        required=True,
        help='输出的 SQLite 数据库文件路径'
    )
    
    parser.add_argument(
        '-v', '--verbose',
        action='store_true',
        help='显示详细输出'
    )
    
    args = parser.parse_args()
    
    # 检查输入文件
    if not os.path.exists(args.input):
        print(f"错误: 输入文件不存在: {args.input}")
        sys.exit(1)
    
    print("=" * 60)
    print("POI 提取工具")
    print("=" * 60)
    print(f"输入文件: {args.input}")
    print(f"输出文件: {args.output}")
    print()
    
    # 第一步：创建数据库（先创建，以便流式写入）
    print(">>> 步骤 1/4: 创建数据库...")
    conn = create_database(args.output)
    print("  数据库创建完成")
    
    # 第二步：解析 OSM 数据并流式写入
    print("\n>>> 步骤 2/4: 解析 OSM 数据...")
    handler = POIHandler(db_conn=conn)
    handler.apply_file(args.input, locations=True)
    
    # 写入剩余的 POI
    if handler.pois:
        insert_pois(conn, handler.pois)
    
    print(f"  处理完成:")
    print(f"    - 节点数: {handler.node_count}")
    print(f"    - 路径数: {handler.way_count}")
    print(f"    - 关系数: {handler.relation_count}")
    print(f"    - 提取 POI: {handler.poi_count}")
    
    if handler.poi_count == 0:
        print("\n警告: 未提取到任何 POI 数据")
        conn.close()
        sys.exit(0)
    
    # 第三步：跳过（数据已流式写入）
    print("\n>>> 步骤 3/4: 数据已流式写入...")
    
    # 统计实际插入数量
    cursor = conn.cursor()
    cursor.execute('SELECT COUNT(*) FROM poi')
    inserted = cursor.fetchone()[0]
    print(f"  共插入: {inserted} 条记录")
    
    # 第四步：更新统计和元数据
    print("\n>>> 步骤 4/4: 更新统计信息...")
    update_category_stats(conn)
    update_metadata(conn, args.input, inserted)
    print("  统计信息更新完成")
    
    # 显示统计
    print_stats(conn)
    
    # 关闭数据库
    conn.close()
    
    # 显示文件大小
    db_size = os.path.getsize(args.output)
    db_size_mb = db_size / (1024 * 1024)
    
    print(f"\n数据库文件大小: {db_size_mb:.2f} MB")
    print(f"\n✅ POI 数据库生成完成: {args.output}")


if __name__ == '__main__':
    main()
