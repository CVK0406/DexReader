package com.example.dexreader.core.network

import android.util.Log
import com.example.dexreader.BuildConfig
import com.example.dexreader.core.parser.MangaLoaderContextImpl
import com.example.dexreader.core.parser.MangaRepository
import com.example.dexreader.core.parser.RemoteMangaRepository
import com.example.dexreader.core.util.ext.printStackTraceDebug
import dagger.Lazy
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.example.dexreader.parsers.model.MangaSource
import org.example.dexreader.parsers.util.mergeWith
import java.net.IDN
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommonHeadersInterceptor @Inject constructor(
	private val mangaRepositoryFactoryLazy: Lazy<MangaRepository.Factory>,
	private val mangaLoaderContextLazy: Lazy<MangaLoaderContextImpl>,
) : Interceptor {

	override fun intercept(chain: Interceptor.Chain): Response {
		val request = chain.request()
		val source = request.tag(MangaSource::class.java)
		val repository = if (source != null) {
			mangaRepositoryFactoryLazy.get().create(source) as? RemoteMangaRepository
		} else {
			if (BuildConfig.DEBUG) {
				Log.w("Http", "Request without source tag: ${request.url}")
			}
			null
		}
		val headersBuilder = request.headers.newBuilder()
		repository?.headers?.let {
			headersBuilder.mergeWith(it, replaceExisting = false)
		}
		if (headersBuilder[CommonHeaders.USER_AGENT] == null) {
			headersBuilder[CommonHeaders.USER_AGENT] = mangaLoaderContextLazy.get().getDefaultUserAgent()
		}
		if (headersBuilder[CommonHeaders.REFERER] == null && repository != null) {
			val idn = IDN.toASCII(repository.domain)
			headersBuilder.trySet(CommonHeaders.REFERER, "https://$idn/")
		}
		val newRequest = request.newBuilder().headers(headersBuilder.build()).build()
		return repository?.intercept(ProxyChain(chain, newRequest)) ?: chain.proceed(newRequest)
	}

	private fun Headers.Builder.trySet(name: String, value: String) = try {
		set(name, value)
	} catch (e: IllegalArgumentException) {
		e.printStackTraceDebug()
	}

	private class ProxyChain(
		private val delegate: Interceptor.Chain,
		private val request: Request,
	) : Interceptor.Chain by delegate {

		override fun request(): Request = request
	}
}
