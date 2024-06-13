package com.example.dexreader.core.cache

import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.model.MangaPage
import org.example.dexreader.parsers.model.MangaSource

interface ContentCache {

	val isCachingEnabled: Boolean

	suspend fun getDetails(source: MangaSource, url: String): Manga?

	fun putDetails(source: MangaSource, url: String, details: SafeDeferred<Manga>)

	suspend fun getPages(source: MangaSource, url: String): List<MangaPage>?

	fun putPages(source: MangaSource, url: String, pages: SafeDeferred<List<MangaPage>>)

	suspend fun getRelatedManga(source: MangaSource, url: String): List<Manga>?

	fun putRelatedManga(source: MangaSource, url: String, related: SafeDeferred<List<Manga>>)

	fun clear(source: MangaSource)

	data class Key(
		val source: MangaSource,
		val url: String,
	)
}
