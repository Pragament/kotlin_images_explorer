package com.pragament.kotlin_images_explorer

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import com.pragament.kotlin_images_explorer.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.compose.KoinContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class KotlinImagesExplorerApp: Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@KotlinImagesExplorerApp)
            androidLogger(Level.ERROR)
            modules(appModule)
        }
    }
}