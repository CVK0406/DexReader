package com.example.dexreader.details.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.prefs.observeAsFlow
import com.example.dexreader.details.data.MangaDetails
import com.example.dexreader.favourites.domain.FavouritesRepository
import com.example.dexreader.history.data.HistoryRepository
import com.example.dexreader.local.data.LocalMangaRepository
import com.example.dexreader.local.domain.model.LocalManga
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.util.runCatchingCancellable
import com.example.dexreader.tracker.domain.TrackingRepository
import javax.inject.Inject

/* TODO: remove */
class DetailsInteractor @Inject constructor(
	private val historyRepository: HistoryRepository,
	private val favouritesRepository: FavouritesRepository,
	private val localMangaRepository: LocalMangaRepository,
	private val trackingRepository: TrackingRepository,
	private val settings: AppSettings,
) {

	fun observeIsFavourite(mangaId: Long): Flow<Boolean> {
		return favouritesRepository.observeCategoriesIds(mangaId)
			.map { it.isNotEmpty() }
	}

	fun observeNewChapters(mangaId: Long): Flow<Int> {
		return settings.observeAsFlow(AppSettings.KEY_TRACKER_ENABLED) { isTrackerEnabled }
			.flatMapLatest { isEnabled ->
				if (isEnabled) {
					trackingRepository.observeNewChaptersCount(mangaId)
				} else {
					flowOf(0)
				}
			}
	}

	suspend fun updateLocal(subject: MangaDetails?, localManga: LocalManga): MangaDetails? {
		subject ?: return null
		return if (subject.id == localManga.manga.id) {
			if (subject.isLocal) {
				subject.copy(
					manga = localManga.manga,
				)
			} else {
				subject.copy(
					localManga = runCatchingCancellable {
						localManga.copy(
							manga = localMangaRepository.getDetails(localManga.manga),
						)
					}.getOrNull() ?: subject.local,
				)
			}
		} else {
			subject
		}
	}

	suspend fun findRemote(seed: Manga) = localMangaRepository.getRemoteManga(seed)
}
