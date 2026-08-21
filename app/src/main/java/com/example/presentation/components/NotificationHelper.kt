package com.example.presentation.components

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.core.util.MathUtils

/**
 * Push bildirim yardımcısı: whale/burst uyarıları için kanal kurar ve bildirim basar.
 * İzin yoksa veya bildirimler kapalıysa sessizce no-op.
 */
class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_WHALE = "piram_whale"
        const val CHANNEL_BURST = "piram_burst"
        private const val WHALE_ID = 1001
        private const val BURST_ID = 1002

        fun canNotify(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return false
            }
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    private val nm: NotificationManager? =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm != null) {
            val whale = NotificationChannel(
                CHANNEL_WHALE, "Whale Uyarıları", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Kurumsal hacim emirleri" }
            val burst = NotificationChannel(
                CHANNEL_BURST, "Salvo Uyarıları", NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Ani emir kümeleri" }
            nm.createNotificationChannel(whale)
            nm.createNotificationChannel(burst)
        }
    }

    fun postWhale(symbol: String, side: String, volume: Double, price: Double, value: Double) {
        if (!canNotify(context)) return
        val title = "🐋 Whale — $symbol"
        val text = "$side ${MathUtils.formatVolume(volume)} @ ${MathUtils.formatPrice(price)} — ${MathUtils.formatUsd(value)}"
        post(CHANNEL_WHALE, WHALE_ID, title, text)
    }

    fun postBurst(symbol: String, side: String, orderCount: Int, totalValue: Double) {
        if (!canNotify(context)) return
        val title = "⚡ Salvo — $symbol"
        val text = "$side $orderCount emir — ${MathUtils.formatUsd(totalValue)}"
        post(CHANNEL_BURST, BURST_ID, title, text)
    }

    private fun post(channel: String, id: Int, title: String, text: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }
}
