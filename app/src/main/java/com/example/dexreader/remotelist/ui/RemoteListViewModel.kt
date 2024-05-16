package com.example.dexreader.remotelist.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import com.example.dexreader.R
import com.example.dexreader.core.model.distinctById
import com.example.dexreader.core.parser.MangaRepository
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.util.ext.MutableEventFlow
import com.example.dexreader.core.util.ext.call
import com.example.dexreader.core.util.ext.printStackTraceDebug
import com.example.dexreader.core.util.ext.require
import com.example.dexreader.download.ui.worker.DownloadWorker
import com.example.dexreader.explore.domain.ExploreRepository
import com.example.dexreader.filter.ui.FilterCoordinator
import com.example.dexreader.filter.ui.MangaFilter
import com.example.dexreader.list.domain.ListExtraProvider
import com.example.dexreader.list.ui.MangaListViewModel
import com.example.dexreader.list.ui.model.EmptyState
import com.example.dexreader.list.ui.model.LoadingFooter
import com.example.dexreader.list.ui.model.LoadingState
import com.example.dexreader.list.ui.model.toErrorFooter
import com.example.dexreader.list.ui.model.toErrorState
import com.example.dexreader.list.ui.model.toUi
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.model.MangaListFilter
import org.example.dexreader.parsers.model.MangaSource
import org.example.dexreader.parsers.model.MangaTag
import javax.inject.Inject

private const val FILTER_MIN_INTERVAL = 250L

@HiltViewModel
open class RemoteListViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	mangaRepositoryFactory: MangaRepository.Factory,
	private val filter: FilterCoordinator,
	settings: AppSettings,
	listExtraProvider: ListExtraProvider,
	downloadScheduler: DownloadWorker.Scheduler,
	private val exploreRepository: ExploreRepository,
) : MangaListViewModel(settings, downloadScheduler), MangaFilter by filter {

	val source = savedStateHandle.require<MangaSource>(RemoteListFragment.ARG_SOURCE)
	val isRandomLoading = MutableStateFlow(false)
	val onOpenManga = MutableEventFlow<Manga>()

	private val repository = mangaRepositoryFactory.create(source)
	private val mangaList = MutableStateFlow<List<Manga>?>(null)
	private val hasNextPage = MutableStateFlow(false)
	private val listError = MutableStateFlow<Throwable?>(null)
	private var loadingJob: Job? = null
	private var randomJob: Job? = null

	val isSearchAvailable: Boolean
		get() = repository.isSearchSupported

	override val content = combine(
		mangaList.map { it?.distinctById()?.skipNsfwIfNeeded() },
		listMode,
		listError,
		hasNextPage,
	) { list, mode, error, hasNext ->
		buildList(list?.size?.plus(2) ?: 2) {
			when {
				list.isNullOrEmpty() && error != null -> add(error.toErrorState(canRetry = true))
				list == null -> add(LoadingState)
				list.isEmpty() -> add(createEmptyState(canResetFilter = header.value.isFilterApplied))
				else -> {
					list.toUi(this, mode, listExtraProvider)
					when {
						error != null -> add(error.toErrorFooter())
						hasNext -> add(LoadingFooter())
					}
				}
			}
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(LoadingState))

	init {
		filter.observeState()
			.debounce(FILTER_MIN_INTERVAL)
			.onEach { filterState ->
				loadingJob?.cancelAndJoin()
				mangaList.value = null
				loadList(filterState, false)
			}.catch { error ->
				listError.value = error
			}.launchIn(viewModelScope)
	}

	override fun onRefresh() {
		loadList(filter.snapshot(), append = false)
	}

	override fun onRetry() {
		loadList(filter.snapshot(), append = !mangaList.value.isNullOrEmpty())
	}

	fun loadNextPage() {
		if (hasNextPage.value && listError.value == null) {
			loadList(filter.snapshot(), append = true)
		}
	}

	fun resetFilter() = filter.reset()

	override fun onUpdateFilter(tags: Set<MangaTag>) {
		applyFilter(tags)
	}

	protected fun loadList(filterState: MangaListFilter.Advanced, append: Boolean): Job {
		loadingJob?.let {
			if (it.isActive) return it
		}
		return launchLoadingJob(Dispatchers.Default) {
			try {
				listError.value = null
				val list = repository.getList(
					offset = if (append) mangaList.value?.size ?: 0 else 0,
					filter = filterState,
				)
				val oldList = mangaList.getAndUpdate { oldList ->
					if (!append || oldList.isNullOrEmpty()) {
						list
					} else {
						oldList + list
					}
				}.orEmpty()
				hasNextPage.value = if (append) {
					list.isNotEmpty()
				} else {
					list.size > oldList.size || hasNextPage.value
				}
			} catch (e: CancellationException) {
				throw e
			} catch (e: Throwable) {
				e.printStackTraceDebug()
				listError.value = e
				if (!mangaList.value.isNullOrEmpty()) {
					errorEvent.call(e)
				}
				hasNextPage.value = false
			}
		}.also { loadingJob = it }
	}

	protected open fun createEmptyState(canResetFilter: Boolean) = EmptyState(
		icon = R.drawable.ic_empty_common,
		textPrimary = R.string.nothing_found,
		textSecondary = 0,
		actionStringRes = if (canResetFilter) R.string.reset_filter else 0,
	)

	fun openRandom() {
		if (randomJob?.isActive == true) {
			return
		}
		randomJob = launchLoadingJob(Dispatchers.Default) {
			isRandomLoading.value = true
			val manga = exploreRepository.findRandomManga(source, 16)
			onOpenManga.call(manga)
			isRandomLoading.value = false
		}
	}
}
