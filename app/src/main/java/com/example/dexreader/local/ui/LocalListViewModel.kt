package com.example.dexreader.local.ui

import android.content.SharedPreferences
import androidx.lifecycle.SavedStateHandle
import com.example.dexreader.R
import com.example.dexreader.core.parser.MangaRepository
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.util.ext.MutableEventFlow
import com.example.dexreader.core.util.ext.call
import com.example.dexreader.download.ui.worker.DownloadWorker
import com.example.dexreader.explore.domain.ExploreRepository
import com.example.dexreader.filter.ui.FilterCoordinator
import com.example.dexreader.list.domain.ListExtraProvider
import com.example.dexreader.list.ui.model.EmptyState
import com.example.dexreader.local.data.LocalStorageChanges
import com.example.dexreader.local.domain.DeleteLocalMangaUseCase
import com.example.dexreader.local.domain.model.LocalManga
import com.example.dexreader.remotelist.ui.RemoteListViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

@HiltViewModel
class LocalListViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	mangaRepositoryFactory: MangaRepository.Factory,
	filter: FilterCoordinator,
	private val settings: AppSettings,
	downloadScheduler: DownloadWorker.Scheduler,
	listExtraProvider: ListExtraProvider,
	private val deleteLocalMangaUseCase: DeleteLocalMangaUseCase,
	exploreRepository: ExploreRepository,
	@LocalStorageChanges private val localStorageChanges: SharedFlow<LocalManga?>,
) : RemoteListViewModel(
	savedStateHandle,
	mangaRepositoryFactory,
	filter,
	settings,
	listExtraProvider,
	downloadScheduler,
	exploreRepository,
), SharedPreferences.OnSharedPreferenceChangeListener {

	val onMangaRemoved = MutableEventFlow<Unit>()

	init {
		launchJob(Dispatchers.Default) {
			localStorageChanges
				.collect {
					loadList(filter.snapshot(), append = false).join()
				}
		}
		settings.subscribe(this)
	}

	override fun onCleared() {
		settings.unsubscribe(this)
		super.onCleared()
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		if (key == AppSettings.KEY_LOCAL_MANGA_DIRS) {
			onRefresh()
		}
	}

	fun delete(ids: Set<Long>) {
		launchLoadingJob(Dispatchers.Default) {
			deleteLocalMangaUseCase(ids)
			onMangaRemoved.call(Unit)
		}
	}

	override fun createEmptyState(canResetFilter: Boolean): EmptyState {
		return EmptyState(
			icon = R.drawable.ic_empty_local,
			textPrimary = R.string.text_local_holder_primary,
			textSecondary = R.string.text_local_holder_secondary,
			actionStringRes = R.string._import,
		)
	}
}
