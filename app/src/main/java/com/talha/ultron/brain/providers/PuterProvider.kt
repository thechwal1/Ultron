package com.talha.ultron.brain.providers

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.talha.ultron.SecureSettings
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PuterProvider(context: Context) : AiProvider {
    override val id = "puter"
    override val displayName = "Puter.js (free, no key)"
    override val needsApiKey = false
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private var pageReady = false
    private val pageReadyQueue = mutableListOf<() -> Unit>()
    private val pending = ConcurrentHashMap<Long, Continuation<String>>()
    private val nextId = AtomicLong(0)
    init { mainHandler.post { setupWebView() } }
    override fun isReady(settings: SecureSettings): Boolean = true
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val wv = WebView(appContext)
        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.addJavascriptInterface(Bridge(), "AndroidBridge")
        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                pageReady = true
                pageReadyQueue.forEach { it() }
                pageReadyQueue.clear()
            }
        }
        wv.loadUrl("file:///android_asset/puter_bridge.html")
        webView = wv
    }
    override suspend fun send(userMessage: String, history: List<String>, systemPrompt: String, settings: SecureSettings): String {
        val fullPrompt = buildString { append(systemPrompt); if (history.isNotEmpty()) append("\n\nContext: ").append(history.joinToString(" | ")); append("\n\n").append(userMessage) }
        val model = settings.getProviderModel(id).ifBlank { "claude-sonnet-5" }
        return suspendCancellableCoroutine { cont ->
            val requestId = nextId.incrementAndGet()
            pending[requestId] = cont
            val runCall: () -> Unit = {
                val js = "askPuter($requestId, ${JSONObject.quote(fullPrompt)}, ${JSONObject.quote(model)});"
                webView?.evaluateJavascript(js, null)
            }
            mainHandler.post { if (pageReady) runCall() else pageReadyQueue.add(runCall) }
            cont.invokeOnCancellation { pending.remove(requestId) }
        }
    }
    fun destroy() { mainHandler.post { webView?.destroy() } }
    private inner class Bridge {
        @JavascriptInterface fun onResult(requestId: String, text: String) { pending.remove(requestId.toLongOrNull())?.resume(text) }
        @JavascriptInterface fun onError(requestId: String, error: String) { pending.remove(requestId.toLongOrNull())?.resumeWithException(IOException("Puter.js: $error")) }
    }
}
