package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ZoyaAccessibilityService : AccessibilityService() {

    companion object {
        var shouldAutoClick = false
            set(value) {
                field = value
                if (value) {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        field = false
                    }, 10000) // Reset after 10 seconds to give WhatsApp time to load
                }
            }
        var targetAppName = "whatsapp"
        var instance: ZoyaAccessibilityService? = null

        fun dispatchGestureClick(x: Float, y: Float): Boolean {
            val inst = instance ?: return false
            val path = android.graphics.Path()
            path.moveTo(x, y)
            path.lineTo(x, y)

            val builder = android.accessibilityservice.GestureDescription.Builder()
            builder.addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 50))
            val gesture = builder.build()

            return inst.dispatchGesture(gesture, null, null)
        }

        fun clickTextOnScreen(text: String): Boolean {
            val inst = instance ?: return false
            val root = inst.rootInActiveWindow ?: return false
            val nodes = root.findAccessibilityNodeInfosByText(text)
            for (node in nodes) {
                var current: AccessibilityNodeInfo? = node
                while(current != null) {
                    val bounds = android.graphics.Rect()
                    current.getBoundsInScreen(bounds)
                    if (!bounds.isEmpty && (current.isClickable || current == node)) {
                        val x = bounds.centerX().toFloat()
                        val y = bounds.centerY().toFloat()
                        if (dispatchGestureClick(x, y)) return true
                    }
                    current = current.parent
                }
            }
            return false
        }

        fun performSwipe(direction: String): Boolean {
            val inst = instance ?: return false
            val metrics = inst.resources.displayMetrics
            val width = metrics.widthPixels.toFloat()
            val height = metrics.heightPixels.toFloat()

            val startX: Float
            val startY: Float
            val endX: Float
            val endY: Float

            when (direction.lowercase()) {
                "up", "scroll_up", "downward" -> {
                    // Swipe finger up (scrolls down)
                    startX = width / 2f
                    startY = height * 0.75f
                    endX = width / 2f
                    endY = height * 0.25f
                }
                "down", "scroll_down", "upward" -> {
                    // Swipe finger down (scrolls up)
                    startX = width / 2f
                    startY = height * 0.25f
                    endX = width / 2f
                    endY = height * 0.75f
                }
                "left" -> {
                    startX = width * 0.85f
                    startY = height / 2f
                    endX = width * 0.15f
                    endY = height / 2f
                }
                "right" -> {
                    startX = width * 0.15f
                    startY = height / 2f
                    endX = width * 0.85f
                    endY = height / 2f
                }
                else -> return false
            }

            val path = android.graphics.Path()
            path.moveTo(startX, startY)
            path.lineTo(endX, endY)

            val builder = android.accessibilityservice.GestureDescription.Builder()
            builder.addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 300))
            val gesture = builder.build()
            return inst.dispatchGesture(gesture, null, null)
        }

        fun performSystemNav(action: String): Boolean {
            val inst = instance ?: return false
            val globalAction = when (action.lowercase()) {
                "back" -> AccessibilityService.GLOBAL_ACTION_BACK
                "home" -> AccessibilityService.GLOBAL_ACTION_HOME
                "recents", "recent_apps" -> AccessibilityService.GLOBAL_ACTION_RECENTS
                "lock", "lock_screen" -> AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
                "screenshot", "take_screenshot" -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT
                } else AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
                else -> return false
            }
            return inst.performGlobalAction(globalAction)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("ZoyaAccessibility", "Accessibility Service Connected")
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !shouldAutoClick) return
        
        val packageName = event.packageName?.toString() ?: ""
        if (packageName.contains("whatsapp")) {
            
            val rootNode = rootInActiveWindow ?: return
            
            val clicked = searchAndClickSendButton(rootNode)
            if (clicked) {
                Log.d("ZoyaAccessibility", "Successfully clicked send button!")
                shouldAutoClick = false
            }
        }
    }

    private fun searchAndClickSendButton(node: AccessibilityNodeInfo): Boolean {
        // Attempt 1: by common View IDs
        val idsToTry = listOf(
            "com.whatsapp:id/send",
            "com.whatsapp.w4b:id/send"
        )
        for (id in idsToTry) {
            val sendButtons = node.findAccessibilityNodeInfosByViewId(id)
            if (sendButtons.isNotEmpty()) {
                for (button in sendButtons) {
                    if (performClick(button)) {
                        Log.d("ZoyaAccessibility", "Clicked send button by ID: $id")
                        return true
                    }
                }
            }
        }

        // Attempt 2: Recursive search for Content Description "Send", "Bhejen", etc
        return recursiveSearchAndClick(node)
    }

    private fun recursiveSearchAndClick(node: AccessibilityNodeInfo): Boolean {
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        
        if (desc == "send" || desc == "bheje" || desc == "bhejen" || desc == "envio") {
            if (performClick(node)) {
                Log.d("ZoyaAccessibility", "Clicked send button by content description!")
                return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                if (recursiveSearchAndClick(child)) {
                    return true
                }
            }
        }
        return false
    }

    private fun performClick(node: AccessibilityNodeInfo): Boolean {
        // Real visual click first
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)
        if (!bounds.isEmpty) {
            val x = bounds.centerX().toFloat()
            val y = bounds.centerY().toFloat()
            if (dispatchGestureClick(x, y)) {
                return true
            }
        }

        if (node.isClickable) {
            val success = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (success) return true
        }
        // Try parent if not clickable
        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable) {
                val success = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (success) return true
            }
            parent = parent.parent
        }
        return false
    }

    override fun onInterrupt() {
        Log.d("ZoyaAccessibility", "Accessibility Service Interrupted")
    }
}
