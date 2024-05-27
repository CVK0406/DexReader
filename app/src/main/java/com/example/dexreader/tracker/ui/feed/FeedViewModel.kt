package com.example.dexreader.tracker.ui.feed

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import com.example.dexreader.R
import com.example.dexreader.core.prefs.ListMode
import com.example.dexreader.core.ui.BaseViewModel
import com.example.dexreader.core.ui.model.DateTimeAgo
import com.example.dexreader.core.util.ext.MutableEventFlow
import com.example.dexreader.core.util.ext.call
import com.example.dexreader.core.util.ext.calculateTimeAgo
import com.example.dexreader.list.domain.ListExtraProvider
import com.example.dexreader.list.ui.model.EmptyState
import com.example.dexreader.list.ui.model.ListHeader
import com.example.dexreader.list.ui.model.ListModel
import com.example.dexreader.list.ui.model.LoadingState
import com.example.dexreader.list.ui.model.toUi
import com.example.dexreader.tracker.domain.TrackingRepository
import com.example.dexreader.tracker.domain.model.TrackingLogItem
import com.example.dexreader.tracker.ui.feed.model.UpdatedMangaHeader
import com.example.dexreader.tracker.ui.feed.model.toFeedItem
import com.example.dexreader.tracker.work.TrackWorker
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

private const val PAGE_SIZE = 20

@HiltViewModel
class FeedViewModel @Inject constructor(
	private val repository: TrackingRepository,
	private val scheduler: TrackWorker.Scheduler,
	private val listExtraProvider: ListExtraProvider,
) : BaseViewModel() {

	private val limit = MutableStateFlow(PAGE_SIZE)
	private val isReady = AtomicBoolean(false)

	val isRunning = scheduler.observeIsRunning()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, false)

	val onFeedCleared = MutableEventFlow<Unit>()
	val content = combine(
		observeHeader(),
		repository.observeTrackingLog(limit),
	) { header, list ->
		val result = ArrayList<ListModel>((list.size * 1.4).toInt().coerceAtLeast(2))
		if (header != null) {
			result += header
		}
		if (list.isEmpty()) {
			result += EmptyState(
				icon = R.drawable.ic_empty_feed,
				textPrimary = R.string.text_empty_holder_primary,
				textSecondary = R.string.text_feed_holder,
				actionStringRes = 0,
			)
		} else {
			isReady.set(true)
			list.mapListTo(result)
		}
		result
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(LoadingState))

	init {
		launchJob(Dispatchers.Default) {
			repository.gc()
		}
	}

	fun clearFeed(clearCounters: Boolean) {
		launchLoadingJob(Dispatchers.Default) {
			repository.clearLogs()
			if (clearCounters) {
				repository.clearCounters()
			}
			onFeedCleared.call(Unit)
		}
	}

	fun requestMoreItems() {
		if (isReady.compareAndSet(true, false)) {
			limit.value += PAGE_SIZE
		}
	}

	fun update() {
		scheduler.startNow()
	}

	private fun List<TrackingLogItem>.mapListTo(destination: MutableList<ListModel>) {
		var prevDate: DateTimeAgo? = null
		for (item in this) {
			val date = calculateTimeAgo(item.createdAt)
			if (prevDate != date) {
				destination += ListHeader(date)
			}
			prevDate = date
			destination += item.toFeedItem()
		}
	}

	private fun observeHeader() = repository.observeUpdatedManga(10).map { mangaList ->
		if (mangaList.isEmpty()) {
			null
		} else {
			UpdatedMangaHeader(mangaList.toUi(ListMode.GRID, listExtraProvider))
		}
	}
}
