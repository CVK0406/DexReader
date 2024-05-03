package com.example.dexreader.settings.sources.catalog

import androidx.room.InvalidationTracker
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.ViewModelLifecycle
import dagger.hilt.android.lifecycle.RetainedLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.dexreader.R
import com.example.dexreader.core.db.MangaDatabase
import com.example.dexreader.core.db.TABLE_SOURCES
import com.example.dexreader.core.db.removeObserverAsync
import com.example.dexreader.core.util.ext.lifecycleScope
import com.example.dexreader.explore.data.MangaSourcesRepository
import org.example.dexreader.parsers.model.ContentType

class SourcesCatalogListProducer @AssistedInject constructor(
	@Assisted private val locale: String?,
	@Assisted private val contentType: ContentType,
	@Assisted lifecycle: ViewModelLifecycle,
	private val repository: MangaSourcesRepository,
	private val database: MangaDatabase,
) : InvalidationTracker.Observer(TABLE_SOURCES), RetainedLifecycle.OnClearedListener {

	private val scope = lifecycle.lifecycleScope
	private var query: String? = null
	val list = MutableStateFlow(emptyList<SourceCatalogItem>())

	private var job = scope.launch(Dispatchers.Default) {
		list.value = buildList()
	}

	init {
		scope.launch(Dispatchers.Default) {
			database.invalidationTracker.addObserver(this@SourcesCatalogListProducer)
		}
		lifecycle.addOnClearedListener(this)
	}

	override fun onCleared() {
		database.invalidationTracker.removeObserverAsync(this)
	}

	override fun onInvalidated(tables: Set<String>) {
		val prevJob = job
		job = scope.launch(Dispatchers.Default) {
			prevJob.cancelAndJoin()
			list.update { buildList() }
		}
	}

	fun setQuery(value: String?) {
		this.query = value
		onInvalidated(emptySet())
	}

	private suspend fun buildList(): List<SourceCatalogItem> {
		val sources = repository.getDisabledSources().toMutableList()
		when (val q = query) {
			null -> sources.retainAll { it.contentType == contentType && it.locale == locale }
			"" -> return emptyList()
			else -> sources.retainAll { it.title.contains(q, ignoreCase = true) }
		}
		return if (sources.isEmpty()) {
			listOf(
				if (query == null) {
					SourceCatalogItem.Hint(
						icon = R.drawable.ic_empty_feed,
						title = R.string.no_manga_sources,
						text = R.string.no_manga_sources_catalog_text,
					)
				} else {
					SourceCatalogItem.Hint(
						icon = R.drawable.ic_empty_feed,
						title = R.string.nothing_found,
						text = R.string.no_manga_sources_found,
					)
				},
			)
		} else {
			sources.sortBy { it.title }
			sources.map {
				SourceCatalogItem.Source(
					source = it,
					showSummary = query != null,
				)
			}
		}
	}

	@AssistedFactory
	interface Factory {

		fun create(
			locale: String?,
			contentType: ContentType,
			lifecycle: ViewModelLifecycle,
		): SourcesCatalogListProducer
	}
}
