package com.example.dexreader.main.domain

import com.example.dexreader.core.os.NetworkState
import com.example.dexreader.history.data.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.example.dexreader.parsers.model.MangaSource
import javax.inject.Inject

class ReadingResumeEnabledUseCase @Inject constructor(
	private val networkState: NetworkState,
	private val historyRepository: HistoryRepository,
) {

	operator fun invoke(): Flow<Boolean> = combine(networkState, historyRepository.observeLast()) { isOnline, last ->
		last != null && (isOnline || last.source == MangaSource.LOCAL)
	}
}
