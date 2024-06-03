package com.example.dexreader.core

import android.app.Application
import android.os.Build
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.room.InvalidationTracker
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.dexreader.core.db.MangaDatabase
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.util.WorkServiceStopHelper
import com.example.dexreader.core.util.ext.processLifecycleScope
import com.example.dexreader.settings.work.WorkScheduleManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.conscrypt.Conscrypt
import java.security.Security
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidApp
open class BaseApp : Application(), Configuration.Provider {

	@Inject
	lateinit var databaseObservers: Set<@JvmSuppressWildcards InvalidationTracker.Observer>

	@Inject
	lateinit var activityLifecycleCallbacks: Set<@JvmSuppressWildcards ActivityLifecycleCallbacks>

	@Inject
	lateinit var database: Provider<MangaDatabase>

	@Inject
	lateinit var settings: AppSettings

	@Inject
	lateinit var workerFactory: HiltWorkerFactory

	@Inject
	lateinit var workScheduleManager: WorkScheduleManager

	@Inject
	lateinit var workManagerProvider: Provider<WorkManager>

	override val workManagerConfiguration: Configuration
		get() = Configuration.Builder()
			.setWorkerFactory(workerFactory)
			.build()

	override fun onCreate() {
		super.onCreate()
		AppCompatDelegate.setDefaultNightMode(settings.theme)
		AppCompatDelegate.setApplicationLocales(settings.appLocales)
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
			Security.insertProviderAt(Conscrypt.newProvider(), 1)
		}
		setupActivityLifecycleCallbacks()
		processLifecycleScope.launch(Dispatchers.Default) {
			setupDatabaseObservers()
		}
		workScheduleManager.init()
		WorkServiceStopHelper(workManagerProvider).setup()
	}

	@WorkerThread
	private suspend fun setupDatabaseObservers() {
		withContext(Dispatchers.IO) {
			val tracker = database.get().invalidationTracker
			databaseObservers.forEach {
				tracker.addObserver(it)
			}
		}
	}

	private fun setupActivityLifecycleCallbacks() {
		activityLifecycleCallbacks.forEach {
			registerActivityLifecycleCallbacks(it)
		}
	}
}
