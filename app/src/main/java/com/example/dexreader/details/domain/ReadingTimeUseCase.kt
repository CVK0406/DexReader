package com.example.dexreader.details.domain

import com.example.dexreader.core.model.MangaHistory
import com.example.dexreader.core.model.findById
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.details.data.MangaDetails
import com.example.dexreader.details.data.ReadingTime
import com.example.dexreader.stats.data.StatsRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.roundToInt

class ReadingTimeUseCase @Inject constructor(
	private val settings: AppSettings,
	private val statsRepository: StatsRepository,
) {

	suspend fun invoke(manga: MangaDetails?, branch: String?, history: MangaHistory?): ReadingTime? {
		if (!settings.isReadingTimeEstimationEnabled) {
			return null
		}
		val chapters = manga?.chapters?.get(branch)
		if (chapters.isNullOrEmpty()) {
			return null
		}
		val isOnHistoryBranch = history != null && chapters.findById(history.chapterId) != null
		// Impossible task, I guess. Good luck on this.
		var averageTimeSec: Int = 20 /* pages */ * getSecondsPerPage(manga.id) * chapters.size
		if (isOnHistoryBranch) {
			averageTimeSec = (averageTimeSec * (1f - checkNotNull(history).percent)).roundToInt()
		}
		if (averageTimeSec < 60) {
			return null
		}
		return ReadingTime(
			minutes = (averageTimeSec / 60) % 60,
			hours = averageTimeSec / 3600,
			isContinue = isOnHistoryBranch,
		)
	}

	private suspend fun getSecondsPerPage(mangaId: Long): Int {
		var time = if (settings.isStatsEnabled) {
			TimeUnit.MILLISECONDS.toSeconds(statsRepository.getTimePerPage(mangaId)).toInt()
		} else {
			0
		}
		if (time == 0) {
			time = 10 // default
		}
		return time
	}
}
