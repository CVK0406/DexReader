package com.example.dexreader.core.parser

import android.net.Uri
import coil.request.CachePolicy
import com.example.dexreader.core.model.MangaSource
import com.example.dexreader.core.util.ext.ifNullOrEmpty
import com.example.dexreader.explore.data.MangaSourcesRepository
import dagger.Reusable
import org.example.dexreader.parsers.exception.NotFoundException
import org.example.dexreader.parsers.model.ContentType
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.model.MangaListFilter
import org.example.dexreader.parsers.model.MangaSource
import org.example.dexreader.parsers.util.almostEquals
import org.example.dexreader.parsers.util.levenshteinDistance
import org.example.dexreader.parsers.util.runCatchingCancellable
import org.example.dexreader.parsers.util.toRelativeUrl
import javax.inject.Inject

@Reusable
class MangaLinkResolver @Inject constructor(
	private val repositoryFactory: MangaRepository.Factory,
	private val sourcesRepository: MangaSourcesRepository,
	private val dataRepository: MangaDataRepository,
) {

	suspend fun resolve(uri: Uri): Manga {
		return if (uri.scheme == "dexreader" || uri.host == "dexreader.app") {
			resolveAppLink(uri)
		} else {
			resolveExternalLink(uri)
		} ?: throw NotFoundException("Cannot resolve link", uri.toString())
	}

	private suspend fun resolveAppLink(uri: Uri): Manga? {
		require(uri.pathSegments.singleOrNull() == "manga") { "Invalid url" }
		val sourceName = requireNotNull(uri.getQueryParameter("source")) { "Source is not specified" }
		val source = MangaSource(sourceName)
		require(source != MangaSource.DUMMY) { "Manga source $sourceName is not supported" }
		val repo = repositoryFactory.create(source)
		return repo.findExact(
			url = uri.getQueryParameter("url"),
			title = uri.getQueryParameter("name"),
		)
	}

	private suspend fun resolveExternalLink(uri: Uri): Manga? {
		dataRepository.findMangaByPublicUrl(uri.toString())?.let {
			return it
		}
		val host = uri.host ?: return null
		val repo = sourcesRepository.allMangaSources.asSequence()
			.map { source ->
				repositoryFactory.create(source) as RemoteMangaRepository
			}.find { repo ->
				host in repo.domains
			} ?: return null
		return repo.findExact(uri.toString().toRelativeUrl(host), null)
	}

	private suspend fun MangaRepository.findExact(url: String?, title: String?): Manga? {
		if (!title.isNullOrEmpty()) {
			val list = getList(0, MangaListFilter.Search(title))
			if (url != null) {
				list.find { it.url == url }?.let {
					return it
				}
			}
			list.minByOrNull { it.title.levenshteinDistance(title) }
				?.takeIf { it.title.almostEquals(title, 0.2f) }
				?.let { return it }
		}
		val seed = getDetailsNoCache(
			getSeedManga(source, url ?: return null, title),
		)
		return runCatchingCancellable {
			val seedTitle = seed.title.ifEmpty {
				seed.altTitle
			}.ifNullOrEmpty {
				seed.author
			} ?: return@runCatchingCancellable null
			val seedList = getList(0, MangaListFilter.Search(seedTitle))
			seedList.first { x -> x.url == url }
		}.getOrThrow()
	}

	private suspend fun MangaRepository.getDetailsNoCache(manga: Manga): Manga {
		return if (this is RemoteMangaRepository) {
			getDetails(manga, CachePolicy.READ_ONLY)
		} else {
			getDetails(manga)
		}
	}

	private fun getSeedManga(source: MangaSource, url: String, title: String?) = Manga(
		id = run {
			var h = 1125899906842597L
			source.name.forEach { c ->
				h = 31 * h + c.code
			}
			url.forEach { c ->
				h = 31 * h + c.code
			}
			h
		},
		title = title.orEmpty(),
		altTitle = null,
		url = url,
		publicUrl = "",
		rating = 0.0f,
		isNsfw = source.contentType == ContentType.HENTAI,
		coverUrl = "",
		tags = emptySet(),
		state = null,
		author = null,
		largeCoverUrl = null,
		description = null,
		chapters = null,
		source = source,
	)
}
