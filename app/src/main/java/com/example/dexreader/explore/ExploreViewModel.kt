package com.example.dexreader.explore.ui

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import com.example.dexreader.R
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.prefs.observeAsFlow
import com.example.dexreader.core.prefs.observeAsStateFlow
import com.example.dexreader.core.ui.BaseViewModel
import com.example.dexreader.core.ui.util.ReversibleAction
import com.example.dexreader.core.util.ext.MutableEventFlow
import com.example.dexreader.core.util.ext.call
import com.example.dexreader.explore.data.MangaSourcesRepository
import com.example.dexreader.explore.domain.ExploreRepository
import com.example.dexreader.explore.ui.model.ExploreButtons
import com.example.dexreader.explore.ui.model.MangaSourceItem
import com.example.dexreader.explore.ui.model.RecommendationsItem
import com.example.dexreader.list.ui.model.EmptyHint
import com.example.dexreader.list.ui.model.ListHeader
import com.example.dexreader.list.ui.model.ListModel
import com.example.dexreader.list.ui.model.LoadingState
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.model.MangaSource
import org.example.dexreader.parsers.util.runCatchingCancellable
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
	private val settings: AppSettings,
	private val exploreRepository: ExploreRepository,
	private val sourcesRepository: MangaSourcesRepository,
) : BaseViewModel() {

	val isGrid = settings.observeAsStateFlow(
		key = AppSettings.KEY_SOURCES_GRID,
		scope = viewModelScope + Dispatchers.IO,
		valueProducer = { isSourcesGridMode },
	)

	private val isSuggestionsEnabled = settings.observeAsFlow(
		key = AppSettings.KEY_SUGGESTIONS,
		valueProducer = { isSuggestionsEnabled },
	)

	val onOpenManga = MutableEventFlow<Manga>()
	val onActionDone = MutableEventFlow<ReversibleAction>()
	val onShowSuggestionsTip = MutableEventFlow<Unit>()
	private val isRandomLoading = MutableStateFlow(false)

	val content: StateFlow<List<ListModel>> = isLoading.flatMapLatest { loading ->
		if (loading) {
			flowOf(getLoadingStateList())
		} else {
			createContentFlow()
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, getLoadingStateList())

	init {
		launchJob(Dispatchers.Default) {
			if (!settings.isSuggestionsEnabled && settings.isTipEnabled(TIP_SUGGESTIONS)) {
				onShowSuggestionsTip.call(Unit)
			}
		}
	}

	fun openRandom() {
		if (isRandomLoading.value) {
			return
		}
		launchJob(Dispatchers.Default) {
			isRandomLoading.value = true
			try {
				val manga = exploreRepository.findRandomManga(tagsLimit = 8)
				onOpenManga.call(manga)
			} finally {
				isRandomLoading.value = false
			}
		}
	}

	fun hideSource(source: MangaSource) {
		launchJob(Dispatchers.Default) {
			val rollback = sourcesRepository.setSourceEnabled(source, isEnabled = false)
			onActionDone.call(ReversibleAction(R.string.source_disabled, rollback))
		}
	}

	fun discardNewSources() {
		launchJob(Dispatchers.Default) {
			sourcesRepository.assimilateNewSources()
		}
	}

	fun respondSuggestionTip(isAccepted: Boolean) {
		settings.isSuggestionsEnabled = isAccepted
		settings.closeTip(TIP_SUGGESTIONS)
	}

	private fun createContentFlow() = combine(
		sourcesRepository.observeEnabledSources(),
		isGrid,
		isRandomLoading,
		sourcesRepository.observeNewSources(),
	) { content, grid, randomLoading, newSources ->
		buildList(content, grid, randomLoading, newSources)
	}

	private fun buildList(
		sources: List<MangaSource>,
		isGrid: Boolean,
		randomLoading: Boolean,
		newSources: Set<MangaSource>,
	): List<ListModel> {
		val result = ArrayList<ListModel>(sources.size + 3)
		result += ExploreButtons(randomLoading)
		if (sources.isNotEmpty()) {
			result += ListHeader(
				textRes = R.string.remote_sources,
				buttonTextRes = R.string.catalog,
				badge = if (newSources.isNotEmpty()) "" else null,
			)
			sources.mapTo(result) { MangaSourceItem(it, isGrid) }
		} else {
			result += EmptyHint(
				icon = R.drawable.ic_empty_common,
				textPrimary = R.string.no_manga_sources,
				textSecondary = R.string.no_manga_sources_text,
				actionStringRes = R.string.catalog,
			)
		}
		return result
	}

	private fun getLoadingStateList() = listOf(
		ExploreButtons(isRandomLoading.value),
		LoadingState,
	)



	companion object {

		private const val TIP_SUGGESTIONS = "suggestions"
		const val TIP_NEW_SOURCES = "new_sources"
	}
}
