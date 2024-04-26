package com.example.dexreader.settings.reader

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import com.example.dexreader.core.ui.BaseViewModel
import com.example.dexreader.reader.data.TapGridSettings
import com.example.dexreader.reader.domain.TapGridArea
import com.example.dexreader.reader.ui.tapgrid.TapAction
import java.util.EnumMap
import javax.inject.Inject

@HiltViewModel
class ReaderTapGridConfigViewModel @Inject constructor(
	private val tapGridSettings: TapGridSettings,
) : BaseViewModel() {

	val content = tapGridSettings.observe()
		.onStart { emit(null) }
		.map { getData() }
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, emptyMap())

	fun reset() {
		launchJob(Dispatchers.Default) {
			tapGridSettings.reset()
		}
	}

	fun disableAll() {
		launchJob(Dispatchers.Default) {
			tapGridSettings.disableAll()
		}
	}

	fun setTapAction(area: TapGridArea, isLongTap: Boolean, action: TapAction?) {
		launchJob(Dispatchers.Default) {
			tapGridSettings.setTapAction(area, isLongTap, action)
		}
	}

	private fun getData(): Map<TapGridArea, TapActions> {
		val map = EnumMap<TapGridArea, TapActions>(TapGridArea::class.java)
		for (area in TapGridArea.entries) {
			map[area] = TapActions(
				tapAction = tapGridSettings.getTapAction(area, isLongTap = false),
				longTapAction = tapGridSettings.getTapAction(area, isLongTap = true),
			)
		}
		return map
	}

	data class TapActions(
		val tapAction: TapAction?,
		val longTapAction: TapAction?,
	)
}
