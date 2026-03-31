package com.internal.wamessenger.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

object WhatsAppLauncher {

    private const val WA_PACKAGE = "com.whatsapp"

    fun isWhatsAppInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(WA_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isWhatsAppLoggedIn(context: Context): Boolean {
        // Best-effort: if WA is installed, we assume logged in.
        // The accessibility service will catch the login screen at runtime.
        return isWhatsAppInstalled(context)
    }

    /**
     * Opens WhatsApp chat with pre-filled message using wa.me deep link.
     * Returns true if intent resolved successfully.
     */
    fun openChat(context: Context, phone: String, message: String): Boolean {
        if (!isWhatsAppInstalled(context)) return false

        return try {
            val encodedMsg = Uri.encode(message)
            val url = "https://wa.me/$phone?text=$encodedMsg"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage(WA_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Opens the main WhatsApp screen (fallback)
     */
    fun openWhatsApp(context: Context) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(WA_PACKAGE)
                ?: return
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // ignore
        }
    }
}
