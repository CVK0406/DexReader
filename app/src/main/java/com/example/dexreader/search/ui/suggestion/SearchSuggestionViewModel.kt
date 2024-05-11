package com.example.dexreader.search.ui.suggestion

import androidx.lifecycle.viewModelScope
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.ui.BaseViewModel
import com.example.dexreader.core.ui.widgets.ChipsView
import com.example.dexreader.core.util.ext.toEnumSet
import com.example.dexreader.explore.data.MangaSourcesRepository
import com.example.dexreader.search.domain.MangaSearchRepository
import com.example.dexreader.search.ui.suggestion.model.SearchSuggestionItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import org.example.dexreader.parsers.model.MangaSource
import org.example.dexreader.parsers.model.MangaTag
import javax.inject.Inject

private const val DEBOUNCE_TIMEOUT = 500L
private const val MAX_MANGA_ITEMS = 6
private const val MAX_QUERY_ITEMS = 16
private const val MAX_TAGS_ITEMS = 8
private const val MAX_SOURCES_ITEMS = 6

@HiltViewModel
class SearchSuggestionViewModel @Inject constructor(
	private val repository: MangaSearchRepository,
	private val settings: AppSettings,
	private val sourcesRepository: MangaSourcesRepository,
) : BaseViewModel() {

	private val query = MutableStateFlow("")
	private var suggestionJob: Job? = null
	private var invalidateOnResume = false

	val suggestion = MutableStateFlow<List<SearchSuggestionItem>>(emptyList())

	init {
		setupSuggestion()
	}

	fun onQueryChanged(newQuery: String) {
		query.value = newQuery
	}

	fun saveQuery(query: String) {
		repository.saveSearchQuery(query)
		invalidateOnResume = true
	}

	fun clearSearchHistory() {
		launchJob {
			repository.clearSearchHistory()
			setupSuggestion()
		}
	}

	fun onSourceToggle(source: MangaSource, isEnabled: Boolean) {
		launchJob(Dispatchers.Default) {
			sourcesRepository.setSourceEnabled(source, isEnabled)
		}
	}

	fun onResume() {
		if (invalidateOnResume) {
			invalidateOnResume = false
			setupSuggestion()
		}
	}

	fun deleteQuery(query: String) {
		launchJob {
			repository.deleteSearchQuery(query)
			setupSuggestion()
		}
	}

	private fun setupSuggestion() {
		suggestionJob?.cancel()
		suggestionJob = combine(
			query.debounce(DEBOUNCE_TIMEOUT),
			sourcesRepository.observeEnabledSources().map { it.toEnumSet() },
			::Pair,
		).mapLatest { (searchQuery, enabledSources) ->
			buildSearchSuggestion(searchQuery, enabledSources)
		}.distinctUntilChanged()
			.onEach {
				suggestion.value = it
			}.launchIn(viewModelScope + Dispatchers.Default)
	}

	private suspend fun buildSearchSuggestion(
		searchQuery: String,
		enabledSources: Set<MangaSource>,
	): List<SearchSuggestionItem> = coroutineScope {
		val queriesDeferred = async {
			repository.getQuerySuggestion(searchQuery, MAX_QUERY_ITEMS)
		}
		val tagsDeferred = async {
			repository.getTagsSuggestion(searchQuery, MAX_TAGS_ITEMS, null)
		}
		val mangaDeferred = async {
			repository.getMangaSuggestion(searchQuery, MAX_MANGA_ITEMS, null)
		}
		val sources = repository.getSourcesSuggestion(searchQuery, MAX_SOURCES_ITEMS)

		val tags = tagsDeferred.await()
		val mangaList = mangaDeferred.await()
		val queries = queriesDeferred.await()

		buildList(queries.size + sources.size + 2) {
			if (tags.isNotEmpty()) {
				add(SearchSuggestionItem.Tags(mapTags(tags)))
			}
			if (mangaList.isNotEmpty()) {
				add(SearchSuggestionItem.MangaList(mangaList))
			}
			sources.mapTo(this) { SearchSuggestionItem.Source(it, it in enabledSources) }
			queries.mapTo(this) { SearchSuggestionItem.RecentQuery(it) } }
		}
	}

	private fun mapTags(tags: List<MangaTag>): List<ChipsView.ChipModel> = tags.map { tag ->
		ChipsView.ChipModel(
			tint = 0,
			title = tag.title,
			icon = 0,
			data = tag,
			isCheckable = false,
			isChecked = false,
		)
	}

