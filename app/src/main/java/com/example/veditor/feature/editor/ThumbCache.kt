package com.example.veditor.feature.editor

import android.graphics.Bitmap
import android.util.LruCache

// 메모리 썸네일 캐시 (바이트 수 기준)
object ThumbMemoryCache {
    private val lru = object : LruCache<String, Bitmap>(8 * 1024 * 1024) { // 8MB
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    fun get(key: String): Bitmap? = lru.get(key)
    fun put(key: String, value: Bitmap) { lru.put(key, value) }
}


