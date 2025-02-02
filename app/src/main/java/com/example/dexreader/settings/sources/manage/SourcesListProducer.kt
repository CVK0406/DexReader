package com.example.dexreader.settings.sources.manage

import androidx.room.InvalidationTracker
import dagger.hilt.android.ViewModelLifecycle
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.dexreader.R
import com.example.dexreader.core.db.TABLE_SOURCES
import com.example.dexreader.core.model.isNsfw
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.util.ext.lifecycleScope
import com.example.dexreader.core.util.ext.toEnumSet
import com.example.dexreader.explore.data.MangaSourcesRepository
import com.example.dexreader.explore.data.SourcesSortOrder
import com.example.dexreader.settings.sources.model.SourceConfigItem
import javax.inject.Inject

@ViewModelScoped
class SourcesListProducer @Inject constructor(
	lifecycle: ViewModelLifecycle,
	private val repository: MangaSourcesRepository,
	private val settings: AppSettings,
) : InvalidationTracker.Observer(TABLE_SOURCES) {

	private val scope = lifecycle.lifecycleScope
	private var query: String = ""
	val list = MutableStateFlow(emptyList<SourceConfigItem>())

	private var job = scope.launch(Dispatchers.Default) {
		list.value = buildList()
	}

	init {
		settings.observe()
			.filter { it == AppSettings.KEY_TIPS_CLOSED || it == AppSettings.KEY_DISABLE_NSFW }
			.flowOn(Dispatchers.Default)
			.onEach { onInvalidated(emptySet()) }
			.launchIn(scope)
	}

	override fun onInvalidated(tables: Set<String>) {
		val prevJob = job
		job = scope.launch(Dispatchers.Default) {
			prevJob.cancelAndJoin()
			list.update { buildList() }
		}
	}

	fun setQuery(value: String) {
		this.query = value
		onInvalidated(emptySet())
	}

	private suspend fun buildList(): List<SourceConfigItem> {
		val enabledSources = repository.getEnabledSources()
		val isNsfwDisabled = settings.isNsfwContentDisabled
		val isReorderAvailable = settings.sourcesSortOrder == SourcesSortOrder.MANUAL
		val withTip = isReorderAvailable && settings.isTipEnabled(TIP_REORDER)
		val enabledSet = enabledSources.toEnumSet()
		if (query.isNotEmpty()) {
			return enabledSources.mapNotNull {
				if (!it.title.contains(query, ignoreCase = true)) {
					return@mapNotNull null
				}
				SourceConfigItem.SourceItem(
					source = it,
					isEnabled = it in enabledSet,
					isDraggable = false,
					isAvailable = !isNsfwDisabled || !it.isNsfw(),
				)
			}.ifEmpty {
				listOf(SourceConfigItem.EmptySearchResult)
			}
		}
		val result = ArrayList<SourceConfigItem>(enabledSources.size + 1)
		if (enabledSources.isNotEmpty()) {
			if (withTip) {
				result += SourceConfigItem.Tip(
					TIP_REORDER,
					R.drawable.ic_tap_reorder,
					R.string.sources_reorder_tip,
				)
			}
			enabledSources.mapTo(result) {
				SourceConfigItem.SourceItem(
					source = it,
					isEnabled = true,
					isDraggable = isReorderAvailable,
					isAvailable = false,
				)
			}
		}
		return result
	}

	companion object {

		const val TIP_REORDER = "src_reorder"
	}
}
