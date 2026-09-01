package com.koreansamjho.app.data.audio

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class TtsStatus { INITIALISING, READY, MISSING_KOREAN_VOICE, NO_ENGINE }

/**
 * Korean speech via the on-device platform TTS engine.
 *
 * Chosen over cloud TTS (per-character billing, needs network) and bundled audio
 * (hundreds of MB of APK). Costs nothing, works offline once the Korean voice is
 * installed, and gives slow playback for free via speech rate.
 *
 * Three things here are the result of testing on a real handset:
 *
 *  1. The language check is posted to the main looper rather than run directly in
 *     onInit. TextToSpeech may invoke its init listener before the constructor has
 *     returned, so [tts] can still be null inside the callback — which would have
 *     been misreported as NO_ENGINE on an otherwise healthy device.
 *  2. [refresh] re-creates the engine. Voice data installed while the app is running
 *     is not picked up by a live engine instance, so without this the user installs
 *     the Korean voice, comes back, and audio is still dead until a cold restart.
 *  3. A missing voice is a recoverable state with an action attached
 *     ([installKoreanVoice]), not a dead button.
 */
class TtsController(context: Context) {

    private companion object { const val TAG = "SamjhoTts" }

    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())

    private val _status = MutableStateFlow(TtsStatus.INITIALISING)
    val status: StateFlow<TtsStatus> = _status.asStateFlow()

    private val _speakingId = MutableStateFlow<String?>(null)
    val speakingId: StateFlow<String?> = _speakingId.asStateFlow()

    private var tts: TextToSpeech? = null

    init { initEngine() }

    private fun initEngine() {
        _status.value = TtsStatus.INITIALISING
        tts = TextToSpeech(appContext) { status ->
            if (status != TextToSpeech.SUCCESS) {
                _status.value = TtsStatus.NO_ENGINE
            } else {
                // Posted so the `tts` field assignment above has definitely completed.
                main.post { applyLanguage() }
            }
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { _speakingId.value = utteranceId }
            override fun onDone(utteranceId: String?) { _speakingId.value = null }
            @Deprecated("Deprecated in Java", ReplaceWith(""))
            override fun onError(utteranceId: String?) {
                _speakingId.value = null
                // Synthesis failed at runtime even though the engine claimed support —
                // treat it as a missing voice so the UI offers the install action
                // instead of appearing to do nothing.
                Log.w(TAG, "korean synthesis failed for utterance $utteranceId")
                _status.value = TtsStatus.MISSING_KOREAN_VOICE
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _speakingId.value = null
                Log.w(TAG, "korean synthesis failed for $utteranceId (code $errorCode)")
                _status.value = TtsStatus.MISSING_KOREAN_VOICE
            }
        })
    }

    /**
     * setLanguage() alone is not trustworthy. Google TTS reports Korean as available
     * on a device that has no Korean voice data, because it *could* fetch it — then
     * synthesis silently produces nothing offline, which reads to the user as
     * "the audio button does nothing". So availability is decided by three signals:
     * the setLanguage result, whether the engine actually exposes a Korean voice, and
     * whether that voice is flagged as not-installed.
     */
    private fun applyLanguage() {
        val engine = tts
        if (engine == null) { _status.value = TtsStatus.NO_ENGINE; return }

        val setResult = runCatching { engine.setLanguage(Locale.KOREAN) }.getOrNull()

        val koreanVoices = runCatching {
            engine.voices.orEmpty().filter { it.locale?.language == Locale.KOREAN.language }
        }.getOrDefault(emptyList())

        // The app requests no INTERNET permission, so a network-backed voice can never
        // synthesise here — it must be an embedded voice that is actually installed.
        // Observed on a Redmi 13: setLanguage returned 0 (available) while the only
        // Korean voice was "ko-kr-x-koc-network", notInstalled, network-required.
        // Trusting setLanguage alone produced a button that silently did nothing.
        val usable = koreanVoices.firstOrNull { v ->
            val needsNetwork = runCatching { v.isNetworkConnectionRequired }.getOrDefault(true)
            val missing = runCatching {
                v.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) == true
            }.getOrDefault(true)
            !needsNetwork && !missing
        }

        Log.i(TAG, "korean tts: setLanguage=$setResult voices=${koreanVoices.map { it.name }} " +
            "usableOffline=${usable?.name}")

        if (usable != null) runCatching { engine.voice = usable }

        _status.value = when {
            setResult == null -> TtsStatus.NO_ENGINE
            setResult == TextToSpeech.LANG_MISSING_DATA ||
                setResult == TextToSpeech.LANG_NOT_SUPPORTED -> TtsStatus.MISSING_KOREAN_VOICE
            usable == null -> TtsStatus.MISSING_KOREAN_VOICE
            else -> TtsStatus.READY
        }
    }

    /**
     * Re-checks Korean availability, rebuilding the engine so voice data installed
     * since startup is picked up. Called when the app returns to the foreground.
     */
    fun refresh() {
        if (_status.value == TtsStatus.READY) return
        runCatching { tts?.stop(); tts?.shutdown() }
        tts = null
        initEngine()
    }

    val isReady: Boolean get() = _status.value == TtsStatus.READY

    /** @param slow uses a reduced speech rate for pronunciation practice. */
    fun speak(text: String, id: String = text, slow: Boolean = false) {
        val engine = tts ?: return
        if (_status.value != TtsStatus.READY) return
        engine.setSpeechRate(if (slow) 0.55f else 1.0f)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    fun stop() { runCatching { tts?.stop() }; _speakingId.value = null }

    fun shutdown() { runCatching { tts?.stop(); tts?.shutdown() }; tts = null }

    /**
     * Opens the engine's voice-data download screen. This is a direct deep link to the
     * language list, which is far shorter than walking a user through
     * Settings → Accessibility → Text-to-speech → engine → install voice data.
     */
    fun installKoreanVoice(ctx: Context): Boolean {
        val engine = runCatching { tts?.defaultEngine }.getOrNull()
        val intents = listOfNotNull(
            Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                .also { if (engine != null) it.setPackage(engine) },
            Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
            Intent("com.android.settings.TTS_SETTINGS"),
        )
        for (i in intents) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { ctx.startActivity(i) }.isSuccess) return true
        }
        return false
    }
}
