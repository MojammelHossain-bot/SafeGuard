package com.example

import android.content.Intent
import android.net.VpnService

class BlockerVpnService : VpnService() {

    // ব্লক করা ডোমেইন লিস্ট
    val blockedDomains = listOf(
        "pornhub.com",
        "xnxx.com",
        "xvideos.com",
        "xhamster.com",
        "redtube.com",
        "youporn.com",
        "brazzers.com",
        "tube8.com"
        // আরও যোগ করুন
    )

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        val builder = Builder()
        builder.setMtu(1500)
        builder.addAddress("10.0.0.2", 32)

        // সব DNS রিকোয়েস্ট আটকাও
        builder.addDnsServer("10.0.0.1")

        // SafeDNS / CleanBrowsing / Cloudflare / AdGuard (Adult & Malware block)
        builder.addDnsServer("185.228.168.168") // CleanBrowsing Family
        builder.addDnsServer("1.1.1.3")         // Cloudflare Family
        builder.addDnsServer("94.140.14.15")    // AdGuard Family

        builder.addRoute("0.0.0.0", 0)
        
        try {
            builder.establish()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
