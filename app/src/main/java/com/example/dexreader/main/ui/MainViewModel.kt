package com.example.dexreader.main.ui

import androidx.lifecycle.viewModelScope
import com.example.dexreader.core.exceptions.EmptyHistoryException
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.ui.BaseViewModel
import com.example.dexreader.core.util.ext.MutableEventFlow
import com.example.dexreader.core.util.ext.call
import com.example.dexreader.explore.data.MangaSourcesRepository
import com.example.dexreader.history.data.HistoryRepository
import com.example.dexreader.main.domain.ReadingResumeEnabledUseCase
import com.example.dexreader.tracker.domain.TrackingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.example.dexreader.parsers.model.Manga
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
	private val historyRepository: HistoryRepository,
	trackingRepository: TrackingRepository,
	private val settings: AppSettings,
	readingResumeEnabledUseCase: ReadingResumeEnabledUseCase,
	private val sourcesRepository: MangaSourcesRepository,
) : BaseViewModel() {

	val onOpenReader = MutableEventFlow<Manga>()
	val onFirstStart = MutableEventFlow<Unit>()

	val isResumeEnabled = readingResumeEnabledUseCase().stateIn(
		scope = viewModelScope + Dispatchers.Default,
		started = SharingStarted.WhileSubscribed(5000),
		initialValue = false,
	)

	val feedCounter = trackingRepository.observeUpdatedMangaCount()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, 0)

	init {
		launchJob(Dispatchers.Default) {
			if (sourcesRepository.isSetupRequired()) {
				onFirstStart.call(Unit)
			}
		}
	}

	fun openLastReader() {
		launchLoadingJob(Dispatchers.Default) {
			val manga = historyRepository.getLastOrNull() ?: throw EmptyHistoryException()
			onOpenReader.call(manga)
		}
	}
}
