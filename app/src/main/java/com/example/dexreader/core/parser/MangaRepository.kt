package com.example.dexreader.core.parser

import androidx.annotation.AnyThread
import com.example.dexreader.core.cache.ContentCache
import com.example.dexreader.core.network.MirrorSwitchInterceptor
import com.example.dexreader.local.data.LocalMangaRepository
import org.example.dexreader.parsers.MangaLoaderContext
import org.example.dexreader.parsers.model.ContentRating
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.model.MangaChapter
import org.example.dexreader.parsers.model.MangaListFilter
import org.example.dexreader.parsers.model.MangaPage
import org.example.dexreader.parsers.model.MangaSource
import org.example.dexreader.parsers.model.MangaState
import org.example.dexreader.parsers.model.MangaTag
import org.example.dexreader.parsers.model.SortOrder
import java.lang.ref.WeakReference
import java.util.EnumMap
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.set

interface MangaRepository {

	val source: MangaSource

	val sortOrders: Set<SortOrder>

	val states: Set<MangaState>

	val contentRatings: Set<ContentRating>

	var defaultSortOrder: SortOrder

	val isMultipleTagsSupported: Boolean

	val isTagsExclusionSupported: Boolean

	val isSearchSupported: Boolean

	suspend fun getList(offset: Int, filter: MangaListFilter?): List<Manga>

	suspend fun getDetails(manga: Manga): Manga

	suspend fun getPages(chapter: MangaChapter): List<MangaPage>

	suspend fun getPageUrl(page: MangaPage): String

	suspend fun getTags(): Set<MangaTag>

	suspend fun getLocales(): Set<Locale>

	suspend fun getRelated(seed: Manga): List<Manga>

	@Singleton
	class Factory @Inject constructor(
		private val localMangaRepository: LocalMangaRepository,
		private val loaderContext: MangaLoaderContext,
		private val contentCache: ContentCache,
		private val mirrorSwitchInterceptor: MirrorSwitchInterceptor,
	) {

		private val cache = EnumMap<MangaSource, WeakReference<RemoteMangaRepository>>(MangaSource::class.java)

		@AnyThread
		fun create(source: MangaSource): MangaRepository {
			if (source == MangaSource.LOCAL) {
				return localMangaRepository
			}
			cache[source]?.get()?.let { return it }
			return synchronized(cache) {
				cache[source]?.get()?.let { return it }
				val repository = RemoteMangaRepository(
					parser = MangaParser(source, loaderContext),
					cache = contentCache,
					mirrorSwitchInterceptor = mirrorSwitchInterceptor,
				)
				cache[source] = WeakReference(repository)
				repository
			}
		}
	}
}
