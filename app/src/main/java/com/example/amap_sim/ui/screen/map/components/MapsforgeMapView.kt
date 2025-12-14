package com.example.amap_sim.ui.screen.map.components

import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.amap_sim.R
import com.example.amap_sim.data.local.OfflineDataManager
import com.example.amap_sim.domain.model.LatLng
import com.example.amap_sim.domain.model.MarkerData
import com.example.amap_sim.domain.model.MarkerType
import com.example.amap_sim.domain.model.RouteResult
import com.example.amap_sim.domain.model.toLatLng
import com.example.amap_sim.ui.screen.map.MapCommand
import com.example.amap_sim.ui.screen.map.MapEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.mapsforge.core.graphics.Paint
import org.mapsforge.core.graphics.Style
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.datastore.MapDataStore
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.overlay.Marker
import org.mapsforge.map.layer.overlay.Polyline
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.ExternalRenderTheme
import org.mapsforge.map.rendertheme.InternalRenderTheme
import java.io.File

private const val TAG = "MapsforgeMapView"

/**
 * Mapsforge 地图 Compose 组件
 * 
 * 封装 Mapsforge MapView，提供 Compose 友好的 API
 * 
 * @param modifier Modifier
 * @param center 地图中心点
 * @param zoomLevel 缩放级别 (0-22)
 * @param markers 标记点列表
 * @param routeResult 路线结果（用于绘制路线）
 * @param commands 命令流 - 用于外部控制地图
 * @param onMapReady 地图准备就绪回调
 * @param onMapClick 地图点击回调
 * @param onMapLongPress 地图长按回调
 * @param onMarkerClick 标记点点击回调
 * @param onMapMoveEnd 地图移动结束回调
 * @param onEvent 所有事件的统一回调
 */
@Composable
fun MapsforgeMapView(
    modifier: Modifier = Modifier,
    center: LatLng = LatLng.WUHAN_CENTER,
    zoomLevel: Int = 14,
    markers: List<MarkerData> = emptyList(),
    routeResult: RouteResult? = null,
    commands: Flow<MapCommand> = emptyFlow(),
    onMapReady: () -> Unit = {},
    onMapClick: (LatLng) -> Unit = {},
    onMapLongPress: (LatLng) -> Unit = {},
    onMarkerClick: (MarkerData) -> Unit = {},
    onMapMoveEnd: (center: LatLng, zoom: Int) -> Unit = { _, _ -> },
    onEvent: (MapEvent) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // 初始化 AndroidGraphicFactory（仅一次）
    LaunchedEffect(Unit) {
        try {
            AndroidGraphicFactory.createInstance(context.applicationContext)
        } catch (e: Exception) {
            // 已经初始化过了
            Log.d(TAG, "AndroidGraphicFactory already initialized")
        }
    }
    
    // 获取数据管理器
    val dataManager = remember { OfflineDataManager.getInstance(context) }
    
    // MapView 状态
    var mapViewState by remember { mutableStateOf<MapViewState?>(null) }
    
    // 创建 MapView
    val mapView = remember {
        MapView(context).apply {
            isClickable = true
            mapScaleBar.isVisible = true
            setBuiltInZoomControls(false) // 使用自定义缩放控件
        }
    }
    
    // 生命周期处理
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    // MapView 不需要特殊的 resume 处理
                }
                Lifecycle.Event.ON_PAUSE -> {
                    // MapView 不需要特殊的 pause 处理
                }
                Lifecycle.Event.ON_DESTROY -> {
                    mapViewState?.destroy()
                    mapView.destroyAll()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewState?.destroy()
            mapView.destroyAll()
        }
    }
    
    // 初始化地图
    LaunchedEffect(mapView) {
        if (!dataManager.isInitialized()) {
            Log.e(TAG, "离线数据未初始化")
            return@LaunchedEffect
        }
        
        try {
            val state = initializeMapView(
                mapView = mapView,
                dataManager = dataManager,
                initialCenter = center,
                initialZoom = zoomLevel
            )
            mapViewState = state
            
            // 设置事件监听
            setupMapListeners(
                mapView = mapView,
                state = state,
                onMapClick = { pos ->
                    onMapClick(pos)
                    onEvent(MapEvent.MapClick(pos))
                },
                onMapLongPress = { pos ->
                    onMapLongPress(pos)
                    onEvent(MapEvent.MapLongPress(pos))
                },
                onMarkerClick = { marker ->
                    onMarkerClick(marker)
                    onEvent(MapEvent.MarkerClick(marker))
                },
                onMapMoveEnd = { c, z ->
                    onMapMoveEnd(c, z)
                    onEvent(MapEvent.MapMoveEnd(c, z))
                }
            )
            
            onMapReady()
            onEvent(MapEvent.MapReady)
            
            Log.i(TAG, "地图初始化完成")
        } catch (e: Exception) {
            Log.e(TAG, "地图初始化失败", e)
        }
    }
    
    // 处理中心点变化
    LaunchedEffect(center, mapViewState) {
        mapViewState?.let { state ->
            val currentCenter = mapView.model.mapViewPosition.center
            if (currentCenter.latitude != center.lat || currentCenter.longitude != center.lon) {
                mapView.model.mapViewPosition.center = center.toMapsforgeLatLong()
            }
        }
    }
    
    // 处理缩放级别变化
    LaunchedEffect(zoomLevel, mapViewState) {
        mapViewState?.let {
            val currentZoom = mapView.model.mapViewPosition.zoomLevel.toInt()
            if (currentZoom != zoomLevel) {
                mapView.model.mapViewPosition.zoomLevel = zoomLevel.toByte()
            }
        }
    }
    
    // 处理标记点变化
    LaunchedEffect(markers, mapViewState) {
        mapViewState?.let { state ->
            updateMarkers(mapView, state, markers, context)
        }
    }
    
    // 处理路线变化
    LaunchedEffect(routeResult, mapViewState) {
        mapViewState?.let { state ->
            updateRoute(mapView, state, routeResult)
        }
    }
    
    // 处理命令
    LaunchedEffect(commands, mapViewState) {
        mapViewState?.let { state ->
            commands.collect { command ->
                handleMapCommand(mapView, state, command, context)
            }
        }
    }
    
    // 渲染 AndroidView
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * MapView 状态持有类
 */
private class MapViewState(
    val tileCache: TileCache,
    val tileRendererLayer: TileRendererLayer,
    val mapDataStore: MapDataStore,
    val markerLayer: MutableMap<String, Marker> = mutableMapOf(),
    val markerDataMap: MutableMap<String, MarkerData> = mutableMapOf(),
    var routePolyline: Polyline? = null
) {
    fun destroy() {
        try {
            tileRendererLayer.onDestroy()
            tileCache.destroy()
            mapDataStore.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying MapViewState", e)
        }
    }
}

/**
 * 初始化 MapView
 */
private fun initializeMapView(
    mapView: MapView,
    dataManager: OfflineDataManager,
    initialCenter: LatLng,
    initialZoom: Int
): MapViewState {
    // 创建瓦片缓存
    val tileCache = AndroidUtil.createTileCache(
        mapView.context,
        "mapcache",
        mapView.model.displayModel.tileSize,
        1f,
        mapView.model.frameBufferModel.overdrawFactor
    )
    
    // 打开地图文件
    val mapFile = dataManager.getMapFile()
    require(mapFile.exists()) { "地图文件不存在: ${mapFile.absolutePath}" }
    val mapDataStore = MapFile(mapFile)
    
    // 创建渲染主题
    val themeFile = dataManager.getThemeFile()
    val renderTheme = if (themeFile.exists()) {
        ExternalRenderTheme(themeFile)
    } else {
        // 使用内置主题
        InternalRenderTheme.DEFAULT
    }
    
    // 创建瓦片渲染层
    val tileRendererLayer = TileRendererLayer(
        tileCache,
        mapDataStore,
        mapView.model.mapViewPosition,
        AndroidGraphicFactory.INSTANCE
    )
    tileRendererLayer.setXmlRenderTheme(renderTheme)
    
    // 添加渲染层到地图
    mapView.layerManager.layers.add(tileRendererLayer)
    
    // 设置初始位置和缩放级别
    mapView.model.mapViewPosition.center = initialCenter.toMapsforgeLatLong()
    mapView.model.mapViewPosition.zoomLevel = initialZoom.toByte()
    
    // 设置缩放范围
    mapView.model.mapViewPosition.zoomLevelMin = 8.toByte()
    mapView.model.mapViewPosition.zoomLevelMax = 20.toByte()
    
    return MapViewState(
        tileCache = tileCache,
        tileRendererLayer = tileRendererLayer,
        mapDataStore = mapDataStore
    )
}

/**
 * 设置地图事件监听
 */
private fun setupMapListeners(
    mapView: MapView,
    state: MapViewState,
    onMapClick: (LatLng) -> Unit,
    onMapLongPress: (LatLng) -> Unit,
    onMarkerClick: (MarkerData) -> Unit,
    onMapMoveEnd: (LatLng, Int) -> Unit
) {
    // 添加 GestureDetector 来处理点击和长按
    val gestureDetector = android.view.GestureDetector(
        mapView.context,
        object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: android.view.MotionEvent): Boolean {
                val latLong = mapView.getMapViewProjection()
                    .fromPixels(e.x.toDouble(), e.y.toDouble())
                if (latLong != null) {
                    // 检查是否点击了标记点
                    val clickedMarker = findClickedMarker(mapView, state, e.x, e.y)
                    if (clickedMarker != null) {
                        onMarkerClick(clickedMarker)
                    } else {
                        onMapClick(latLong.toLatLng())
                    }
                }
                return true
            }
            
            override fun onLongPress(e: android.view.MotionEvent) {
                val latLong = mapView.getMapViewProjection()
                    .fromPixels(e.x.toDouble(), e.y.toDouble())
                if (latLong != null) {
                    onMapLongPress(latLong.toLatLng())
                }
            }
        }
    )
    
    mapView.setOnTouchListener { v, event ->
        gestureDetector.onTouchEvent(event)
        // 返回 false 让 MapView 继续处理滚动和缩放
        false
    }
    
    // 监听地图位置变化
    mapView.model.mapViewPosition.addObserver { 
        val center = mapView.model.mapViewPosition.center
        val zoom = mapView.model.mapViewPosition.zoomLevel.toInt()
        onMapMoveEnd(center.toLatLng(), zoom)
    }
}

/**
 * 查找点击的标记点
 */
private fun findClickedMarker(
    mapView: MapView,
    state: MapViewState,
    x: Float,
    y: Float
): MarkerData? {
    val clickRadius = 30.0 // 点击容差（像素）
    
    for ((id, marker) in state.markerLayer) {
        val markerLatLong = marker.latLong ?: continue
        val markerPixel = mapView.getMapViewProjection().toPixels(markerLatLong)
        
        val dx = x - markerPixel.x
        val dy = y - markerPixel.y
        val distance = kotlin.math.sqrt((dx * dx + dy * dy).toDouble())
        
        if (distance <= clickRadius) {
            return state.markerDataMap[id]
        }
    }
    return null
}

/**
 * 更新标记点
 */
private fun updateMarkers(
    mapView: MapView,
    state: MapViewState,
    markers: List<MarkerData>,
    context: android.content.Context
) {
    // 移除不再存在的标记
    val newMarkerIds = markers.map { it.id }.toSet()
    val markersToRemove = state.markerLayer.keys.filter { it !in newMarkerIds }
    for (id in markersToRemove) {
        state.markerLayer[id]?.let { marker ->
            mapView.layerManager.layers.remove(marker)
        }
        state.markerLayer.remove(id)
        state.markerDataMap.remove(id)
    }
    
    // 添加或更新标记
    for (markerData in markers) {
        val existingMarker = state.markerLayer[markerData.id]
        
        if (existingMarker != null) {
            // 更新位置
            existingMarker.latLong = markerData.position.toMapsforgeLatLong()
        } else {
            // 创建新标记
            val drawable = getMarkerDrawable(context, markerData)
            val bitmap = AndroidGraphicFactory.convertToBitmap(drawable)
            
            val marker = Marker(
                markerData.position.toMapsforgeLatLong(),
                bitmap,
                (bitmap.width * markerData.anchorX).toInt(),
                (bitmap.height * markerData.anchorY).toInt()
            )
            
            state.markerLayer[markerData.id] = marker
            state.markerDataMap[markerData.id] = markerData
            mapView.layerManager.layers.add(marker)
        }
    }
    
    mapView.layerManager.redrawLayers()
}

/**
 * 获取标记点 Drawable
 */
private fun getMarkerDrawable(
    context: android.content.Context,
    markerData: MarkerData
): Drawable {
    // 如果有自定义图标，使用自定义图标
    markerData.iconRes?.let { iconRes ->
        return ContextCompat.getDrawable(context, iconRes)
            ?: getDefaultMarkerDrawable(context, markerData.type)
    }
    
    // 根据类型获取默认图标
    return getDefaultMarkerDrawable(context, markerData.type)
}

/**
 * 获取默认标记点 Drawable
 */
private fun getDefaultMarkerDrawable(
    context: android.content.Context,
    type: MarkerType
): Drawable {
    // 创建一个简单的圆形标记
    val size = when (type) {
        MarkerType.START, MarkerType.END -> 48
        MarkerType.WAYPOINT -> 40
        MarkerType.CURRENT_LOCATION -> 44
        else -> 36
    }
    
    val color = when (type) {
        MarkerType.START -> android.graphics.Color.GREEN
        MarkerType.END -> android.graphics.Color.RED
        MarkerType.WAYPOINT -> android.graphics.Color.BLUE
        MarkerType.CURRENT_LOCATION -> android.graphics.Color.CYAN
        MarkerType.POI -> android.graphics.Color.MAGENTA
        MarkerType.SEARCH_RESULT -> android.graphics.Color.rgb(255, 140, 0) // Orange
        else -> android.graphics.Color.rgb(66, 133, 244) // 默认蓝色
    }
    
    return createCircleMarkerDrawable(size, color)
}

/**
 * 创建圆形标记 Drawable
 */
private fun createCircleMarkerDrawable(size: Int, color: Int): Drawable {
    val bitmap = android.graphics.Bitmap.createBitmap(
        size, size,
        android.graphics.Bitmap.Config.ARGB_8888
    )
    val canvas = android.graphics.Canvas(bitmap)
    
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        this.color = color
        style = android.graphics.Paint.Style.FILL
    }
    
    // 绘制阴影
    val shadowPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        this.color = android.graphics.Color.argb(80, 0, 0, 0)
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f + 2, size / 2f + 2, size / 2f - 4, shadowPaint)
    
    // 绘制主圆
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, paint)
    
    // 绘制边框
    val borderPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        this.color = android.graphics.Color.WHITE
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 3f
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 5, borderPaint)
    
    // 绘制内部小圆点
    val innerPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        this.color = android.graphics.Color.WHITE
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 6f, innerPaint)
    
    return android.graphics.drawable.BitmapDrawable(
        android.content.res.Resources.getSystem(),
        bitmap
    )
}

/**
 * 更新路线
 */
private fun updateRoute(
    mapView: MapView,
    state: MapViewState,
    routeResult: RouteResult?
) {
    // 移除旧路线
    state.routePolyline?.let { polyline ->
        mapView.layerManager.layers.remove(polyline)
    }
    state.routePolyline = null
    
    // 如果没有新路线，直接返回
    if (routeResult == null || routeResult.points.isEmpty()) {
        mapView.layerManager.redrawLayers()
        return
    }
    
    // 创建路线画笔
    val paintStroke = AndroidGraphicFactory.INSTANCE.createPaint().apply {
        setStyle(Style.STROKE)
        setStrokeWidth(12f)
        color = 0xFF4285F4.toInt() // Google 蓝
    }
    
    // 创建 Polyline
    val polyline = Polyline(paintStroke, AndroidGraphicFactory.INSTANCE)
    
    // 添加路线点
    for (point in routeResult.points) {
        polyline.latLongs.add(point.toMapsforgeLatLong())
    }
    
    // 添加到地图
    mapView.layerManager.layers.add(polyline)
    state.routePolyline = polyline
    
    mapView.layerManager.redrawLayers()
}

/**
 * 处理地图命令
 */
private fun handleMapCommand(
    mapView: MapView,
    state: MapViewState,
    command: MapCommand,
    context: android.content.Context
) {
    when (command) {
        is MapCommand.MoveTo -> {
            mapView.model.mapViewPosition.center = command.position.toMapsforgeLatLong()
            command.zoomLevel?.let {
                mapView.model.mapViewPosition.zoomLevel = it.toByte()
            }
        }
        
        is MapCommand.ZoomTo -> {
            mapView.model.mapViewPosition.zoomLevel = command.zoomLevel.toByte()
        }
        
        is MapCommand.ZoomIn -> {
            val currentZoom = mapView.model.mapViewPosition.zoomLevel
            val maxZoom = mapView.model.mapViewPosition.zoomLevelMax
            if (currentZoom < maxZoom) {
                mapView.model.mapViewPosition.zoomLevel = (currentZoom + 1).toByte()
            }
        }
        
        is MapCommand.ZoomOut -> {
            val currentZoom = mapView.model.mapViewPosition.zoomLevel
            val minZoom = mapView.model.mapViewPosition.zoomLevelMin
            if (currentZoom > minZoom) {
                mapView.model.mapViewPosition.zoomLevel = (currentZoom - 1).toByte()
            }
        }
        
        is MapCommand.FitBounds -> {
            val boundingBox = org.mapsforge.core.model.BoundingBox(
                command.minLat,
                command.minLon,
                command.maxLat,
                command.maxLon
            )
            val dimension = mapView.model.mapViewDimension.dimension
            if (dimension != null) {
                val zoomLevel = org.mapsforge.core.util.LatLongUtils.zoomForBounds(
                    dimension,
                    boundingBox,
                    mapView.model.displayModel.tileSize
                )
                mapView.model.mapViewPosition.center = boundingBox.centerPoint
                mapView.model.mapViewPosition.zoomLevel = 
                    (zoomLevel - 1).coerceIn(8, 18).toByte()
            }
        }
        
        is MapCommand.AddMarker -> {
            updateMarkers(mapView, state, 
                state.markerDataMap.values.toList() + command.marker, context)
        }
        
        is MapCommand.RemoveMarker -> {
            state.markerLayer[command.markerId]?.let { marker ->
                mapView.layerManager.layers.remove(marker)
            }
            state.markerLayer.remove(command.markerId)
            state.markerDataMap.remove(command.markerId)
            mapView.layerManager.redrawLayers()
        }
        
        is MapCommand.ClearMarkers -> {
            for ((_, marker) in state.markerLayer) {
                mapView.layerManager.layers.remove(marker)
            }
            state.markerLayer.clear()
            state.markerDataMap.clear()
            mapView.layerManager.redrawLayers()
        }
        
        is MapCommand.ShowRoute -> {
            updateRoute(mapView, state, command.route)
        }
        
        is MapCommand.ClearRoute -> {
            updateRoute(mapView, state, null)
        }
        
        is MapCommand.Redraw -> {
            mapView.layerManager.redrawLayers()
        }
    }
}

// 扩展函数
private fun LatLng.toMapsforgeLatLong(): LatLong = LatLong(lat, lon)
