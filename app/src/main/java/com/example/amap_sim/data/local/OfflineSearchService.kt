package com.example.amap_sim.data.local

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.amap_sim.domain.model.LatLng
import com.example.amap_sim.domain.model.PoiCategory
import com.example.amap_sim.domain.model.PoiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 离线搜索服务
 * 
 * 基于 SQLite FTS5 的离线 POI 搜索服务
 * 
 * 功能：
 * - 关键词搜索（支持模糊匹配）
 * - 附近搜索（基于距离）
 * - 分类搜索
 * - 热门推荐
 * 
 * 数据库表结构：
 * - poi: 主表，存储 POI 基础信息
 * - poi_fts: FTS5 虚拟表，用于全文搜索
 */
class OfflineSearchService(
    private val dataManager: OfflineDataManager
) {
    companion object {
        private const val TAG = "OfflineSearchService"
        
        // 默认搜索参数
        const val DEFAULT_LIMIT = 20
        const val DEFAULT_RADIUS_METERS = 5000.0 // 5公里
        const val MAX_RADIUS_METERS = 50000.0 // 50公里
        
        // 地球半径（米），用于距离计算
        private const val EARTH_RADIUS = 6371000.0
    }
    
    private var database: SQLiteDatabase? = null
    private val initMutex = Mutex()
    private var isInitialized = false
    
    // 数据库表和列名
    private object Tables {
        const val POI = "poi"
        const val POI_FTS = "poi_fts"
    }
    
    private object Columns {
        const val ID = "id"
        const val NAME = "name"
        const val MAIN_CATEGORY = "main_category"
        const val SUB_CATEGORY = "sub_category"
        const val LAT = "lat"
        const val LON = "lon"
        const val ADDRESS = "address"
        const val PHONE = "phone"
    }
    
    /**
     * 初始化搜索服务
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        initMutex.withLock {
            if (isInitialized && database != null) {
                Log.d(TAG, "搜索服务已初始化")
                return@withContext Result.success(Unit)
            }
            
            try {
                val dbFile = dataManager.getPoiDbFile()
                require(dbFile.exists()) {
                    "POI 数据库文件不存在: ${dbFile.absolutePath}"
                }
                
                Log.i(TAG, "开始初始化搜索服务，数据库: ${dbFile.absolutePath}")
                
                // 以只读方式打开数据库
                database = SQLiteDatabase.openDatabase(
                    dbFile.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READONLY
                )
                
                // 验证数据库结构
                validateDatabase()
                
                isInitialized = true
                Log.i(TAG, "搜索服务初始化完成")
                
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "搜索服务初始化失败", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * 验证数据库结构
     */
    private fun validateDatabase() {
        val db = database ?: throw IllegalStateException("数据库未打开")
        
        // 检查 poi 表是否存在
        val cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(Tables.POI)
        )
        
        val tableExists = cursor.use { it.count > 0 }
        require(tableExists) { "POI 表不存在" }
        
        Log.d(TAG, "数据库验证通过")
    }
    
    /**
     * 检查是否已初始化
     */
    fun isReady(): Boolean = isInitialized && database != null
    
    /**
     * 关键词搜索
     * 
     * @param keyword 搜索关键词
     * @param limit 返回结果数量限制
     * @param center 中心点（用于计算距离和排序）
     * @return POI 结果列表
     */
    suspend fun searchByKeyword(
        keyword: String,
        limit: Int = DEFAULT_LIMIT,
        center: LatLng? = null
    ): Result<List<PoiResult>> = withContext(Dispatchers.IO) {
        try {
            val db = database
            requireNotNull(db) { "搜索服务未初始化" }
            require(keyword.isNotBlank()) { "关键词不能为空" }
            
            Log.d(TAG, "关键词搜索: $keyword, limit=$limit")
            
            val results = mutableListOf<PoiResult>()
            
            // 首先尝试 FTS 全文搜索
            val ftsResults = searchFts(db, keyword, limit * 2)
            results.addAll(ftsResults)
            
            // 如果 FTS 结果不足，用 LIKE 补充
            if (results.size < limit) {
                val likeResults = searchLike(db, keyword, limit - results.size, results.map { it.id })
                results.addAll(likeResults)
            }
            
            // 计算距离并排序
            val sortedResults = if (center != null) {
                results.map { poi ->
                    poi.copy(distance = center.distanceTo(poi.location))
                }.sortedBy { it.distance }
            } else {
                results
            }
            
            Log.i(TAG, "关键词搜索完成: ${sortedResults.size} 条结果")
            
            Result.success(sortedResults.take(limit))
        } catch (e: Exception) {
            Log.e(TAG, "关键词搜索失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * FTS 全文搜索
     */
    private fun searchFts(db: SQLiteDatabase, keyword: String, limit: Int): List<PoiResult> {
        val results = mutableListOf<PoiResult>()
        
        try {
            // 检查 FTS 表是否存在
            val checkCursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                arrayOf(Tables.POI_FTS)
            )
            val ftsExists = checkCursor.use { it.count > 0 }
            
            if (!ftsExists) {
                Log.d(TAG, "FTS 表不存在，跳过全文搜索")
                return results
            }
            
            // FTS 搜索（匹配名称或地址）
            val ftsQuery = """
                SELECT p.${Columns.ID}, p.${Columns.NAME}, p.${Columns.MAIN_CATEGORY},
                       p.${Columns.LAT}, p.${Columns.LON}, p.${Columns.ADDRESS}, p.${Columns.PHONE},
                       p.opening_hours, p.description, p.travel_time, p.rating
                FROM ${Tables.POI} p
                INNER JOIN ${Tables.POI_FTS} f ON p.${Columns.ID} = f.rowid
                WHERE ${Tables.POI_FTS} MATCH ?
                LIMIT ?
            """.trimIndent()
            
            // FTS5 搜索语法：使用 * 进行前缀匹配
            val searchTerm = "${keyword}*"
            
            db.rawQuery(ftsQuery, arrayOf(searchTerm, limit.toString())).use { cursor ->
                while (cursor.moveToNext()) {
                    results.add(cursorToPoi(cursor))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "FTS 搜索失败，回退到 LIKE 搜索", e)
        }
        
        return results
    }
    
    /**
     * LIKE 模糊搜索（作为后备方案）
     */
    private fun searchLike(
        db: SQLiteDatabase, 
        keyword: String, 
        limit: Int,
        excludeIds: List<Long> = emptyList()
    ): List<PoiResult> {
        val results = mutableListOf<PoiResult>()
        
        val excludeClause = if (excludeIds.isNotEmpty()) {
            "AND ${Columns.ID} NOT IN (${excludeIds.joinToString(",")})"
        } else {
            ""
        }
        
        val query = """
            SELECT ${Columns.ID}, ${Columns.NAME}, ${Columns.MAIN_CATEGORY},
                   ${Columns.LAT}, ${Columns.LON}, ${Columns.ADDRESS}, ${Columns.PHONE},
                   opening_hours, description, travel_time, rating
            FROM ${Tables.POI}
            WHERE (${Columns.NAME} LIKE ? OR ${Columns.ADDRESS} LIKE ?)
            $excludeClause
            LIMIT ?
        """.trimIndent()
        
        val pattern = "%$keyword%"
        
        db.rawQuery(query, arrayOf(pattern, pattern, limit.toString())).use { cursor ->
            while (cursor.moveToNext()) {
                results.add(cursorToPoi(cursor))
            }
        }
        
        return results
    }
    
    /**
     * 附近搜索
     * 
     * @param center 中心点坐标
     * @param radiusMeters 搜索半径（米）
     * @param category 分类过滤（可选）
     * @param limit 返回结果数量限制
     * @return POI 结果列表（按距离排序）
     */
    suspend fun searchNearby(
        center: LatLng,
        radiusMeters: Double = DEFAULT_RADIUS_METERS,
        category: String? = null,
        limit: Int = DEFAULT_LIMIT
    ): Result<List<PoiResult>> = withContext(Dispatchers.IO) {
        try {
            val db = database
            requireNotNull(db) { "搜索服务未初始化" }
            
            val radius = radiusMeters.coerceIn(100.0, MAX_RADIUS_METERS)
            Log.d(TAG, "附近搜索: center=$center, radius=${radius}m, category=$category")
            
            // 计算经纬度范围（简化的矩形范围）
            val latRange = radius / EARTH_RADIUS * (180.0 / Math.PI)
            val lonRange = radius / (EARTH_RADIUS * Math.cos(Math.toRadians(center.lat))) * (180.0 / Math.PI)
            
            val minLat = center.lat - latRange
            val maxLat = center.lat + latRange
            val minLon = center.lon - lonRange
            val maxLon = center.lon + lonRange
            
            val categoryClause = if (category != null) {
                "AND ${Columns.MAIN_CATEGORY} = ?"
            } else {
                ""
            }
            
            // 使用 Haversine 公式计算精确距离
            val query = """
                SELECT ${Columns.ID}, ${Columns.NAME}, ${Columns.MAIN_CATEGORY},
                       ${Columns.LAT}, ${Columns.LON}, ${Columns.ADDRESS}, ${Columns.PHONE},
                       (6371000 * acos(
                           cos(radians(?)) * cos(radians(${Columns.LAT})) *
                           cos(radians(${Columns.LON}) - radians(?)) +
                           sin(radians(?)) * sin(radians(${Columns.LAT}))
                       )) AS distance
                FROM ${Tables.POI}
                WHERE ${Columns.LAT} BETWEEN ? AND ?
                  AND ${Columns.LON} BETWEEN ? AND ?
                  $categoryClause
                HAVING distance <= ?
                ORDER BY distance
                LIMIT ?
            """.trimIndent()
            
            val args = mutableListOf(
                center.lat.toString(),
                center.lon.toString(),
                center.lat.toString(),
                minLat.toString(),
                maxLat.toString(),
                minLon.toString(),
                maxLon.toString()
            )
            
            if (category != null) {
                args.add(category)
            }
            
            args.add(radius.toString())
            args.add(limit.toString())
            
            val results = mutableListOf<PoiResult>()
            
            // 简化查询（不使用 SQLite 的数学函数，改为在代码中计算距离）
            val simpleQuery = """
                SELECT ${Columns.ID}, ${Columns.NAME}, ${Columns.MAIN_CATEGORY},
                       ${Columns.LAT}, ${Columns.LON}, ${Columns.ADDRESS}, ${Columns.PHONE},
                       opening_hours, description, travel_time, rating
                FROM ${Tables.POI}
                WHERE ${Columns.LAT} BETWEEN ? AND ?
                  AND ${Columns.LON} BETWEEN ? AND ?
                  $categoryClause
            """.trimIndent()
            
            val simpleArgs = mutableListOf(
                minLat.toString(),
                maxLat.toString(),
                minLon.toString(),
                maxLon.toString()
            )
            if (category != null) {
                simpleArgs.add(category)
            }
            
            db.rawQuery(simpleQuery, simpleArgs.toTypedArray()).use { cursor ->
                Log.d(TAG, "数据库查询返回 ${cursor.count} 条记录")
                while (cursor.moveToNext()) {
                    val poi = cursorToPoi(cursor)
                    val distance = center.distanceTo(poi.location)
                    Log.d(TAG, "  POI: ${poi.name} at (${poi.lat}, ${poi.lon}), distance=${distance}m")
                    if (distance <= radius) {
                        results.add(poi.copy(distance = distance))
                    }
                }
            }
            
            // 按距离排序并限制数量
            val sortedResults = results.sortedBy { it.distance }.take(limit)
            
            Log.i(TAG, "附近搜索完成: ${sortedResults.size} 条结果")
            
            Result.success(sortedResults)
        } catch (e: Exception) {
            Log.e(TAG, "附近搜索失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 分类搜索
     * 
     * @param category 分类名称
     * @param center 中心点（用于计算距离和排序）
     * @param limit 返回结果数量限制
     * @return POI 结果列表
     */
    suspend fun searchByCategory(
        category: String,
        center: LatLng? = null,
        limit: Int = DEFAULT_LIMIT
    ): Result<List<PoiResult>> = withContext(Dispatchers.IO) {
        try {
            val db = database
            requireNotNull(db) { "搜索服务未初始化" }
            require(category.isNotBlank()) { "分类不能为空" }
            
            Log.d(TAG, "分类搜索: $category, limit=$limit")
            
            val query = """
                SELECT ${Columns.ID}, ${Columns.NAME}, ${Columns.MAIN_CATEGORY},
                       ${Columns.LAT}, ${Columns.LON}, ${Columns.ADDRESS}, ${Columns.PHONE},
                       opening_hours, description, travel_time, rating
                FROM ${Tables.POI}
                WHERE ${Columns.MAIN_CATEGORY} = ?
                LIMIT ?
            """.trimIndent()
            
            val results = mutableListOf<PoiResult>()
            
            db.rawQuery(query, arrayOf(category, (limit * 2).toString())).use { cursor ->
                while (cursor.moveToNext()) {
                    results.add(cursorToPoi(cursor))
                }
            }
            
            // 计算距离并排序
            val sortedResults = if (center != null) {
                results.map { poi ->
                    poi.copy(distance = center.distanceTo(poi.location))
                }.sortedBy { it.distance }.take(limit)
            } else {
                results.take(limit)
            }
            
            Log.i(TAG, "分类搜索完成: ${sortedResults.size} 条结果")
            
            Result.success(sortedResults)
        } catch (e: Exception) {
            Log.e(TAG, "分类搜索失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取所有可用分类
     */
    suspend fun getAvailableCategories(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val db = database
            requireNotNull(db) { "搜索服务未初始化" }
            
            val query = """
                SELECT DISTINCT ${Columns.MAIN_CATEGORY}
                FROM ${Tables.POI}
                ORDER BY ${Columns.MAIN_CATEGORY}
            """.trimIndent()
            
            val categories = mutableListOf<String>()
            
            db.rawQuery(query, null).use { cursor ->
                while (cursor.moveToNext()) {
                    cursor.getString(0)?.let { categories.add(it) }
                }
            }
            
            Log.d(TAG, "获取分类列表: ${categories.size} 个")
            
            Result.success(categories)
        } catch (e: Exception) {
            Log.e(TAG, "获取分类列表失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取热门分类及其 POI 数量
     */
    suspend fun getPopularCategories(): Result<List<Pair<String, Int>>> = withContext(Dispatchers.IO) {
        try {
            val db = database
            requireNotNull(db) { "搜索服务未初始化" }
            
            val query = """
                SELECT ${Columns.MAIN_CATEGORY}, COUNT(*) as count
                FROM ${Tables.POI}
                GROUP BY ${Columns.MAIN_CATEGORY}
                ORDER BY count DESC
                LIMIT 10
            """.trimIndent()
            
            val categories = mutableListOf<Pair<String, Int>>()
            
            db.rawQuery(query, null).use { cursor ->
                while (cursor.moveToNext()) {
                    val category = cursor.getString(0) ?: continue
                    val count = cursor.getInt(1)
                    categories.add(category to count)
                }
            }
            
            Result.success(categories)
        } catch (e: Exception) {
            Log.e(TAG, "获取热门分类失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 根据 ID 获取 POI 详情
     */
    suspend fun getPoiById(id: Long): Result<PoiResult?> = withContext(Dispatchers.IO) {
        try {
            val db = database
            requireNotNull(db) { "搜索服务未初始化" }
            
            val query = """
                SELECT ${Columns.ID}, ${Columns.NAME}, ${Columns.MAIN_CATEGORY},
                       ${Columns.LAT}, ${Columns.LON}, ${Columns.ADDRESS}, ${Columns.PHONE},
                       opening_hours, description, travel_time, rating
                FROM ${Tables.POI}
                WHERE ${Columns.ID} = ?
            """.trimIndent()
            
            var result: PoiResult? = null
            
            db.rawQuery(query, arrayOf(id.toString())).use { cursor ->
                if (cursor.moveToFirst()) {
                    result = cursorToPoi(cursor)
                }
            }
            
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "获取 POI 详情失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取数据库中的 POI 总数
     */
    suspend fun getPoiCount(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val db = database
            requireNotNull(db) { "搜索服务未初始化" }
            
            var count = 0
            db.rawQuery("SELECT COUNT(*) FROM ${Tables.POI}", null).use { cursor ->
                if (cursor.moveToFirst()) {
                    count = cursor.getInt(0)
                }
            }
            
            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "获取 POI 数量失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 从游标读取 POI 数据
     */
    private fun cursorToPoi(cursor: Cursor): PoiResult {
        val rating = cursor.getDoubleOrNull("rating")
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(Columns.ID))
        val name = cursor.getString(cursor.getColumnIndexOrThrow(Columns.NAME)) ?: ""

        // 调试日志：显示rating读取情况
        if (rating != null) {
            Log.d(TAG, "POI[$id] $name 有评分: $rating")
        }

        return PoiResult(
            id = id,
            name = name,
            category = cursor.getString(cursor.getColumnIndexOrThrow(Columns.MAIN_CATEGORY)) ?: "",
            lat = cursor.getDouble(cursor.getColumnIndexOrThrow(Columns.LAT)),
            lon = cursor.getDouble(cursor.getColumnIndexOrThrow(Columns.LON)),
            address = cursor.getStringOrNull(Columns.ADDRESS),
            phone = cursor.getStringOrNull(Columns.PHONE),
            openingHours = cursor.getStringOrNull("opening_hours"),
            description = cursor.getStringOrNull("description"),
            travelTime = cursor.getStringOrNull("travel_time"),
            rating = rating
        )
    }

    /**
     * 安全获取Double列（可能为 null）
     */
    private fun Cursor.getDoubleOrNull(columnName: String): Double? {
        val index = getColumnIndex(columnName)
        return if (index >= 0 && !isNull(index)) getDouble(index) else null
    }
    
    /**
     * 安全获取字符串列（可能为 null）
     */
    private fun Cursor.getStringOrNull(columnName: String): String? {
        val index = getColumnIndex(columnName)
        return if (index >= 0 && !isNull(index)) getString(index) else null
    }
    
    /**
     * 释放资源
     */
    fun close() {
        try {
            database?.close()
            database = null
            isInitialized = false
            Log.i(TAG, "搜索服务资源已释放")
        } catch (e: Exception) {
            Log.e(TAG, "释放搜索服务资源失败", e)
        }
    }
}

/**
 * 搜索异常
 */
class SearchException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
