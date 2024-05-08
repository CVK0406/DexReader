package com.example.dexreader.list.ui

import androidx.lifecycle.viewModelScope
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.prefs.observeAsFlow
import com.example.dexreader.core.prefs.observeAsStateFlow
import com.example.dexreader.core.ui.BaseViewModel
import com.example.dexreader.core.ui.util.ReversibleAction
import com.example.dexreader.core.util.ext.MutableEventFlow
import com.example.dexreader.core.util.ext.call
import com.example.dexreader.download.ui.worker.DownloadWorker
import com.example.dexreader.list.ui.model.ListModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.model.MangaTag

abstract class MangaListViewModel(
	private val settings: AppSettings,
	private val downloadScheduler: DownloadWorker.Scheduler,
) : BaseViewModel() {

	abstract val content: StateFlow<List<ListModel>>
	open val listMode = settings.observeAsFlow(AppSettings.KEY_LIST_MODE) { listMode }
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, settings.listMode)
	val onActionDone = MutableEventFlow<ReversibleAction>()
	val gridScale = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_GRID_SIZE,
		valueProducer = { gridSize / 100f },
	)
	val onDownloadStarted = MutableEventFlow<Unit>()

	open fun onUpdateFilter(tags: Set<MangaTag>) = Unit

	abstract fun onRefresh()

	abstract fun onRetry()

	fun download(items: Set<Manga>) {
		launchJob(Dispatchers.Default) {
			downloadScheduler.schedule(items)
			onDownloadStarted.call(Unit)
		}
	}

	fun List<Manga>.skipNsfwIfNeeded() = if (settings.isNsfwContentDisabled) {
		filterNot { it.isNsfw }
	} else {
		this
	}
}
