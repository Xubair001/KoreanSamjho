package com.koreansamjho.app.di

import android.content.Context
import androidx.room.Room
import com.koreansamjho.app.data.audio.TtsController
import com.koreansamjho.app.data.content.ContentDb
import com.koreansamjho.app.data.prefs.SettingsStore
import com.koreansamjho.app.data.progress.ProgressDatabase
import com.koreansamjho.app.data.repository.LearningRepository

/**
 * Manual dependency injection.
 *
 * Hilt was rejected: Room already requires KSP, and adding a second annotation
 * processor plus startup reflection does not pay for itself in a single-module app.
 * Constructor injection is still dependency injection — the framework is optional.
 * See docs/04-technical-architecture.md 4.3.
 */
class AppContainer(private val context: Context) {

    private val progressDb: ProgressDatabase by lazy {
        Room.databaseBuilder(context, ProgressDatabase::class.java, "progress.db")
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    val contentDb: ContentDb by lazy { ContentDb(context) }
    val settings: SettingsStore by lazy { SettingsStore(context) }
    val tts: TtsController by lazy { TtsController(context) }
    val repository: LearningRepository by lazy { LearningRepository(contentDb, progressDb.dao()) }
}
