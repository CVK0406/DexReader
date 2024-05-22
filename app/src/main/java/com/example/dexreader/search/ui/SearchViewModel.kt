package com.example.dexreader.search.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.dexreader.R
import com.example.dexreader.core.parser.MangaRepository
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.util.ext.require
import com.example.dexreader.download.ui.worker.DownloadWorker
import com.example.dexreader.list.domain.ListExtraProvider
import com.example.dexreader.list.ui.MangaListViewModel
import com.example.dexreader.list.ui.model.EmptyState
import com.example.dexreader.list.ui.model.ListModel
import com.example.dexreader.list.ui.model.LoadingFooter
import com.example.dexreader.list.ui.model.LoadingState
import com.example.dexreader.list.ui.model.toErrorFooter
import com.example.dexreader.list.ui.model.toErrorState
import com.example.dexreader.list.ui.model.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.model.MangaListFilter
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	repositoryFactory: MangaRepository.Factory,
	settings: AppSettings,
	private val extraProvider: ListExtraProvider,
	downloadScheduler: DownloadWorker.Scheduler,
) : MangaListViewModel(settings, downloadScheduler) {

	private val query = savedStateHandle.require<String>(SearchFragment.ARG_QUERY)
	private val repository = repositoryFactory.create(savedStateHandle.require(SearchFragment.ARG_SOURCE))
	private val mangaList = MutableStateFlow<List<Manga>?>(null)
	private val hasNextPage = MutableStateFlow(false)
	private val listError = MutableStateFlow<Throwable?>(null)
	private var loadingJob: Job? = null

	override val content = combine(
		mangaList,
		listMode,
		listError,
		hasNextPage,
	) { list, mode, error, hasNext ->
		when {
			list.isNullOrEmpty() && error != null -> listOf(error.toErrorState(canRetry = true))
			list == null -> listOf(LoadingState)
			list.isEmpty() -> listOf(
				EmptyState(
					icon = R.drawable.ic_empty_common,
					textPrimary = R.string.nothing_found,
					textSecondary = R.string.text_search_holder_secondary,
					actionStringRes = 0,
				),
			)

			else -> {
				val result = ArrayList<ListModel>(list.size + 1)
				list.toUi(result, mode, extraProvider)
				when {
					error != null -> result += error.toErrorFooter()
					hasNext -> result += LoadingFooter()
				}
				result
			}
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(LoadingState))

	init {
		loadList(append = false)
	}

	override fun onRefresh() {
		loadList(append = false)
	}

	override fun onRetry() {
		loadList(append = !mangaList.value.isNullOrEmpty())
	}

	fun loadNextPage() {
		if (hasNextPage.value && listError.value == null) {
			loadList(append = true)
		}
	}

	private fun loadList(append: Boolean) {
		if (loadingJob?.isActive == true) {
			return
		}
		loadingJob = launchLoadingJob(Dispatchers.Default) {
			try {
				listError.value = null
				val list = repository.getList(
					offset = if (append) mangaList.value?.size ?: 0 else 0,
					filter = MangaListFilter.Search(query)
				)
				if (!append) {
					mangaList.value = list
				} else if (list.isNotEmpty()) {
					mangaList.value = mangaList.value?.plus(list) ?: list
				}
				hasNextPage.value = list.isNotEmpty()
			} catch (e: CancellationException) {
				throw e
			} catch (e: Throwable) {
				listError.value = e
			}
		}
	}
}
