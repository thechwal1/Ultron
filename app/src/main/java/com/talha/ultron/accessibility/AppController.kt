package com.talha.ultron.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityNodeInfo

class AppController(val context: Context) {

    fun openSettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun tap(target: String): String {
        val service = UltronAccessibilityService.instance ?: return "Accessibility service not connected."
        val root = service.rootInActiveWindow ?: return "No active window found."
        val node = findNodeByText(root, target)
        return if (node != null && node.isClickable) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            "Tapped on $target."
        } else {
            "I couldn't find a clickable element called '$target'."
        }
    }

    fun scroll(direction: String): String {
        val service = UltronAccessibilityService.instance ?: return "Accessibility service not connected."
        val root = service.rootInActiveWindow ?: return "No active window found."
        val action = when (direction) {
            "up" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            "down" -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            else -> return "I can only scroll up or down."
        }
        val scrollable = findScrollableNode(root)
        return if (scrollable?.performAction(action) == true) {
            "Scrolled $direction."
        } else {
            "Couldn't scroll $direction."
        }
    }

    fun type(text: String): String {
        val service = UltronAccessibilityService.instance ?: return "Accessibility service not connected."
        val root = service.rootInActiveWindow ?: return "No active window found."
        val focused = findFocusedEditText(root)
        return if (focused != null) {
            val args = android.os.Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            "Typed '$text'."
        } else {
            "No text field is currently focused."
        }
    }

    fun goBack(): String {
        val service = UltronAccessibilityService.instance ?: return "Accessibility service not connected."
        return if (service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)) {
            "Going back."
        } else {
            "Couldn't go back."
        }
    }

    fun goHome(): String {
        val service = UltronAccessibilityService.instance ?: return "Accessibility service not connected."
        return if (service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)) {
            "Going home."
        } else {
            "Couldn't go home."
        }
    }

    fun openRecents(): String {
        val service = UltronAccessibilityService.instance ?: return "Accessibility service not connected."
        return if (service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)) {
            "Opening recent apps."
        } else {
            "Couldn't open recent apps."
        }
    }

    fun readScreen(): String {
        val service = UltronAccessibilityService.instance ?: return "Accessibility service not connected."
        val root = service.rootInActiveWindow ?: return "No active window found."
        val text = collectAllText(root)
        return if (text.isNotBlank()) text else "The screen appears to be empty."
    }

    fun isVisible(target: String): String {
        val service = UltronAccessibilityService.instance ?: return "Accessibility service not connected."
        val root = service.rootInActiveWindow ?: return "No active window found."
        val node = findNodeByText(root, target)
        return if (node != null) "Yes, I can see '$target' on the screen."
        else "No, I don't see '$target' on the screen."
    }

    private fun findNodeByText(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val queue = java.util.ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.poll()
            if (node.text?.toString()?.contains(text, ignoreCase = true) == true ||
                node.contentDescription?.toString()?.contains(text, ignoreCase = true) == true) {
                return node
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun findScrollableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = java.util.ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.poll()
            if (node.isScrollable) return node
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun findFocusedEditText(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = java.util.ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.poll()
            if (node.isFocused && node.className?.contains("EditText") == true) return node
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun collectAllText(root: AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        val queue = java.util.ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.poll()
            node.text?.toString()?.let { if (it.isNotBlank()) sb.append(it).append(". ") }
            node.contentDescription?.toString()?.let { if (it.isNotBlank()) sb.append(it).append(". ") }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return sb.toString().trim()
    }
}
