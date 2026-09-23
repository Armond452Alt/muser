package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.audio.AudioEngine
import com.example.audio.AudioEngineState

private const val TAG = "AudioRoutingService"
private const val CHANNEL_ID = "muser_audio_channel"
private const val NOTIFICATION_ID = 101

const val ACTION_START_STREAMING = "com.example.muser.ACTION_START_STREAMING"
const val ACTION_TOGGLE_MUTE = "com.example.muser.ACTION_TOGGLE_MUTE"
const val ACTION_STOP_SERVICE = "com.example.muser.ACTION_STOP_SERVICE"

class AudioRoutingService : Service() {

    private val binder = LocalBinder()
    val audioEngine: AudioEngine by lazy {
        AudioEngine.getInstance(applicationContext)
    }

    private var headsetPlugReceiver: BroadcastReceiver? = null
    private var isForegroundActive = false

    inner class LocalBinder : Binder() {
        fun getService(): AudioRoutingService = this@AudioRoutingService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerHeadsetPlugReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_STREAMING -> {
                startForegroundWithNotification()
            }
            ACTION_TOGGLE_MUTE -> {
                val newMute = !audioEngine.state.isMuted
                audioEngine.setMuted(newMute)
                updateNotification()
            }
            ACTION_STOP_SERVICE -> {
                audioEngine.stop()
                stopForegroundNotification()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun startForegroundWithNotification() {
        val hasMicPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMicPermission) {
            Log.w(TAG, "Cannot start microphone foreground service: RECORD_AUDIO permission not granted yet")
            return
        }

        try {
            val notification = buildNotification(audioEngine.state)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                } else {
                    0
                }
                startForeground(NOTIFICATION_ID, notification, serviceType)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            isForegroundActive = true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to start foreground service (ignoring so audio continues uninterrupted)", e)
            isForegroundActive = false
        }
    }

    fun stopForegroundNotification() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            isForegroundActive = false
        } catch (e: Throwable) {
            Log.e(TAG, "Error stopping foreground", e)
        }
    }

    fun updateNotification() {
        if (!isForegroundActive) return
        try {
            val notification = buildNotification(audioEngine.state)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to update notification", e)
        }
    }

    private fun buildNotification(state: AudioEngineState): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleMuteIntent = Intent(this, AudioRoutingService::class.java).apply {
            action = ACTION_TOGGLE_MUTE
        }
        val pendingMute = PendingIntent.getService(
            this, 1, toggleMuteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AudioRoutingService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val pendingStop = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = when {
            state.isMuted -> "🔴 MUTED - Party chat cannot hear you"
            state.isRunning -> "🟢 LIVE - Transmitting via ${state.outputRoute.title}"
            else -> "⚪ Idle - Ready to connect"
        }

        val muteActionTitle = if (state.isMuted) "Unmute" else "Mute"
        val muteIcon = if (state.isMuted) R.drawable.ic_stat_mic else R.drawable.ic_stat_mic_off

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Muser Console Mic")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_stat_mic)
            .setContentIntent(pendingOpenApp)
            .setOngoing(state.isRunning)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(muteIcon, muteActionTitle, pendingMute)
            .addAction(R.drawable.ic_stat_stop, "Disconnect", pendingStop)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Muser Audio Stream",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live console microphone streaming status and controls"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun registerHeadsetPlugReceiver() {
        headsetPlugReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_HEADSET_PLUG) {
                    val isPlugged = intent.getIntExtra("state", 0) == 1
                    audioEngine.setAuxPlugged(isPlugged)
                }
            }
        }
        try {
            registerReceiver(headsetPlugReceiver, IntentFilter(Intent.ACTION_HEADSET_PLUG))
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to register headset plug receiver", e)
        }
    }

    override fun onDestroy() {
        try {
            audioEngine.stop()
        } catch (ignored: Throwable) {}

        headsetPlugReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (ignored: Throwable) {}
        }
        super.onDestroy()
    }
}
