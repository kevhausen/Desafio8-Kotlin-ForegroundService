package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

private const val CHANNEL_ID = "ForegroundService Kotlin"

class ForegroundService : Service() {

    private val mHandler: Handler? = Handler()
    private lateinit var mRunnable: Runnable
    private var contador:Int = 0

    companion object {
        var running = false
        private lateinit var handlerCallback: Handler

        fun startService(
            context: Context,
            message: String,
            handler: Handler
        ) {
            val startIntent = Intent(context, ForegroundService::class.java)
            startIntent.putExtra("inputExtra", message)
            ContextCompat.startForegroundService(context, startIntent)
            running = true
            handlerCallback = handler
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, ForegroundService::class.java)
            context.stopService(stopIntent)
            running = false
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        // No estamos utilizando un servicio de enlace, por lo que devolvemos null
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val input = intent?.getStringExtra("inputExtra") ?: ""


        createNotificationChannel(input)

        // A ejecutar una tarea
        runTask()

        // El servicio solo se reinicia si el tiempo de ejecución tiene llamadas
        // pendientes de startService() desde la finalización del servicio.
        return START_NOT_STICKY
    }

    /**
     * Simula una tarea corriendo en segundo plano de forma indefinida hasta ser detenido explícitamente
     */
    private fun runTask() {
        val delayMillis = 1000 * 1L // 6 segundos
        mRunnable = Runnable {
            notifyNextEvent()
            mHandler?.postDelayed(mRunnable, delayMillis)
            contador++
        };

        // Schedule the task to repeat after delay seconds
        // Tarea agendada que se repite despues del tiempo indicado por delayMillis
        mHandler?.postDelayed(
            mRunnable,
            delayMillis // intervalo entre ejecuciones
        )
    }

    private fun createNotificationChannel(input: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, getString(R.string.notification_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager!!.createNotificationChannel(serviceChannel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, // Context
            0, // request code
            notificationIntent, // Intent con la notificación
            0
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.title))
            .setContentText(input)
            .setSmallIcon(R.drawable.ic_android)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)
    }

    private fun notifyNextEvent() {
        // Ocurre un evento en el servicio y se hace alguna acción
        val message = handlerCallback.obtainMessage(1, "msg")
        message.data.putString("Estado", "Tarea ejecutada con éxito")
        message.data.putInt("contador",contador)
        message.sendToTarget()
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler!!.removeCallbacks(mRunnable)
    }
}