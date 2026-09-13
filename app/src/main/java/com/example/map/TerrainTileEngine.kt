package com.example.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.*
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.*

enum class MapLayerType(val displayName: String, val subtitle: String) {
    TERRAIN_TOPO("Terrain Topo", "World Topographic & Elevation Contours"),
    SATELLITE("Satellite", "High-Resolution Orbital Optical Imagery"),
    STREET_MAP("Vector Street", "OpenStreetMap Urban Network")
}

data class TileKey(val z: Int, val x: Int, val y: Int, val layerType: MapLayerType)

/**
 * High-performance asynchronous Tile Engine for rendering real-world Terrain & Satellite maps.
 * Handles Web Mercator projection, LRU memory caching, OkHttp disk caching, and seamless canvas drawing.
 */
class TerrainTileEngine(context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 64MB In-Memory Bitmap Cache
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 8).coerceAtLeast(32 * 1024) // min 32MB

    private val memoryCache = object : LruCache<TileKey, ImageBitmap>(cacheSize) {
        override fun sizeOf(key: TileKey, value: ImageBitmap): Int {
            // Approx size in KB (256x256 ARGB_8888 = 256KB)
            return (value.width * value.height * 4) / 1024
        }
    }

    private val activeLoads = mutableSetOf<TileKey>()

    // OkHttp Client with 50MB Disk Cache
    private val httpClient: OkHttpClient by lazy {
        val cacheDir = File(context.cacheDir, "terrain_tile_cache")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        OkHttpClient.Builder()
            .cache(Cache(cacheDir, 50L * 1024 * 1024))
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "ISRO-NavDR-EdgeEngine/1.0 (Android; TopoViewer)")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    private var onTileLoadedListener: (() -> Unit)? = null

    fun setOnTileLoadedListener(listener: () -> Unit) {
        this.onTileLoadedListener = listener
    }

    /**
     * Converts WGS84 Geodetic Lat/Lng to Web Mercator World Pixel coordinates at given zoom level.
     */
    fun latLngToWorldPixel(lat: Double, lng: Double, zoom: Double): Pair<Double, Double> {
        val clampedLat = lat.coerceIn(-85.05112878, 85.05112878)
        val latRad = Math.toRadians(clampedLat)
        val n = 2.0.pow(zoom) * 256.0

        val worldX = (lng + 180.0) / 360.0 * n
        val worldY = (1.0 - asinh(tan(latRad)) / Math.PI) / 2.0 * n
        return Pair(worldX, worldY)
    }

    /**
     * Converts Web Mercator World Pixel coordinates back to WGS84 Lat/Lng.
     */
    fun worldPixelToLatLng(worldX: Double, worldY: Double, zoom: Double): Pair<Double, Double> {
        val n = 2.0.pow(zoom) * 256.0
        val lng = (worldX / n) * 360.0 - 180.0
        val latRad = atan(sinh(Math.PI * (1.0 - 2.0 * worldY / n)))
        val lat = Math.toDegrees(latRad)
        return Pair(lat, lng)
    }

    /**
     * Gets Tile URL according to the selected Map Layer
     */
    private fun getTileUrl(key: TileKey): String {
        return when (key.layerType) {
            MapLayerType.TERRAIN_TOPO -> {
                // ESRI World Topo Map with elevation shading & contours
                "https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/${key.z}/${key.y}/${key.x}"
            }
            MapLayerType.SATELLITE -> {
                // ESRI World Imagery (High-res orbital imagery)
                "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/${key.z}/${key.y}/${key.x}"
            }
            MapLayerType.STREET_MAP -> {
                // OpenStreetMap Standard
                val sub = listOf("a", "b", "c")[(key.x + key.y) % 3]
                "https://$sub.tile.openstreetmap.org/${key.z}/${key.x}/${key.y}.png"
            }
        }
    }

    /**
     * Asynchronously requests tile bitmap from network/cache.
     */
    private fun requestTile(key: TileKey) {
        synchronized(activeLoads) {
            if (activeLoads.contains(key) || memoryCache.get(key) != null) return
            activeLoads.add(key)
        }

        scope.launch {
            try {
                val url = getTileUrl(key)
                val request = Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()

                if (response.isSuccessful) {
                    val bytes = response.body?.bytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bitmap != null) {
                            val imageBitmap = bitmap.asImageBitmap()
                            memoryCache.put(key, imageBitmap)
                            withContext(Dispatchers.Main) {
                                onTileLoadedListener?.invoke()
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Network error or timeout: fall back gracefully to vector terrain
            } finally {
                synchronized(activeLoads) {
                    activeLoads.remove(key)
                }
            }
        }
    }

    /**
     * Draws actual terrain and map tiles directly on the Compose DrawScope.
     */
    fun drawTiles(
        drawScope: DrawScope,
        centerLat: Double,
        centerLng: Double,
        zoomScale: Float,
        centerCanvasX: Float,
        centerCanvasY: Float,
        layerType: MapLayerType
    ) {
        // Compute discrete zoom level and continuous sub-pixel scale
        // zoomScale 1.8f corresponds roughly to zoom 16
        val continuousZoom = (15.0 + ln(zoomScale.toDouble() / 1.0) / ln(2.0)).coerceIn(10.0, 19.0)
        val discreteZoom = continuousZoom.toInt()
        val subScale = 2.0.pow(continuousZoom - discreteZoom).toFloat()

        val (centerWorldX, centerWorldY) = latLngToWorldPixel(centerLat, centerLng, discreteZoom.toDouble())

        val tileSize = 256f * subScale
        val canvasWidth = drawScope.size.width
        val canvasHeight = drawScope.size.height

        // Calculate tile range to cover the canvas viewport
        val minWorldX = centerWorldX - (centerCanvasX / subScale)
        val maxWorldX = centerWorldX + ((canvasWidth - centerCanvasX) / subScale)
        val minWorldY = centerWorldY - (centerCanvasY / subScale)
        val maxWorldY = centerWorldY + ((canvasHeight - centerCanvasY) / subScale)

        val minTileX = (minWorldX / 256.0).toInt().coerceAtLeast(0)
        val maxTileX = (maxWorldX / 256.0).toInt()
        val minTileY = (minWorldY / 256.0).toInt().coerceAtLeast(0)
        val maxTileY = (maxWorldY / 256.0).toInt()

        val maxTileIndex = (2.0.pow(discreteZoom) - 1).toInt()

        for (tileX in minTileX..maxTileX.coerceAtMost(maxTileIndex)) {
            for (tileY in minTileY..maxTileY.coerceAtMost(maxTileIndex)) {
                val tileKey = TileKey(discreteZoom, tileX, tileY, layerType)
                val cachedBitmap = memoryCache.get(tileKey)

                // Top-left pixel of this tile in canvas coordinates
                val tileWorldX = tileX * 256.0
                val tileWorldY = tileY * 256.0

                val canvasLeft = (centerCanvasX + (tileWorldX - centerWorldX) * subScale).toFloat()
                val canvasTop = (centerCanvasY + (tileWorldY - centerWorldY) * subScale).toFloat()

                if (cachedBitmap != null) {
                    drawScope.drawImage(
                        image = cachedBitmap,
                        dstOffset = IntOffset(canvasLeft.roundToInt(), canvasTop.roundToInt()),
                        dstSize = IntSize(tileSize.roundToInt(), tileSize.roundToInt())
                    )
                } else {
                    // Trigger async fetch
                    requestTile(tileKey)

                    // Check if parent zoom tile is in memory cache to avoid blank holes while loading
                    val parentZoom = discreteZoom - 1
                    if (parentZoom >= 10) {
                        val parentKey = TileKey(parentZoom, tileX / 2, tileY / 2, layerType)
                        val parentBitmap = memoryCache.get(parentKey)
                        if (parentBitmap != null) {
                            val subX = (tileX % 2) * 128
                            val subY = (tileY % 2) * 128
                            drawScope.drawImage(
                                image = parentBitmap,
                                srcOffset = IntOffset(subX, subY),
                                srcSize = IntSize(128, 128),
                                dstOffset = IntOffset(canvasLeft.roundToInt(), canvasTop.roundToInt()),
                                dstSize = IntSize(tileSize.roundToInt(), tileSize.roundToInt())
                            )
                        }
                    }
                }
            }
        }
    }
}
