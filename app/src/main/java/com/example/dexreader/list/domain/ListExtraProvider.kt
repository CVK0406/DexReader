package com.example.dexreader.list.domain

import android.content.Context
import androidx.annotation.ColorRes
import com.example.dexreader.R
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.history.data.HistoryRepository
import com.example.dexreader.history.data.PROGRESS_NONE
import com.example.dexreader.tracker.domain.TrackingRepository
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import org.example.dexreader.parsers.model.MangaTag
import javax.inject.Inject

@Reusable
class ListExtraProvider @Inject constructor(
	@ApplicationContext context: Context,
	private val settings: AppSettings,
	private val trackingRepository: TrackingRepository,
	private val historyRepository: HistoryRepository,
) {

	private val dict by lazy {
		context.resources.openRawResource(R.raw.tags_redlist).use {
			val set = HashSet<String>()
			it.bufferedReader().forEachLine { x ->
				val line = x.trim()
				if (line.isNotEmpty()) {
					set.add(line)
				}
			}
			set
		}
	}

	suspend fun getCounter(mangaId: Long): Int {
		return if (settings.isTrackerEnabled) {
			trackingRepository.getNewChaptersCount(mangaId)
		} else {
			0
		}
	}

	suspend fun getProgress(mangaId: Long): Float {
		return if (settings.isReadingIndicatorsEnabled) {
			historyRepository.getProgress(mangaId)
		} else {
			PROGRESS_NONE
		}
	}

	@ColorRes
	fun getTagTint(tag: MangaTag): Int {
		return if (tag.title.lowercase() in dict) {
			R.color.warning
		} else {
			0
		}
	}
}
