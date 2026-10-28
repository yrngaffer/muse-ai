package com.muse.companion

import android.app.Application
import com.muse.companion.di.AppContainer

class MuseApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
