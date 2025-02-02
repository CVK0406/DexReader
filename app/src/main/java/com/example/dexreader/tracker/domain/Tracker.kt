package com.example.dexreader.tracker.domain

import androidx.annotation.VisibleForTesting
import androidx.collection.MutableLongSet
import coil.request.CachePolicy
import com.example.dexreader.core.model.getPreferredBranch
import com.example.dexreader.core.parser.MangaRepository
import com.example.dexreader.core.parser.RemoteMangaRepository
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.util.CompositeMutex2
import com.example.dexreader.history.data.HistoryRepository
import org.example.dexreader.parsers.model.Manga
import com.example.dexreader.tracker.domain.model.MangaTracking
import com.example.dexreader.tracker.domain.model.MangaUpdates
import com.example.dexreader.tracker.work.TrackerNotificationChannels
import com.example.dexreader.tracker.work.TrackingItem
import javax.inject.Inject
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class Tracker @Inject constructor(
	private val settings: AppSettings,
	private val repository: TrackingRepository,
	private val historyRepository: HistoryRepository,
	private val channels: TrackerNotificationChannels,
	private val mangaRepositoryFactory: MangaRepository.Factory,
) {

	suspend fun getAllTracks(): List<TrackingItem> {
		val sources = settings.trackSources
		if (sources.isEmpty()) {
			return emptyList()
		}
		val knownManga = MutableLongSet()
		val result = ArrayList<TrackingItem>()
		// Favourites
		if (AppSettings.TRACK_FAVOURITES in sources) {
			val favourites = repository.getAllFavouritesManga()
			channels.updateChannels(favourites.keys)
			for ((category, mangaList) in favourites) {
				if (!category.isTrackingEnabled || mangaList.isEmpty()) {
					continue
				}
				val categoryTracks = repository.getTracks(mangaList)
				val channelId = if (channels.isFavouriteNotificationsEnabled(category)) {
					channels.getFavouritesChannelId(category.id)
				} else {
					null
				}
				for (track in categoryTracks) {
					if (knownManga.add(track.manga.id)) {
						result.add(TrackingItem(track, channelId))
					}
				}
			}
		}
		// History
		if (AppSettings.TRACK_HISTORY in sources) {
			val history = repository.getAllHistoryManga()
			val historyTracks = repository.getTracks(history)
			val channelId = if (channels.isHistoryNotificationsEnabled()) {
				channels.getHistoryChannelId()
			} else {
				null
			}
			for (track in historyTracks) {
				if (knownManga.add(track.manga.id)) {
					result.add(TrackingItem(track, channelId))
				}
			}
		}
		result.trimToSize()
		return result
	}

	suspend fun getTracks(ids: Set<Long>): List<TrackingItem> {
		return getAllTracks().filterTo(ArrayList(ids.size)) { x -> x.tracking.manga.id in ids }
	}

	suspend fun gc() {
		repository.gc()
	}

	suspend fun fetchUpdates(
		track: MangaTracking,
		commit: Boolean
	): MangaUpdates.Success = withMangaLock(track.manga.id) {
		val repo = mangaRepositoryFactory.create(track.manga.source)
		require(repo is RemoteMangaRepository) { "Repository ${repo.javaClass.simpleName} is not supported" }
		val manga = repo.getDetails(track.manga, CachePolicy.WRITE_ONLY)
		val updates = compare(track, manga, getBranch(manga))
		if (commit) {
			repository.saveUpdates(updates)
		}
		return updates
	}

	@VisibleForTesting
	suspend fun checkUpdates(manga: Manga, commit: Boolean): MangaUpdates.Success {
		val track = repository.getTrack(manga)
		val updates = compare(track, manga, getBranch(manga))
		if (commit) {
			repository.saveUpdates(updates)
		}
		return updates
	}

	@VisibleForTesting
	suspend fun deleteTrack(mangaId: Long) = withMangaLock(mangaId) {
		repository.deleteTrack(mangaId)
	}

	private suspend fun getBranch(manga: Manga): String? {
		val history = historyRepository.getOne(manga)
		return manga.getPreferredBranch(history)
	}

	/**
	 * The main functionality of tracker: check new chapters in [manga] comparing to the [track]
	 */
	private fun compare(track: MangaTracking, manga: Manga, branch: String?): MangaUpdates.Success {
		if (track.isEmpty()) {
			// first check or manga was empty on last check
			return MangaUpdates.Success(manga, emptyList(), isValid = false, channelId = null)
		}
		val chapters = requireNotNull(manga.getChapters(branch))
		val newChapters = chapters.takeLastWhile { x -> x.id != track.lastChapterId }
		return when {
			newChapters.isEmpty() -> {
				MangaUpdates.Success(
					manga = manga,
					newChapters = emptyList(),
					isValid = chapters.lastOrNull()?.id == track.lastChapterId,
					channelId = null,
				)
			}

			newChapters.size == chapters.size -> {
				MangaUpdates.Success(manga, emptyList(), isValid = false, channelId = null)
			}

			else -> {
				MangaUpdates.Success(manga, newChapters, isValid = true, channelId = null)
			}
		}
	}

	private companion object {

		private val mangaMutex = CompositeMutex2<Long>()

		suspend inline fun <T> withMangaLock(id: Long, action: () -> T): T {
			contract {
				callsInPlace(action, InvocationKind.EXACTLY_ONCE)
			}
			mangaMutex.lock(id)
			try {
				return action()
			} finally {
				mangaMutex.unlock(id)
			}
		}
	}
}
