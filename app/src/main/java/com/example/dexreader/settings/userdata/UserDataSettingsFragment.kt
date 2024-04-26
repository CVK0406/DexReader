package com.example.dexreader.settings.userdata

import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.postDelayed
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import androidx.preference.forEach
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.StateFlow
import com.example.dexreader.R
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.ui.BasePreferenceFragment
import com.example.dexreader.core.ui.util.ActivityRecreationHandle
import com.example.dexreader.core.ui.util.ReversibleActionObserver
import com.example.dexreader.core.util.FileSize
import com.example.dexreader.core.util.ext.observe
import com.example.dexreader.core.util.ext.observeEvent
import com.example.dexreader.core.util.ext.tryLaunch
import com.example.dexreader.local.data.CacheDir
import com.example.dexreader.settings.backup.BackupDialogFragment
import com.example.dexreader.settings.backup.RestoreDialogFragment
import javax.inject.Inject

@AndroidEntryPoint
class UserDataSettingsFragment : BasePreferenceFragment(R.string.data_and_privacy),
	SharedPreferences.OnSharedPreferenceChangeListener,
	ActivityResultCallback<Uri?> {

	@Inject
	lateinit var activityRecreationHandle: ActivityRecreationHandle

	private val viewModel: UserDataSettingsViewModel by viewModels()

	private val backupSelectCall = registerForActivityResult(
		ActivityResultContracts.OpenDocument(),
		this,
	)

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_user_data)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		findPreference<Preference>(AppSettings.KEY_PAGES_CACHE_CLEAR)?.bindBytesSizeSummary(checkNotNull(viewModel.cacheSizes[CacheDir.PAGES]))
		findPreference<Preference>(AppSettings.KEY_THUMBS_CACHE_CLEAR)?.bindBytesSizeSummary(checkNotNull(viewModel.cacheSizes[CacheDir.THUMBS]))
		findPreference<Preference>(AppSettings.KEY_HTTP_CACHE_CLEAR)?.bindBytesSizeSummary(viewModel.httpCacheSize)
		bindPeriodicalBackupSummary()
		findPreference<Preference>(AppSettings.KEY_SEARCH_HISTORY_CLEAR)?.let { pref ->
			viewModel.searchHistoryCount.observe(viewLifecycleOwner) {
				pref.summary = if (it < 0) {
					view.context.getString(R.string.loading_)
				} else {
					pref.context.resources.getQuantityString(R.plurals.items, it, it)
				}
			}
		}
		findPreference<Preference>(AppSettings.KEY_UPDATES_FEED_CLEAR)?.let { pref ->
			viewModel.feedItemsCount.observe(viewLifecycleOwner) {
				pref.summary = if (it < 0) {
					view.context.getString(R.string.loading_)
				} else {
					pref.context.resources.getQuantityString(R.plurals.items, it, it)
				}
			}
		}
		viewModel.loadingKeys.observe(viewLifecycleOwner) { keys ->
			preferenceScreen.forEach { pref ->
				pref.isEnabled = pref.key !in keys
			}
		}
		viewModel.onActionDone.observeEvent(viewLifecycleOwner, ReversibleActionObserver(listView))
		viewModel.onChaptersCleanedUp.observeEvent(viewLifecycleOwner, ::onChaptersCleanedUp)
		settings.subscribe(this)
	}

	override fun onDestroyView() {
		settings.unsubscribe(this)
		super.onDestroyView()
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		return when (preference.key) {
			AppSettings.KEY_PAGES_CACHE_CLEAR -> {
				viewModel.clearCache(preference.key, CacheDir.PAGES)
				true
			}

			AppSettings.KEY_THUMBS_CACHE_CLEAR -> {
				viewModel.clearCache(preference.key, CacheDir.THUMBS)
				true
			}

			AppSettings.KEY_COOKIES_CLEAR -> {
				clearCookies()
				true
			}

			AppSettings.KEY_SEARCH_HISTORY_CLEAR -> {
				clearSearchHistory()
				true
			}

			AppSettings.KEY_HTTP_CACHE_CLEAR -> {
				viewModel.clearHttpCache()
				true
			}

			AppSettings.KEY_CHAPTERS_CLEAR -> {
				cleanupChapters()
				true
			}

			AppSettings.KEY_UPDATES_FEED_CLEAR -> {
				viewModel.clearUpdatesFeed()
				true
			}

			AppSettings.KEY_BACKUP -> {
				BackupDialogFragment.show(childFragmentManager)
				true
			}

			AppSettings.KEY_RESTORE -> {
				if (!backupSelectCall.tryLaunch(arrayOf("*/*"))) {
					Snackbar.make(
						listView, R.string.operation_not_supported, Snackbar.LENGTH_SHORT,
					).show()
				}
				true
			}
			else -> super.onPreferenceTreeClick(preference)
		}
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		when (key) {
			AppSettings.KEY_THEME -> {
				AppCompatDelegate.setDefaultNightMode(settings.theme)
			}

			AppSettings.KEY_COLOR_THEME,
			AppSettings.KEY_THEME_AMOLED -> {
				postRestart()
			}

			AppSettings.KEY_APP_LOCALE -> {
				AppCompatDelegate.setApplicationLocales(settings.appLocales)
			}
		}
	}

	override fun onActivityResult(result: Uri?) {
		if (result != null) {
			RestoreDialogFragment.show(childFragmentManager, result)
		}
	}

	private fun onChaptersCleanedUp(result: Pair<Int, Long>) {
		val c = context ?: return
		val text = if (result.first == 0 && result.second == 0L) {
			c.getString(R.string.no_chapters_deleted)
		} else {
			c.getString(
				R.string.chapters_deleted_pattern,
				c.resources.getQuantityString(R.plurals.chapters, result.first, result.first),
				FileSize.BYTES.format(c, result.second),
			)
		}
		Snackbar.make(listView, text, Snackbar.LENGTH_SHORT).show()
	}


	private fun Preference.bindBytesSizeSummary(stateFlow: StateFlow<Long>) {
		stateFlow.observe(viewLifecycleOwner) { size ->
			summary = if (size < 0) {
				context.getString(R.string.computing_)
			} else {
				FileSize.BYTES.format(context, size)
			}
		}
	}

	private fun bindPeriodicalBackupSummary() {
		val preference = findPreference<Preference>(AppSettings.KEY_BACKUP_PERIODICAL_ENABLED) ?: return
		val entries = resources.getStringArray(R.array.backup_frequency)
		val entryValues = resources.getStringArray(R.array.values_backup_frequency)
		viewModel.periodicalBackupFrequency.observe(viewLifecycleOwner) { freq ->
			preference.summary = if (freq == 0L) {
				getString(R.string.disabled)
			} else {
				val index = entryValues.indexOf(freq.toString())
				entries.getOrNull(index)
			}
		}
	}

	private fun clearSearchHistory() {
		MaterialAlertDialogBuilder(context ?: return)
			.setTitle(R.string.clear_search_history)
			.setMessage(R.string.text_clear_search_history_prompt)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.clear) { _, _ ->
				viewModel.clearSearchHistory()
			}.show()
	}

	private fun clearCookies() {
		MaterialAlertDialogBuilder(context ?: return)
			.setTitle(R.string.clear_cookies)
			.setMessage(R.string.text_clear_cookies_prompt)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.clear) { _, _ ->
				viewModel.clearCookies()
			}.show()
	}

	private fun cleanupChapters() {
		MaterialAlertDialogBuilder(context ?: return)
			.setTitle(R.string.delete_read_chapters)
			.setMessage(R.string.delete_read_chapters_prompt)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.delete) { _, _ ->
				viewModel.cleanupChapters()
			}.show()
	}

	private fun postRestart() {
		view?.postDelayed(400) {
			activityRecreationHandle.recreateAll()
		}
	}

}
