package com.example.dexreader.reader.domain

import android.util.LongSparseArray
import com.example.dexreader.core.parser.MangaRepository
import com.example.dexreader.details.data.MangaDetails
import com.example.dexreader.reader.ui.pager.ReaderPage
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.example.dexreader.parsers.model.MangaChapter
import javax.inject.Inject

private const val PAGES_TRIM_THRESHOLD = 120

@ViewModelScoped
class ChaptersLoader @Inject constructor(
	private val mangaRepositoryFactory: MangaRepository.Factory,
) {

	private val chapters = LongSparseArray<MangaChapter>()
	private val chapterPages = ChapterPages()
	private val mutex = Mutex()

	val size: Int
		get() = chapters.size()

	suspend fun init(manga: MangaDetails) = mutex.withLock {
		chapters.clear()
		manga.allChapters.forEach {
			chapters.put(it.id, it)
		}
	}

	suspend fun loadPrevNextChapter(manga: MangaDetails, currentId: Long, isNext: Boolean) {
		val chapters = manga.allChapters
		val predicate: (MangaChapter) -> Boolean = { it.id == currentId }
		val index = if (isNext) chapters.indexOfFirst(predicate) else chapters.indexOfLast(predicate)
		if (index == -1) return
		val newChapter = chapters.getOrNull(if (isNext) index + 1 else index - 1) ?: return
		val newPages = loadChapter(newChapter.id)
		mutex.withLock {
			if (chapterPages.chaptersSize > 1) {
				// trim pages
				if (chapterPages.size > PAGES_TRIM_THRESHOLD) {
					if (isNext) {
						chapterPages.removeFirst()
					} else {
						chapterPages.removeLast()
					}
				}
			}
			if (isNext) {
				chapterPages.addLast(newChapter.id, newPages)
			} else {
				chapterPages.addFirst(newChapter.id, newPages)
			}
		}
	}

	suspend fun loadSingleChapter(chapterId: Long) {
		val pages = loadChapter(chapterId)
		mutex.withLock {
			chapterPages.clear()
			chapterPages.addLast(chapterId, pages)
		}
	}

	fun peekChapter(chapterId: Long): MangaChapter? = chapters[chapterId]

	fun hasPages(chapterId: Long): Boolean {
		return chapterId in chapterPages
	}

	fun getPages(chapterId: Long): List<ReaderPage> {
		return chapterPages.subList(chapterId)
	}

	fun getPagesCount(chapterId: Long): Int {
		return chapterPages.size(chapterId)
	}

	fun last() = chapterPages.last()

	fun first() = chapterPages.first()

	fun snapshot() = chapterPages.toList()

	private suspend fun loadChapter(chapterId: Long): List<ReaderPage> {
		val chapter = checkNotNull(chapters[chapterId]) { "Requested chapter not found" }
		val repo = mangaRepositoryFactory.create(chapter.source)
		return repo.getPages(chapter).mapIndexed { index, page ->
			ReaderPage(page, index, chapterId)
		}
	}
}
