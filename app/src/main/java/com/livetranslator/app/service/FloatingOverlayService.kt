package com.livetranslator.app.service

import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.livetranslator.app.translation.LanguagePair
import com.livetranslator.app.translation.TranslationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FloatingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var tvOriginal: TextView? = null
    private var tvTranslated: TextView? = null
    private var btnCopy: Button? = null
    private var btnClose: Button? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var translationJob: Job? = null
    private var lastTranslatedText = ""

    companion object {
        var translationManager: TranslationManager? = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        setupFloatingWindow()
        observeTranscripts()
    }

    private fun setupFloatingWindow() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 150 // Offset from bottom so it doesn't obstruct TikTok control buttons
        }

        // Inflate or programmatically build floating overlay layout
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(0xEE1E1E2E.toInt()) // Sleek dark semi-transparent background
            setPadding(32, 24, 32, 24)
        }

        // Header with title and controls
        val header = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val tvTitle = TextView(this).apply {
            text = "⚡ Uživo Prevodilac"
            setTextColor(0xFF89B4FA.toInt())
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        btnCopy = Button(this).apply {
            text = "📋 Kopiraj"
            textSize = 12f
            setOnClickListener { copyToClipboard(lastTranslatedText) }
        }

        btnClose = Button(this).apply {
            text = "✕"
            textSize = 12f
            setOnClickListener { stopSelf() }
        }

        header.addView(tvTitle)
        header.addView(btnCopy)
        header.addView(btnClose)

        // Text views
        tvOriginal = TextView(this).apply {
            text = "Slušam audio iz aplikacije..."
            setTextColor(0xFFA6ADC8.toInt())
            textSize = 13f
            setPadding(0, 8, 0, 4)
        }

        tvTranslated = TextView(this).apply {
            text = "Prevedeni tekst pojavit će se ovdje..."
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 4, 0, 8)
        }

        container.addView(header)
        container.addView(tvOriginal)
        container.addView(tvTranslated)

        // Make window draggable
        setupDraggable(container)

        overlayView = container
        windowManager.addView(overlayView, layoutParams)
    }

    private fun setupDraggable(view: View) {
        var initialY = 0
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialY = layoutParams?.y ?: 0
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams?.y = initialY - (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(overlayView, layoutParams)
                    true
                }
                else -> false
            }
        }
    }

    private fun observeTranscripts() {
        val engine = AudioCaptureService.speechEngine ?: return
        val manager = translationManager ?: return

        translationJob = serviceScope.launch {
            engine.transcriptFlow.collectLatest { text ->
                tvOriginal?.text = text
                val translated = manager.translateText(text)
                lastTranslatedText = translated
                tvTranslated?.text = translated
            }
        }
    }

    private fun copyToClipboard(text: String) {
        if (text.isBlank()) {
            Toast.makeText(this, "Nema teksta za kopiranje", Toast.LENGTH_SHORT).show()
            return
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Prevedeni Tekst", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Prevedeni tekst kopiran u clipboard!", Toast.LENGTH_SHORT).show()
    }

    /**
     * Call to update translated text externally if needed
     */
    fun updateText(original: String, translated: String) {
        tvOriginal?.text = original
        tvTranslated?.text = translated
        lastTranslatedText = translated
    }

    override fun onDestroy() {
        super.onDestroy()
        translationJob?.cancel()
        if (overlayView != null) {
            windowManager.removeView(overlayView)
            overlayView = null
        }
    }
}
