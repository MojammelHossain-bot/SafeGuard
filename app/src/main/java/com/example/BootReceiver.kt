package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("safeguard_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("vpn_enabled", false)) {
                try {
                    val vpnIntent = VpnService.prepare(context)
                    if (vpnIntent == null) {
                        context.startService(Intent(context, BlockerVpnService::class.java))
                    }
                } catch (e: Exception) {
                    // Ignore, might be emulator limitation
                }
            }
        }
    }
}
