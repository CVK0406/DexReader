package com.example.dexreader.explore.domain

import com.example.dexreader.core.parser.MangaRepository
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.util.ext.almostEquals
import com.example.dexreader.core.util.ext.asArrayList
import com.example.dexreader.core.util.ext.printStackTraceDebug
import com.example.dexreader.explore.data.MangaSourcesRepository
import com.example.dexreader.history.data.HistoryRepository
import org.example.dexreader.parsers.model.ContentType
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.model.MangaListFilter
import org.example.dexreader.parsers.model.MangaSource
import org.example.dexreader.parsers.util.runCatchingCancellable
import javax.inject.Inject

class ExploreRepository @Inject constructor(
	private val settings: AppSettings,
	private val sourcesRepository: MangaSourcesRepository,
	private val historyRepository: HistoryRepository,
	private val mangaRepositoryFactory: MangaRepository.Factory,
) {

	suspend fun findRandomManga(tagsLimit: Int): Manga {
		val tags = historyRepository.getPopularTags(tagsLimit).mapNotNull {
			 it.title
		}
		val sources = sourcesRepository.getEnabledSources()
		check(sources.isNotEmpty()) { "No sources available" }
		for (i in 0..4) {
			val list = getList(sources.random(), tags)
			val manga = list.randomOrNull() ?: continue
			val details = runCatchingCancellable {
				mangaRepositoryFactory.create(manga.source).getDetails(manga)
			}.getOrNull() ?: continue
			if ((settings.isSuggestionsExcludeNsfw && details.isNsfw)) {
				continue
			}
			return details
		}
		throw NoSuchElementException()
	}

	suspend fun findRandomManga(source: MangaSource, tagsLimit: Int): Manga {
		val skipNsfw = settings.isSuggestionsExcludeNsfw && source.contentType != ContentType.HENTAI
		val tags = historyRepository.getPopularTags(tagsLimit).mapNotNull {
			it.title
		}
		for (i in 0..4) {
			val list = getList(source, tags)
			val manga = list.randomOrNull() ?: continue
			val details = runCatchingCancellable {
				mangaRepositoryFactory.create(manga.source).getDetails(manga)
			}.getOrNull() ?: continue
			if ((skipNsfw && details.isNsfw)) {
				continue
			}
			return details
		}
		throw NoSuchElementException()
	}

	private suspend fun getList(
		source: MangaSource,
		tags: List<String>,
	): List<Manga> = runCatchingCancellable {
		val repository = mangaRepositoryFactory.create(source)
		val order = repository.sortOrders.random()
		val availableTags = repository.getTags()
		val tag = tags.firstNotNullOfOrNull { title ->
			availableTags.find { x -> x.title.almostEquals(title, 0.4f) }
		}
		val list = repository.getList(
			offset = 0,
			filter = MangaListFilter.Advanced.Builder(order)
				.tags(setOfNotNull(tag))
				.build(),
		).asArrayList()
		if (settings.isSuggestionsExcludeNsfw) {
			list.removeAll { it.isNsfw }
		}
		list.shuffle()
		list
	}.onFailure {
		it.printStackTraceDebug()
	}.getOrDefault(emptyList())
}
