package com.example.dexreader.core.parser

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import android.webkit.WebView
import androidx.annotation.MainThread
import androidx.core.os.LocaleListCompat
import com.example.dexreader.core.network.MangaHttpClient
import com.example.dexreader.core.network.cookies.MutableCookieJar
import com.example.dexreader.core.prefs.SourceSettings
import com.example.dexreader.core.util.ext.configureForParser
import com.example.dexreader.core.util.ext.printStackTraceDebug
import com.example.dexreader.core.util.ext.sanitizeHeaderValue
import com.example.dexreader.core.util.ext.toList
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.example.dexreader.parsers.MangaLoaderContext
import org.example.dexreader.parsers.config.MangaSourceConfig
import org.example.dexreader.parsers.model.MangaSource
import org.example.dexreader.parsers.network.UserAgents
import org.example.dexreader.parsers.util.SuspendLazy
import java.lang.ref.WeakReference
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class MangaLoaderContextImpl @Inject constructor(
	@MangaHttpClient override val httpClient: OkHttpClient,
	override val cookieJar: MutableCookieJar,
	@ApplicationContext private val androidContext: Context,
) : MangaLoaderContext() {

	private var webViewCached: WeakReference<WebView>? = null

	private val userAgentLazy = SuspendLazy {
		withContext(Dispatchers.Main) {
			obtainWebView().settings.userAgentString
		}.sanitizeHeaderValue()
	}

	@SuppressLint("SetJavaScriptEnabled")
	override suspend fun evaluateJs(script: String): String? = withContext(Dispatchers.Main) {
		val webView = obtainWebView()
		suspendCoroutine { cont ->
			webView.evaluateJavascript(script) { result ->
				cont.resume(result?.takeUnless { it == "null" })
			}
		}
	}

	override fun getDefaultUserAgent(): String = runCatching {
		runBlocking {
			userAgentLazy.get()
		}
	}.onFailure { e ->
		e.printStackTraceDebug()
	}.getOrDefault(UserAgents.FIREFOX_MOBILE)

	override fun getConfig(source: MangaSource): MangaSourceConfig {
		return SourceSettings(androidContext, source)
	}

	override fun encodeBase64(data: ByteArray): String {
		return Base64.encodeToString(data, Base64.NO_WRAP)
	}

	override fun decodeBase64(data: String): ByteArray {
		return Base64.decode(data, Base64.DEFAULT)
	}

	override fun getPreferredLocales(): List<Locale> {
		return LocaleListCompat.getAdjustedDefault().toList()
	}

	@MainThread
	private fun obtainWebView(): WebView {
		return webViewCached?.get() ?: WebView(androidContext).also {
			it.configureForParser(null)
			webViewCached = WeakReference(it)
		}
	}
}
