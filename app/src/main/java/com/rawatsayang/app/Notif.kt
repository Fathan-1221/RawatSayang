package com.rawatsayang.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.Calendar

object Pengingat {
    private const val CHANNEL = "obat"

    fun berikutnya(jam: String): Long {
        val (h, m) = jam.split(":").map { it.toInt() }
        val sekarang = System.currentTimeMillis()
        val c = Calendar.getInstance().apply {
            timeInMillis = sekarang
            set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (c.timeInMillis <= sekarang) c.add(Calendar.DAY_OF_YEAR, 1)
        return c.timeInMillis
    }

    private fun pi(ctx: Context, id: Long, nama: String, lansia: String, dosis: String, jam: String): PendingIntent {
        val i = Intent(ctx, AlarmReceiver::class.java)
            .putExtra("id", id).putExtra("nama", nama).putExtra("lansia", lansia)
            .putExtra("dosis", dosis).putExtra("jam", jam)
        return PendingIntent.getBroadcast(ctx, id.toInt(), i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun jadwalkan(ctx: Context, id: Long, nama: String, lansia: String, dosis: String, jam: String) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, berikutnya(jam), pi(ctx, id, nama, lansia, dosis, jam))
    }

    fun batalkan(ctx: Context, id: Long) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pi(ctx, id, "", "", "", "00:00"))
    }

    fun tampilkan(ctx: Context, id: Long, nama: String, lansia: String, dosis: String, jam: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Pengingat obat", NotificationManager.IMPORTANCE_HIGH))
        }
        val buka = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Waktunya obat: $nama")
            .setContentText("$lansia · $dosis · jadwal $jam")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(buka)
            .build()
        nm.notify(id.toInt(), n)
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getLongExtra("id", 0)
        val nama = intent.getStringExtra("nama") ?: return
        val lansia = intent.getStringExtra("lansia") ?: ""
        val dosis = intent.getStringExtra("dosis") ?: ""
        val jam = intent.getStringExtra("jam") ?: return
        Pengingat.tampilkan(ctx, id, nama, lansia, dosis, jam)
        Pengingat.jadwalkan(ctx, id, nama, lansia, dosis, jam)
    }
}
