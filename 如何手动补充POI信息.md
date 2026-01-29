# 如何手动补充POI信息

## 数据库位置
POI数据存储在SQLite数据库中：
```
app/src/main/assets/map/wuhan_poi.db
```

## 数据库结构
poi表包含以下字段：
- `id`: POI ID
- `name`: 名称
- `phone`: 电话
- `address`: 地址
- `opening_hours`: 开放时间
- `description`: 景点简介
- `travel_time`: 行车时间（可选，通常由系统计算）
- `lat`, `lon`: 经纬度
- `main_category`: 主分类
- `sub_category`: 子分类

## 手动补充步骤

### 1. 查找POI记录
```sql
sqlite3 "app/src/main/assets/map/wuhan_poi.db" "SELECT id, name FROM poi WHERE name LIKE '%关键词%';"
```

### 2. 更新POI信息
```sql
sqlite3 "app/src/main/assets/map/wuhan_poi.db" "UPDATE poi SET
    phone='电话号码',
    address='详细地址',
    opening_hours='开放时间',
    description='景点简介'
WHERE id=POI的ID;"
```

## 示例：八七会议会址纪念馆

已更新的信息：
- **ID**: 677
- **名称**: 八七会议会址纪念馆
- **电话**: 027-82835088
- **地址**: 湖北省武汉市江岸区一元街街道一元街办事处邮阳街139号
- **开放时间**: 周一至周四,周六至周日 09:00-17:00
- **简介**: 八七会议会址纪念馆位于湖北省武汉市江岸区一元街街道一元街办事处邮阳街139号，依托1927年中共中央召开的"八七会议"旧址而建。

## 注意事项

1. **数据库编码**: 使用UTF-8编码
2. **行车时间**: travel_time字段通常由系统根据距离自动计算，无需手动填写
3. **重新安装**: 修改数据库后，需要卸载并重新安装应用，或清除应用数据
4. **备份**: 修改前建议备份原数据库文件

## 验证更新
```sql
sqlite3 "app/src/main/assets/map/wuhan_poi.db" "SELECT id, name, phone, opening_hours FROM poi WHERE id=677;"
```
