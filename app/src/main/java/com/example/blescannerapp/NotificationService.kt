package com.example.blescannerapp

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Base64
import java.io.ByteArrayOutputStream
import android.content.Context


class NotificationService : NotificationListenerService() {

    companion object {
        var listener: ((String) -> Unit)? = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val extras = sbn.notification.extras

        val prefs = getSharedPreferences("ble_settings", Context.MODE_PRIVATE)
        val sendMaps = prefs.getBoolean("send_maps", true)
        val sendWhatsapp = prefs.getBoolean("send_whatsapp", true)

        val title = extras.getString("android.title")
        val text = extras.getCharSequence("android.text")?.toString()
        val subText = extras.getCharSequence("android.subText")?.toString()
        val bigText = extras.getCharSequence("android.bigText")?.toString()
        val summary = extras.getCharSequence("android.summaryText")?.toString()
        val infoText = extras.getCharSequence("android.infoText")?.toString()
        val textLines = extras.getCharSequenceArray("android.textLines")

        if (packageName == "com.google.android.apps.maps") {
            val messageBuilder = StringBuilder("Google Maps:\n")

            if (!title.isNullOrEmpty()) messageBuilder.append("Tujuan: $title\n")

            if (!text.isNullOrEmpty()) {
                val arahEmoji = getDirectionEmoji(text)
                messageBuilder.append("Arah: $text $arahEmoji\n")
            }

            if (!subText.isNullOrEmpty()) messageBuilder.append("Waktu: $subText\n")
            if (!summary.isNullOrEmpty()) messageBuilder.append("Info: $summary\n")
            if (!infoText.isNullOrEmpty()) messageBuilder.append("Jarak: $infoText\n")
            if (!bigText.isNullOrEmpty()) messageBuilder.append("Detail: $bigText\n")

            if (!textLines.isNullOrEmpty()) {
                messageBuilder.append("📚 Navigasi Berikutnya:\n")
                for (line in textLines) {
                    val arrow = getDirectionEmoji(line.toString())
                    messageBuilder.append(" - $line $arrow\n")
                }
            }

            // Ambil ikon panah dari notifikasi
            val arrowIconBase64 = getNotificationIconBase64(sbn)
            if (arrowIconBase64 != null) {
                messageBuilder.append("IconBase64: $arrowIconBase64\n")
            }

            val fullMessage = messageBuilder.toString().trim()
            Log.d("NotificationService", fullMessage)
            listener?.invoke(fullMessage)
        }

        else if (packageName == "com.whatsapp" && sendWhatsapp) {
            val message = "$title: $text"
            Log.d("NotificationService", "WhatsApp: $message")
            listener?.invoke("💬 WA - $message")
        }
    }

    private fun getNotificationIconBase64(sbn: StatusBarNotification): String? {
        return try {
            val context = applicationContext
            val resources = packageManager.getResourcesForApplication(sbn.packageName)
            val drawable = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                // API 23 ke atas
                sbn.notification.smallIcon?.loadDrawable(context)
            } else {
                // API 21-22 pakai resource ID lama
                val iconId = sbn.notification.icon
                if (iconId != 0) resources.getDrawable(iconId, null) else null
            }

            if (drawable is BitmapDrawable) {
                val bitmap = drawable.bitmap
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("NotificationService", "Error getNotificationIconBase64", e)
            null
        }
    }

    private fun getDirectionEmoji(text: String): String {
        val lowercase = text.lowercase()
        return when {
            "belok kiri" in lowercase -> "⬅️"
            "belok kanan" in lowercase -> "➡️"
            "lurus" in lowercase -> "⬆️"
            "putar balik" in lowercase -> "↩️"
            "menuju" in lowercase -> "🧭"
            "di bundaran" in lowercase -> "⭕"
            "tiba di" in lowercase -> "🏁"
            else -> ""
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}
}
