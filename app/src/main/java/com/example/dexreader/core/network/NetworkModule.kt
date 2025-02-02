package com.example.dexreader.core.network

import android.content.Context
import android.util.AndroidRuntimeException
import com.example.dexreader.BuildConfig
import com.example.dexreader.core.network.cookies.AndroidCookieJar
import com.example.dexreader.core.network.cookies.MutableCookieJar
import com.example.dexreader.core.network.cookies.PreferencesCookieJar
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.local.data.LocalStorageManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface NetworkModule {

	@Binds
	fun bindCookieJar(androidCookieJar: MutableCookieJar): CookieJar

	companion object {

		@Provides
		@Singleton
		fun provideCookieJar(
			@ApplicationContext context: Context
		): MutableCookieJar = try {
			AndroidCookieJar()
		} catch (e: AndroidRuntimeException) {
			PreferencesCookieJar(context)
		}

		@Provides
		@Singleton
		fun provideHttpCache(
			localStorageManager: LocalStorageManager,
		): Cache = localStorageManager.createHttpCache()

		@Provides
		@Singleton
		@BaseHttpClient
		fun provideBaseHttpClient(
			cache: Cache,
			cookieJar: CookieJar,
			settings: AppSettings,
		): OkHttpClient = OkHttpClient.Builder().apply {
			connectTimeout(20, TimeUnit.SECONDS)
			readTimeout(60, TimeUnit.SECONDS)
			writeTimeout(20, TimeUnit.SECONDS)
			cookieJar(cookieJar)
			proxySelector(AppProxySelector(settings))
			proxyAuthenticator(ProxyAuthenticator(settings))
			dns(DoHManager(cache, settings))
			if (settings.isSSLBypassEnabled) {
				bypassSSLErrors()
			}
			cache(cache)
			addInterceptor(GZipInterceptor())
			addInterceptor(RateLimitInterceptor())
			if (BuildConfig.DEBUG) {
				addInterceptor(CurlLoggingInterceptor())
			}
		}.build()

		@Provides
		@Singleton
		@MangaHttpClient
		fun provideMangaHttpClient(
			@BaseHttpClient baseClient: OkHttpClient,
			commonHeadersInterceptor: CommonHeadersInterceptor,
			mirrorSwitchInterceptor: MirrorSwitchInterceptor,
		): OkHttpClient = baseClient.newBuilder().apply {
			addNetworkInterceptor(CacheLimitInterceptor())
			addInterceptor(commonHeadersInterceptor)
			addInterceptor(mirrorSwitchInterceptor)
		}.build()

	}
}
