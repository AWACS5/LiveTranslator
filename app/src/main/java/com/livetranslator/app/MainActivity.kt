package com.livetranslator.app

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.livetranslator.app.service.AudioCaptureService
import com.livetranslator.app.service.FloatingOverlayService
import com.livetranslator.app.speech.SpeechToTextEngine
import com.livetranslator.app.translation.TranslationManager
import com.livetranslator.app.ui.ComposableMainScreen

class MainActivity : ComponentActivity() {

    private lateinit var translationManager: TranslationManager
    private lateinit var speechEngine: SpeechToTextEngine

    private var isServiceRunning by mutableStateOf(false)
    private var hasOverlayPermission by mutableStateOf(false)

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            startLiveServices(result.resultCode, result.data!!)
        } else {
            Toast.makeText(this, "Dozvola za snimanje audija odbijena", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        translationManager = TranslationManager(this)
        speechEngine = SpeechToTextEngine(this)
        
        AudioCaptureService.speechEngine = speechEngine
        FloatingOverlayService.translationManager = translationManager

        checkPermissionsState()

        setContent {
            ComposableMainScreen(
                translationManager = translationManager,
                isServiceRunning = isServiceRunning,
                onStartServiceClicked = { requestAudioCapture() },
                onStopServiceClicked = { stopLiveServices() },
                onRequestOverlayPermissionClicked = { requestOverlayPermission() },
                hasOverlayPermission = hasOverlayPermission
            )
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsState()
    }

    private fun checkPermissionsState() {
        hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun requestAudioCapture() {
        if (!hasOverlayPermission) {
            requestOverlayPermission()
            return
        }

        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        mediaProjectionLauncher.launch(intent)
    }

    private fun startLiveServices(resultCode: Int, data: Intent) {
        // Start Audio Capture Service
        val captureIntent = Intent(this, AudioCaptureService::class.java).apply {
            putExtra(AudioCaptureService.EXTRA_RESULT_CODE, resultCode)
            putExtra(AudioCaptureService.EXTRA_RESULT_DATA, data)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(captureIntent)
        } else {
            startService(captureIntent)
        }

        // Start Floating Overlay Service
        val overlayIntent = Intent(this, FloatingOverlayService::class.java)
        startService(overlayIntent)

        isServiceRunning = true
        Toast.makeText(this, "Prevođenje uživo pokrenuto!", Toast.LENGTH_SHORT).show()
    }

    private fun stopLiveServices() {
        stopService(Intent(this, AudioCaptureService::class.java))
        stopService(Intent(this, FloatingOverlayService::class.java))
        isServiceRunning = false
        Toast.makeText(this, "Prevođenje uživo zaustavljeno", Toast.LENGTH_SHORT).show()
    }
}
