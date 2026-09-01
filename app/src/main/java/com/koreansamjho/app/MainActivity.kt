package com.koreansamjho.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.koreansamjho.app.ui.AppViewModel
import com.koreansamjho.app.ui.SamjhoRoot
import com.koreansamjho.app.ui.samjhoFactory
import com.koreansamjho.app.ui.theme.SamjhoTheme

/**
 * Extends AppCompatActivity, not ComponentActivity, and that is deliberate: on API < 33
 * androidx applies the per-app locale through the AppCompat delegate's attachBaseContext.
 * With a plain ComponentActivity, in-app language switching silently fails below Android 13 —
 * which is most of our target devices. See docs/04-technical-architecture.md 4.5.
 */
class MainActivity : AppCompatActivity() {

    private val appVm: AppViewModel by viewModels {
        samjhoFactory { AppViewModel(it.settings) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var ready = false
        splash.setKeepOnScreenCondition { !ready }

        setContent {
            val settings by appVm.settings.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) { ready = true }

            // Keep the system locale in step with the in-app choice so resource
            // qualifiers and layout direction resolve correctly.
            LaunchedEffect(settings.lang) {
                val tag = settings.lang.code
                val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
                if (current != tag) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                }
            }

            SamjhoTheme(
                lang = settings.lang,
                themeMode = settings.themeMode,
                fontScale = settings.fontScale,
                reducedMotion = settings.reducedMotion,
                showRomanization = settings.showRomanization,
            ) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SamjhoRoot(appVm = appVm, settings = settings)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Voice data installed while we were backgrounded is not visible to a live
        // TTS engine, so re-check on every return to the foreground.
        (application as SamjhoApp).container.tts.refresh()
    }

    override fun onDestroy() {
        if (isFinishing) (application as SamjhoApp).container.tts.stop()
        super.onDestroy()
    }
}
