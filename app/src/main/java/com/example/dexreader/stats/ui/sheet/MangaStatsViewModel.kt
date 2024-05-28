package com.example.dexreader.stats.ui.sheet

import androidx.collection.IntList
import androidx.collection.MutableIntList
import androidx.collection.emptyIntList
import androidx.lifecycle.SavedStateHandle
import com.example.dexreader.core.model.parcelable.ParcelableManga
import com.example.dexreader.core.ui.BaseViewModel
import com.example.dexreader.core.ui.model.DateTimeAgo
import com.example.dexreader.core.util.ext.calculateTimeAgo
import com.example.dexreader.core.util.ext.require
import com.example.dexreader.stats.data.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class MangaStatsViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val repository: StatsRepository,
) : BaseViewModel() {

	val manga = savedStateHandle.require<ParcelableManga>(MangaStatsSheet.ARG_MANGA).manga

	val stats = MutableStateFlow<IntList>(emptyIntList())
	val startDate = MutableStateFlow<DateTimeAgo?>(null)
	val totalPagesRead = MutableStateFlow(0)

	init {
		launchLoadingJob(Dispatchers.Default) {
			val timeline = repository.getMangaTimeline(manga.id)
			if (timeline.isEmpty()) {
				startDate.value = null
				stats.value = emptyIntList()
			} else {
				val startDay = TimeUnit.MILLISECONDS.toDays(timeline.firstKey())
				val endDay = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())
				val res = MutableIntList((endDay - startDay).toInt() + 1)
				for (day in startDay..endDay) {
					val from = TimeUnit.DAYS.toMillis(day)
					val to = TimeUnit.DAYS.toMillis(day + 1)
					res.add(timeline.subMap(from, true, to, false).values.sum())
				}
				stats.value = res
				startDate.value = calculateTimeAgo(Instant.ofEpochMilli(timeline.firstKey()))
			}
		}
		launchLoadingJob(Dispatchers.Default) {
			totalPagesRead.value = repository.getTotalPagesRead(manga.id)
		}
	}
}
