package com.example.dexreader.core.util

import android.annotation.SuppressLint
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.impl.foreground.SystemForegroundService
import com.example.dexreader.core.util.ext.processLifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Provider

class WorkServiceStopHelper(
	private val workManagerProvider: Provider<WorkManager>,
) {

	fun setup() {
		processLifecycleScope.launch(Dispatchers.Default) {
			workManagerProvider.get()
				.getWorkInfosFlow(WorkQuery.fromStates(WorkInfo.State.RUNNING))
				.map { it.isEmpty() }
				.distinctUntilChanged()
				.collectLatest {
					if (it) {
						delay(1_000)
						stopWorkerService()
					}
				}
		}
	}

	@SuppressLint("RestrictedApi")
	private fun stopWorkerService() {
		SystemForegroundService.getInstance()?.stop()
	}
}

