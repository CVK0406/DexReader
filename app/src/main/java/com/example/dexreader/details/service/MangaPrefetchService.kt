package com.example.dexreader.details.service

import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import com.example.dexreader.core.cache.ContentCache
import com.example.dexreader.core.model.findById
import com.example.dexreader.core.model.parcelable.ParcelableChapter
import com.example.dexreader.core.model.parcelable.ParcelableManga
import com.example.dexreader.core.parser.MangaRepository
import com.example.dexreader.core.ui.CoroutineIntentService
import com.example.dexreader.core.util.ext.getParcelableExtraCompat
import com.example.dexreader.core.util.ext.isPowerSaveMode
import com.example.dexreader.core.util.ext.printStackTraceDebug
import com.example.dexreader.history.data.HistoryRepository
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.model.MangaChapter
import org.example.dexreader.parsers.model.MangaSource
import org.example.dexreader.parsers.util.runCatchingCancellable
import javax.inject.Inject

@AndroidEntryPoint
class MangaPrefetchService : CoroutineIntentService() {

	@Inject
	lateinit var mangaRepositoryFactory: MangaRepository.Factory

	@Inject
	lateinit var cache: ContentCache

	@Inject
	lateinit var historyRepository: HistoryRepository

	override suspend fun processIntent(startId: Int, intent: Intent) {
		when (intent.action) {
			ACTION_PREFETCH_DETAILS -> prefetchDetails(
				manga = intent.getParcelableExtraCompat<ParcelableManga>(EXTRA_MANGA)?.manga
					?: return,
			)

			ACTION_PREFETCH_PAGES -> prefetchPages(
				chapter = intent.getParcelableExtraCompat<ParcelableChapter>(EXTRA_CHAPTER)?.chapter
					?: return,
			)

			ACTION_PREFETCH_LAST -> prefetchLast()
		}
	}

	override fun onError(startId: Int, error: Throwable) = Unit

	private suspend fun prefetchDetails(manga: Manga) {
		val source = mangaRepositoryFactory.create(manga.source)
		runCatchingCancellable { source.getDetails(manga) }
	}

	private suspend fun prefetchPages(chapter: MangaChapter) {
		val source = mangaRepositoryFactory.create(chapter.source)
		runCatchingCancellable { source.getPages(chapter) }
	}

	private suspend fun prefetchLast() {
		val last = historyRepository.getLastOrNull() ?: return
		if (last.source == MangaSource.LOCAL) return
		val repo = mangaRepositoryFactory.create(last.source)
		val details = runCatchingCancellable { repo.getDetails(last) }.getOrNull() ?: return
		val chapters = details.chapters
		if (chapters.isNullOrEmpty()) {
			return
		}
		val history = historyRepository.getOne(last)
		val chapter = if (history == null) {
			chapters.firstOrNull()
		} else {
			chapters.findById(history.chapterId) ?: chapters.firstOrNull()
		} ?: return
		runCatchingCancellable { repo.getPages(chapter) }
	}

	companion object {

		private const val EXTRA_MANGA = "manga"
		private const val EXTRA_CHAPTER = "manga"
		private const val ACTION_PREFETCH_DETAILS = "details"
		private const val ACTION_PREFETCH_PAGES = "pages"
		private const val ACTION_PREFETCH_LAST = "last"

		fun prefetchDetails(context: Context, manga: Manga) {
			if (!isPrefetchAvailable(context, manga.source)) return
			val intent = Intent(context, MangaPrefetchService::class.java)
			intent.action = ACTION_PREFETCH_DETAILS
			intent.putExtra(EXTRA_MANGA, ParcelableManga(manga))
			tryStart(context, intent)
		}

		fun prefetchPages(context: Context, chapter: MangaChapter) {
			if (!isPrefetchAvailable(context, chapter.source)) return
			val intent = Intent(context, MangaPrefetchService::class.java)
			intent.action = ACTION_PREFETCH_PAGES
			intent.putExtra(EXTRA_CHAPTER, ParcelableChapter(chapter))
			tryStart(context, intent)
		}

		fun prefetchLast(context: Context) {
			if (!isPrefetchAvailable(context, null)) return
			val intent = Intent(context, MangaPrefetchService::class.java)
			intent.action = ACTION_PREFETCH_LAST
			tryStart(context, intent)
		}

		private fun isPrefetchAvailable(context: Context, source: MangaSource?): Boolean {
			if (source == MangaSource.LOCAL) {
				return false
			}
			if (context.isPowerSaveMode()) {
				return false
			}
			val entryPoint = EntryPointAccessors.fromApplication(
				context,
				PrefetchCompanionEntryPoint::class.java,
			)
			return entryPoint.contentCache.isCachingEnabled
		}

		private fun tryStart(context: Context, intent: Intent) {
			try {
				context.startService(intent)
			} catch (e: IllegalStateException) {
				// probably app is in background
				e.printStackTraceDebug()
			}
		}
	}
}
