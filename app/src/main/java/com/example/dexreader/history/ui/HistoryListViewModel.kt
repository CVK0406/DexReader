package com.example.dexreader.history.ui

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import com.example.dexreader.R
import com.example.dexreader.core.model.MangaHistory
import com.example.dexreader.core.model.isLocal
import com.example.dexreader.core.os.NetworkState
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.prefs.ListMode
import com.example.dexreader.core.prefs.observeAsFlow
import com.example.dexreader.core.prefs.observeAsStateFlow
import com.example.dexreader.core.ui.util.ReversibleAction
import com.example.dexreader.core.util.ext.calculateTimeAgo
import com.example.dexreader.core.util.ext.call
import com.example.dexreader.core.util.ext.onFirst
import com.example.dexreader.download.ui.worker.DownloadWorker
import com.example.dexreader.history.data.HistoryRepository
import com.example.dexreader.history.domain.MarkAsReadUseCase
import com.example.dexreader.history.domain.model.MangaWithHistory
import com.example.dexreader.list.domain.ListExtraProvider
import com.example.dexreader.list.domain.ListSortOrder
import com.example.dexreader.list.ui.MangaListViewModel
import com.example.dexreader.list.ui.model.EmptyHint
import com.example.dexreader.list.ui.model.EmptyState
import com.example.dexreader.list.ui.model.ListHeader
import com.example.dexreader.list.ui.model.ListModel
import com.example.dexreader.list.ui.model.LoadingState
import com.example.dexreader.list.ui.model.toErrorState
import com.example.dexreader.list.ui.model.toGridModel
import com.example.dexreader.list.ui.model.toListDetailedModel
import com.example.dexreader.list.ui.model.toListModel
import com.example.dexreader.local.data.LocalMangaRepository
import org.example.dexreader.parsers.model.Manga
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class HistoryListViewModel @Inject constructor(
	private val repository: HistoryRepository,
	settings: AppSettings,
	private val extraProvider: ListExtraProvider,
	private val localMangaRepository: LocalMangaRepository,
	private val markAsReadUseCase: MarkAsReadUseCase,
	networkState: NetworkState,
	downloadScheduler: DownloadWorker.Scheduler,
) : MangaListViewModel(settings, downloadScheduler) {

	private val sortOrder: StateFlow<ListSortOrder> = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.IO,
		key = AppSettings.KEY_HISTORY_ORDER,
		valueProducer = { historySortOrder },
	)

	override val listMode = settings.observeAsFlow(AppSettings.KEY_LIST_MODE_HISTORY) { historyListMode }
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, settings.historyListMode)

	private val isGroupingEnabled = settings.observeAsFlow(
		key = AppSettings.KEY_HISTORY_GROUPING,
		valueProducer = { isHistoryGroupingEnabled },
	).combine(sortOrder) { g, s ->
		g && s.isGroupingSupported()
	}

	val isStatsEnabled = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_STATS_ENABLED,
		valueProducer = { isStatsEnabled },
	)

	override val content = combine(
		sortOrder.flatMapLatest { repository.observeAllWithHistory(it) },
		isGroupingEnabled,
		listMode,
		networkState,
	) { list, grouped, mode, online ->
		when {
			list.isEmpty() -> listOf(
				EmptyState(
					icon = R.drawable.ic_empty_history,
					textPrimary = R.string.text_history_holder_primary,
					textSecondary = R.string.text_history_holder_secondary,
					actionStringRes = 0,
				),
			)

			else -> mapList(list, grouped, mode, online)
		}
	}.onStart {
		loadingCounter.increment()
	}.onFirst {
		loadingCounter.decrement()
	}.catch {
		emit(listOf(it.toErrorState(canRetry = false)))
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(LoadingState))

	override fun onRefresh() = Unit

	override fun onRetry() = Unit

	fun clearHistory(minDate: Instant) {
		launchJob(Dispatchers.Default) {
			val stringRes = if (minDate <= Instant.EPOCH) {
				repository.clear()
				R.string.history_cleared
			} else {
				repository.deleteAfter(minDate.toEpochMilli())
				R.string.removed_from_history
			}
			onActionDone.call(ReversibleAction(stringRes, null))
		}
	}

	fun removeFromHistory(ids: Set<Long>) {
		if (ids.isEmpty()) {
			return
		}
		launchJob(Dispatchers.Default) {
			val handle = repository.delete(ids)
			onActionDone.call(ReversibleAction(R.string.removed_from_history, handle))
		}
	}

	fun markAsRead(items: Set<Manga>) {
		launchLoadingJob(Dispatchers.Default) {
			markAsReadUseCase(items)
		}
	}

	private suspend fun mapList(
		list: List<MangaWithHistory>,
		grouped: Boolean,
		mode: ListMode,
		isOnline: Boolean,
	): List<ListModel> {
		val result = ArrayList<ListModel>(if (grouped) (list.size * 1.4).toInt() else list.size + 1)
		val order = sortOrder.value
		var prevHeader: ListHeader? = null
		if (!isOnline) {
			result += EmptyHint(
				icon = R.drawable.ic_empty_common,
				textPrimary = R.string.network_unavailable,
				textSecondary = R.string.network_unavailable_hint,
				actionStringRes = R.string.manage,
			)
		}
		for ((m, history) in list) {
			val manga = if (!isOnline && !m.isLocal) {
				localMangaRepository.findSavedManga(m)?.manga ?: continue
			} else {
				m
			}
			if (grouped) {
				val header = history.header(order)
				if (header != prevHeader) {
					if (header != null) {
						result += header
					}
					prevHeader = header
				}
			}
			result += when (mode) {
				ListMode.LIST -> manga.toListModel(extraProvider)
				ListMode.DETAILED_LIST -> manga.toListDetailedModel(extraProvider)
				ListMode.GRID -> manga.toGridModel(extraProvider)
			}
		}
		return result
	}

	private fun MangaHistory.header(order: ListSortOrder): ListHeader? = when (order) {
		ListSortOrder.LAST_READ,
		ListSortOrder.LONG_AGO_READ -> ListHeader(calculateTimeAgo(updatedAt))

		ListSortOrder.OLDEST,
		ListSortOrder.NEWEST -> ListHeader(calculateTimeAgo(createdAt))

		ListSortOrder.UNREAD,
		ListSortOrder.PROGRESS -> ListHeader(
			when (percent) {
				1f -> R.string.status_completed
				in 0f..0.01f -> R.string.status_planned
				in 0f..1f -> R.string.status_reading
				else -> R.string.unknown
			},
		)

		ListSortOrder.ALPHABETIC,
		ListSortOrder.ALPHABETIC_REVERSE,
		ListSortOrder.RELEVANCE,
		ListSortOrder.NEW_CHAPTERS,
		ListSortOrder.RATING -> null
	}
}
