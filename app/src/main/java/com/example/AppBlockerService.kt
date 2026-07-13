package com.example

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppBlockerService : AccessibilityService() {

    val blockedApps = listOf(
        "com.vidmate.app",
        "com.nhn.android.search",
        "org.xnxx",
        "com.xvideos",
        "com.pornhub.app",
        "com.xnxx.app",
        "com.bigo.live",
        "com.kwai.video",
        "com.snapchat.android", // Example additional
        "com.zhiliaoapp.musically" // TikTok
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return

        if (blockedApps.contains(packageName)) {
            performGlobalAction(GLOBAL_ACTION_HOME)
            showBlockedWarning("This app is blocked by SafeGuard!")
        }

        if (packageName == "com.android.chrome" || 
            packageName == "org.mozilla.firefox" || 
            packageName == "com.opera.browser" || 
            packageName == "com.microsoft.emmx") {
            checkChromeUrl(event)
        }
        
        // Prevent uninstalling or disabling SafeGuard
        if (packageName == "com.android.settings" || packageName.contains("packageinstaller")) {
            blockSettingsModification()
        }
    }

    override fun onInterrupt() {
    }

    private fun checkChromeUrl(event: AccessibilityEvent?) {
        val source = event?.source ?: return
        if (source.text != null) {
             val url = source.text.toString()
             val blockedKeywords = listOf(
                 "porn", "xxx", "xnxx", "xvideos",
                 "pornhub", "sex", "nude", "brazzers",
                 "redtube", "youporn", "xhamster", "onlyfans"
             )

             blockedKeywords.forEach { keyword ->
                 if (url.contains(keyword, ignoreCase = true)) {
                     performGlobalAction(GLOBAL_ACTION_HOME)
                     showBlockedWarning("Adult content is blocked by SafeGuard!")
                     return
                 }
             }
        }
    }
    
    private fun blockSettingsModification() {
        val rootNode = rootInActiveWindow ?: return
        
        // Check if the screen contains "SafeGuard"
        val isSafeGuardScreen = rootNode.findAccessibilityNodeInfosByText("SafeGuard").isNotEmpty()
        
        if (isSafeGuardScreen) {
            val hasUninstall = rootNode.findAccessibilityNodeInfosByText("Uninstall").isNotEmpty() || 
                               rootNode.findAccessibilityNodeInfosByText("Uninstaller").isNotEmpty()
            val hasDisable = rootNode.findAccessibilityNodeInfosByText("Disable").isNotEmpty()
            val hasDeactivate = rootNode.findAccessibilityNodeInfosByText("Deactivate").isNotEmpty()
            val hasForceStop = rootNode.findAccessibilityNodeInfosByText("Force stop").isNotEmpty() ||
                               rootNode.findAccessibilityNodeInfosByText("Stop").isNotEmpty()
            val hasClearData = rootNode.findAccessibilityNodeInfosByText("Clear data").isNotEmpty() ||
                               rootNode.findAccessibilityNodeInfosByText("Clear storage").isNotEmpty()
            
            if (hasUninstall || hasDisable || hasDeactivate || hasForceStop || hasClearData) {
                performGlobalAction(GLOBAL_ACTION_HOME)
                showBlockedWarning("You cannot disable or modify SafeGuard without permission!")
            }
        }
    }
    
    private fun showBlockedWarning(message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }
}
