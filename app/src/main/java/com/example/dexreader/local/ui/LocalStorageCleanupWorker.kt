package com.example.dexreader.local.ui

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.await
import com.example.dexreader.core.parser.MangaDataRepository
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.local.data.LocalMangaRepository
import com.example.dexreader.local.domain.DeleteReadChaptersUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class LocalStorageCleanupWorker @AssistedInject constructor(
	@Assisted appContext: Context,
	@Assisted params: WorkerParameters,
	private val settings: AppSettings,
	private val localMangaRepository: LocalMangaRepository,
	private val dataRepository: MangaDataRepository,
	private val deleteReadChaptersUseCase: DeleteReadChaptersUseCase,
) : CoroutineWorker(appContext, params) {

	override suspend fun doWork(): Result {
		if (settings.isAutoLocalChaptersCleanupEnabled) {
			deleteReadChaptersUseCase.invoke()
		}
		return if (localMangaRepository.cleanup()) {
			dataRepository.cleanupLocalManga()
			Result.success()
		} else {
			Result.retry()
		}
	}

	companion object {

		private const val TAG = "cleanup"

		suspend fun enqueue(context: Context) {
			val constraints = Constraints.Builder()
				.setRequiresBatteryNotLow(true)
				.build()
			val request = OneTimeWorkRequestBuilder<ImportWorker>()
				.setConstraints(constraints)
				.addTag(TAG)
				.setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.MINUTES)
				.build()
			WorkManager.getInstance(context).enqueueUniqueWork(TAG, ExistingWorkPolicy.KEEP, request).await()
		}
	}
}
