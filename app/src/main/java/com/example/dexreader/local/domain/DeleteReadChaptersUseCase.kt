package com.example.dexreader.local.domain

import com.example.dexreader.core.model.findById
import com.example.dexreader.core.model.ids
import com.example.dexreader.core.model.isLocal
import com.example.dexreader.core.util.ext.printStackTraceDebug
import com.example.dexreader.history.data.HistoryRepository
import com.example.dexreader.local.data.LocalMangaRepository
import com.example.dexreader.local.data.LocalStorageChanges
import com.example.dexreader.local.domain.model.LocalManga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.util.runCatchingCancellable
import javax.inject.Inject

class DeleteReadChaptersUseCase @Inject constructor(
	private val localMangaRepository: LocalMangaRepository,
	private val historyRepository: HistoryRepository,
	@LocalStorageChanges private val localStorageChanges: MutableSharedFlow<LocalManga?>,
) {

	suspend operator fun invoke(manga: Manga): Int {
		val localManga = if (manga.isLocal) {
			LocalManga(manga)
		} else {
			checkNotNull(localMangaRepository.findSavedManga(manga)) { "Cannot find local manga" }
		}
		val task = getDeletionTask(localManga) ?: return 0
		localMangaRepository.deleteChapters(task.manga.manga, task.chaptersIds)
		emitUpdate(localManga)
		return task.chaptersIds.size
	}

	suspend operator fun invoke(): Int {
		val list = localMangaRepository.getList(0, null)
		if (list.isEmpty()) {
			return 0
		}
		return channelFlow {
			for (manga in list) {
				launch(Dispatchers.Default) {
					val task = runCatchingCancellable {
						getDeletionTask(LocalManga(manga))
					}.onFailure {
						it.printStackTraceDebug()
					}.getOrNull()
					if (task != null) {
						send(task)
					}
				}
			}
		}.buffer().map {
			runCatchingCancellable {
				localMangaRepository.deleteChapters(it.manga.manga, it.chaptersIds)
				emitUpdate(it.manga)
				it.chaptersIds.size
			}.onFailure {
				it.printStackTraceDebug()
			}.getOrDefault(0)
		}.fold(0) { acc, x -> acc + x }
	}

	private suspend fun getDeletionTask(manga: LocalManga): DeletionTask? {
		val history = historyRepository.getOne(manga.manga) ?: return null
		val chapters = manga.manga.chapters ?: localMangaRepository.getDetails(manga.manga).chapters
		if (chapters.isNullOrEmpty()) {
			return null
		}
		val branch = (chapters.findById(history.chapterId) ?: return null).branch
		val filteredChapters = manga.manga.getChapters(branch)?.takeWhile { it.id != history.chapterId }
		return if (filteredChapters.isNullOrEmpty()) {
			null
		} else {
			DeletionTask(
				manga = manga,
				chaptersIds = filteredChapters.ids(),
			)
		}
	}

	private suspend fun emitUpdate(subject: LocalManga) {
		val updated = localMangaRepository.getDetails(subject.manga)
		localStorageChanges.emit(subject.copy(manga = updated))
	}

	private class DeletionTask(
		val manga: LocalManga,
		val chaptersIds: Set<Long>,
	)
}
