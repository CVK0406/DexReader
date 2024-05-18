package com.example.dexreader.main.domain

import androidx.collection.ArraySet
import coil.intercept.Interceptor
import coil.network.HttpException
import coil.request.ErrorResult
import coil.request.ImageResult
import com.example.dexreader.bookmarks.domain.Bookmark
import com.example.dexreader.bookmarks.domain.BookmarksRepository
import com.example.dexreader.core.model.findById
import com.example.dexreader.core.parser.MangaDataRepository
import com.example.dexreader.core.parser.MangaRepository
import com.example.dexreader.core.parser.RemoteMangaRepository
import com.example.dexreader.core.util.ext.ifNullOrEmpty
import org.example.dexreader.parsers.exception.ParseException
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.util.runCatchingCancellable
import org.jsoup.HttpStatusException
import java.util.Collections
import javax.inject.Inject

class CoverRestoreInterceptor @Inject constructor(
	private val dataRepository: MangaDataRepository,
	private val bookmarksRepository: BookmarksRepository,
	private val repositoryFactory: MangaRepository.Factory,
) : Interceptor {

	private val blacklist = Collections.synchronizedSet(ArraySet<String>())

	override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
		val request = chain.request
		val result = chain.proceed(request)
		if (result is ErrorResult && result.throwable.shouldRestore()) {
			request.tags.tag<Manga>()?.let {
				if (restoreManga(it)) {
					return chain.proceed(request.newBuilder().build())
				} else {
					return result
				}
			}
			request.tags.tag<Bookmark>()?.let {
				if (restoreBookmark(it)) {
					return chain.proceed(request.newBuilder().build())
				} else {
					return result
				}
			}
		}
		return result
	}

	private suspend fun restoreManga(manga: Manga): Boolean {
		val key = manga.publicUrl
		if (!blacklist.add(key)) {
			return false
		}
		val restored = runCatchingCancellable {
			restoreMangaImpl(manga)
		}.getOrDefault(false)
		if (restored) {
			blacklist.remove(key)
		}
		return restored
	}

	private suspend fun restoreMangaImpl(manga: Manga): Boolean {
		if (dataRepository.findMangaById(manga.id) == null) {
			return false
		}
		val repo = repositoryFactory.create(manga.source) as? RemoteMangaRepository ?: return false
		val fixed = repo.find(manga) ?: return false
		return if (fixed != manga) {
			dataRepository.storeManga(fixed)
			fixed.coverUrl != manga.coverUrl
		} else {
			false
		}
	}

	private suspend fun restoreBookmark(bookmark: Bookmark): Boolean {
		val key = bookmark.imageUrl
		if (!blacklist.add(key)) {
			return false
		}
		val restored = runCatchingCancellable {
			restoreBookmarkImpl(bookmark)
		}.getOrDefault(false)
		if (restored) {
			blacklist.remove(key)
		}
		return restored
	}

	private suspend fun restoreBookmarkImpl(bookmark: Bookmark): Boolean {
		val repo = repositoryFactory.create(bookmark.manga.source) as? RemoteMangaRepository ?: return false
		val chapter = repo.getDetails(bookmark.manga).chapters?.findById(bookmark.chapterId) ?: return false
		val page = repo.getPages(chapter)[bookmark.page]
		val imageUrl = page.preview.ifNullOrEmpty { page.url }
		return if (imageUrl != bookmark.imageUrl) {
			bookmarksRepository.updateBookmark(bookmark, imageUrl)
			true
		} else {
			false
		}
	}

	private fun Throwable.shouldRestore(): Boolean {
		return this is HttpException || this is HttpStatusException || this is ParseException
	}
}
