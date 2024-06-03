package com.example.dexreader.core

import android.app.Application
import android.content.Context
import android.provider.SearchRecentSuggestions
import android.text.Html
import androidx.collection.arraySetOf
import androidx.room.InvalidationTracker
import androidx.work.WorkManager
import coil.ComponentRegistry
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.util.DebugLogger
import com.example.dexreader.BuildConfig
import com.example.dexreader.core.cache.ContentCache
import com.example.dexreader.core.cache.MemoryContentCache
import com.example.dexreader.core.cache.StubContentCache
import com.example.dexreader.core.db.MangaDatabase
import com.example.dexreader.core.network.ImageProxyInterceptor
import com.example.dexreader.core.network.MangaHttpClient
import com.example.dexreader.core.os.NetworkState
import com.example.dexreader.core.parser.MangaLoaderContextImpl
import com.example.dexreader.core.parser.MangaRepository
import com.example.dexreader.core.parser.favicon.FaviconFetcher
import com.example.dexreader.core.ui.image.CoilImageGetter
import com.example.dexreader.core.ui.util.ActivityRecreationHandle
import com.example.dexreader.core.util.ext.connectivityManager
import com.example.dexreader.core.util.ext.isLowRamDevice
import com.example.dexreader.local.data.CacheDir
import com.example.dexreader.local.data.CbzFetcher
import com.example.dexreader.local.data.LocalStorageChanges
import com.example.dexreader.local.domain.model.LocalManga
import com.example.dexreader.main.domain.CoverRestoreInterceptor
import com.example.dexreader.reader.ui.thumbnails.MangaPageFetcher
import com.example.dexreader.search.ui.MangaSuggestionsProvider
import com.example.dexreader.settings.backup.BackupObserver
import com.example.dexreader.widget.WidgetUpdater
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.OkHttpClient
import org.example.dexreader.parsers.MangaLoaderContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface AppModule {

	@Binds
	fun bindMangaLoaderContext(mangaLoaderContextImpl: MangaLoaderContextImpl): MangaLoaderContext

	@Binds
	fun bindImageGetter(coilImageGetter: CoilImageGetter): Html.ImageGetter

	companion object {

		@Provides
		@Singleton
		fun provideNetworkState(
			@ApplicationContext context: Context
		) = NetworkState(context.connectivityManager)

		@Provides
		@Singleton
		fun provideMangaDatabase(
			@ApplicationContext context: Context,
		): MangaDatabase {
			return MangaDatabase(context)
		}

		@Provides
		@Singleton
		fun provideCoil(
			@ApplicationContext context: Context,
			@MangaHttpClient okHttpClient: OkHttpClient,
			mangaRepositoryFactory: MangaRepository.Factory,
			imageProxyInterceptor: ImageProxyInterceptor,
			pageFetcherFactory: MangaPageFetcher.Factory,
			coverRestoreInterceptor: CoverRestoreInterceptor,
		): ImageLoader {
			val diskCacheFactory = {
				val rootDir = context.externalCacheDir ?: context.cacheDir
				DiskCache.Builder()
					.directory(rootDir.resolve(CacheDir.THUMBS.dir))
					.build()
			}
			return ImageLoader.Builder(context)
				.okHttpClient(okHttpClient.newBuilder().cache(null).build())
				.interceptorDispatcher(Dispatchers.Default)
				.fetcherDispatcher(Dispatchers.IO)
				.decoderDispatcher(Dispatchers.Default)
				.transformationDispatcher(Dispatchers.Default)
				.diskCache(diskCacheFactory)
				.logger(if (BuildConfig.DEBUG) DebugLogger() else null)
				.allowRgb565(context.isLowRamDevice())
				.components(
					ComponentRegistry.Builder()
						.add(SvgDecoder.Factory())
						.add(CbzFetcher.Factory())
						.add(FaviconFetcher.Factory(context, okHttpClient, mangaRepositoryFactory))
						.add(pageFetcherFactory)
						.add(imageProxyInterceptor)
						.add(coverRestoreInterceptor)
						.build(),
				).build()
		}

		@Provides
		fun provideSearchSuggestions(
			@ApplicationContext context: Context,
		): SearchRecentSuggestions {
			return MangaSuggestionsProvider.createSuggestions(context)
		}

		@Provides
		@ElementsIntoSet
		fun provideDatabaseObservers(
			widgetUpdater: WidgetUpdater,
			backupObserver: BackupObserver,
		): Set<@JvmSuppressWildcards InvalidationTracker.Observer> = arraySetOf(
			widgetUpdater,
			backupObserver,
		)

		@Provides
		@ElementsIntoSet
		fun provideActivityLifecycleCallbacks(
			activityRecreationHandle: ActivityRecreationHandle,
		): Set<@JvmSuppressWildcards Application.ActivityLifecycleCallbacks> = arraySetOf(
			activityRecreationHandle,
		)

		@Provides
		@Singleton
		fun provideContentCache(
			application: Application,
		): ContentCache {
			return if (application.isLowRamDevice()) {
				StubContentCache()
			} else {
				MemoryContentCache(application)
			}
		}

		@Provides
		@Singleton
		@LocalStorageChanges
		fun provideMutableLocalStorageChangesFlow(): MutableSharedFlow<LocalManga?> = MutableSharedFlow()

		@Provides
		@LocalStorageChanges
		fun provideLocalStorageChangesFlow(
			@LocalStorageChanges flow: MutableSharedFlow<LocalManga?>,
		): SharedFlow<LocalManga?> = flow.asSharedFlow()

		@Provides
		fun provideWorkManager(
			@ApplicationContext context: Context,
		): WorkManager = WorkManager.getInstance(context)
	}
}
