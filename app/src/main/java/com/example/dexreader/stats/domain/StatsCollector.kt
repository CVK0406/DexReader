package com.example.dexreader.stats.domain

import androidx.collection.LongSparseArray
import androidx.collection.set
import com.example.dexreader.core.db.MangaDatabase
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.util.RetainedLifecycleCoroutineScope
import com.example.dexreader.stats.data.StatsEntity
import com.example.dexreader.reader.ui.ReaderState
import dagger.hilt.android.ViewModelLifecycle
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@ViewModelScoped
class StatsCollector @Inject constructor(
	private val db: MangaDatabase,
	private val settings: AppSettings,
	lifecycle: ViewModelLifecycle,
) {

	private val viewModelScope = RetainedLifecycleCoroutineScope(lifecycle)
	private val stats = LongSparseArray<Entry>(1)

	@Synchronized
	fun onStateChanged(mangaId: Long, state: ReaderState) {
		if (!settings.isStatsEnabled) {
			return
		}
		val now = System.currentTimeMillis()
		val entry = stats[mangaId]
		if (entry == null) {
			stats[mangaId] = Entry(
				state = state,
				stats = StatsEntity(
					mangaId = mangaId,
					startedAt = now,
					duration = 0,
					pages = 0,
				),
			)
			return
		}
		val pagesDelta = if (entry.state.page != state.page || entry.state.chapterId != state.chapterId) 1 else 0
		val newEntry = entry.copy(
			stats = StatsEntity(
				mangaId = mangaId,
				startedAt = entry.stats.startedAt,
				duration = now - entry.stats.startedAt,
				pages = entry.stats.pages + pagesDelta,
			),
		)
		stats[mangaId] = newEntry
		commit(newEntry.stats)
	}

	@Synchronized
	fun onPause(mangaId: Long) {
		stats.remove(mangaId)
	}

	private fun commit(entity: StatsEntity) {
		viewModelScope.launch(Dispatchers.Default) {
			db.getStatsDao().upsert(entity)
		}
	}

	private data class Entry(
		val state: ReaderState,
		val stats: StatsEntity,
	)
}
