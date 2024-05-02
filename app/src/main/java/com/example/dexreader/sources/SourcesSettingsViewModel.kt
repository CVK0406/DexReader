package com.example.dexreader.settings.sources

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import com.example.dexreader.core.ui.BaseViewModel
import com.example.dexreader.explore.data.MangaSourcesRepository
import javax.inject.Inject

@HiltViewModel
class SourcesSettingsViewModel @Inject constructor(
	sourcesRepository: MangaSourcesRepository,
) : BaseViewModel() {

	val enabledSourcesCount = sourcesRepository.observeEnabledSourcesCount()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, -1)

	val availableSourcesCount = sourcesRepository.observeAvailableSourcesCount()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, -1)
}
