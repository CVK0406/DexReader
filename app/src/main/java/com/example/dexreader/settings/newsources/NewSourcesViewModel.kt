package com.example.dexreader.settings.newsources

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.ui.BaseViewModel
import com.example.dexreader.explore.data.MangaSourcesRepository
import org.example.dexreader.parsers.model.ContentType
import org.example.dexreader.parsers.util.SuspendLazy
import com.example.dexreader.settings.sources.model.SourceConfigItem
import javax.inject.Inject

@HiltViewModel
class NewSourcesViewModel @Inject constructor(
	private val repository: MangaSourcesRepository,
	private val settings: AppSettings,
) : BaseViewModel() {

	private val newSources = SuspendLazy {
		repository.assimilateNewSources()
	}
	val content: StateFlow<List<SourceConfigItem>> = repository.observeAll()
		.map { sources ->
			val new = newSources.get()
			val skipNsfw = settings.isNsfwContentDisabled
			sources.mapNotNull { (source, enabled) ->
				if (source in new) {
					SourceConfigItem.SourceItem(
						source = source,
						isEnabled = enabled,
						isDraggable = false,
						isAvailable = !skipNsfw || source.contentType != ContentType.HENTAI,
					)
				} else {
					null
				}
			}
		}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, emptyList())

	fun onItemEnabledChanged(item: SourceConfigItem.SourceItem, isEnabled: Boolean) {
		launchJob(Dispatchers.Default) {
			repository.setSourceEnabled(item.source, isEnabled)
		}
	}
}

