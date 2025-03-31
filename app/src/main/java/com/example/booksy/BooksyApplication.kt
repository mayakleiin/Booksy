package com.example.booksy

import android.app.Application
import com.squareup.picasso.LruCache
import com.squareup.picasso.Picasso
import java.io.File

class BooksyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val picassoBuilder = Picasso.Builder(this)
        picassoBuilder.memoryCache(LruCache(1024 * 1024 * 30)) // 30MB cache

        val cacheDir = File(cacheDir, "picasso-cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        val picassoInstance = picassoBuilder.build()
        Picasso.setSingletonInstance(picassoInstance)
    }
}
