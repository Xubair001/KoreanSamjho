package com.koreansamjho.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.koreansamjho.app.di.AppContainer

class SamjhoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Copy and open the content database off the main thread so first frame is not blocked.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { container.contentDb.warmUp() }
        }
    }

    override fun onTerminate() {
        container.tts.shutdown()
        super.onTerminate()
    }
}
