package com.example.dexreader.settings

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.dexreader.R
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.prefs.DownloadFormat
import com.example.dexreader.core.ui.BasePreferenceFragment
import com.example.dexreader.core.util.ext.printStackTraceDebug
import com.example.dexreader.core.util.ext.resolveFile
import com.example.dexreader.core.util.ext.setDefaultValueCompat
import com.example.dexreader.core.util.ext.tryLaunch
import com.example.dexreader.core.util.ext.viewLifecycleScope
import com.example.dexreader.download.ui.worker.DownloadWorker
import com.example.dexreader.local.data.LocalStorageManager
import org.example.dexreader.parsers.util.names
import com.example.dexreader.settings.storage.MangaDirectorySelectDialog
import com.example.dexreader.settings.storage.directories.MangaDirectoriesActivity
import javax.inject.Inject

@AndroidEntryPoint
class DownloadsSettingsFragment :
	BasePreferenceFragment(R.string.downloads),
	SharedPreferences.OnSharedPreferenceChangeListener {

	@Inject
	lateinit var storageManager: LocalStorageManager

	@Inject
	lateinit var downloadsScheduler: DownloadWorker.Scheduler

	private val pickFileTreeLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
		if (it != null) onDirectoryPicked(it)
	}

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_downloads)
		findPreference<ListPreference>(AppSettings.KEY_DOWNLOADS_FORMAT)?.run {
			entryValues = DownloadFormat.entries.names()
			setDefaultValueCompat(DownloadFormat.AUTOMATIC.name)
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		findPreference<Preference>(AppSettings.KEY_LOCAL_STORAGE)?.bindStorageName()
		findPreference<Preference>(AppSettings.KEY_LOCAL_MANGA_DIRS)?.bindDirectoriesCount()
		findPreference<Preference>(AppSettings.KEY_PAGES_SAVE_DIR)?.bindPagesDirectory()
		settings.subscribe(this)
	}

	override fun onDestroyView() {
		settings.unsubscribe(this)
		super.onDestroyView()
	}

	override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
		when (key) {
			AppSettings.KEY_LOCAL_STORAGE -> {
				findPreference<Preference>(key)?.bindStorageName()
			}

			AppSettings.KEY_LOCAL_MANGA_DIRS -> {
				findPreference<Preference>(key)?.bindDirectoriesCount()
			}

			AppSettings.KEY_DOWNLOADS_WIFI -> {
				updateDownloadsConstraints()
			}

			AppSettings.KEY_PAGES_SAVE_DIR -> {
				findPreference<Preference>(AppSettings.KEY_PAGES_SAVE_DIR)?.bindPagesDirectory()
			}
		}
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		return when (preference.key) {
			AppSettings.KEY_LOCAL_STORAGE -> {
				MangaDirectorySelectDialog.show(childFragmentManager)
				true
			}

			AppSettings.KEY_LOCAL_MANGA_DIRS -> {
				startActivity(MangaDirectoriesActivity.newIntent(preference.context))
				true
			}

			AppSettings.KEY_PAGES_SAVE_DIR -> {
				if (!pickFileTreeLauncher.tryLaunch(settings.getPagesSaveDir(preference.context)?.uri)) {
					Snackbar.make(
						requireView(), R.string.operation_not_supported, Snackbar.LENGTH_SHORT,
					).show()
				}
				true
			}

			else -> super.onPreferenceTreeClick(preference)
		}
	}

	private fun onDirectoryPicked(uri: Uri) {
		storageManager.takePermissions(uri)
		val doc = DocumentFile.fromTreeUri(requireContext(), uri)?.takeIf {
			it.canWrite()
		}
		settings.setPagesSaveDir(doc?.uri)
	}

	private fun Preference.bindStorageName() {
		viewLifecycleScope.launch {
			val storage = storageManager.getDefaultWriteableDir()
			summary = if (storage != null) {
				storageManager.getDirectoryDisplayName(storage, isFullPath = true)
			} else {
				getString(R.string.not_available)
			}
		}
	}

	private fun Preference.bindDirectoriesCount() {
		viewLifecycleScope.launch {
			val dirs = storageManager.getReadableDirs().size
			summary = resources.getQuantityString(R.plurals.items, dirs, dirs)
		}
	}

	private fun Preference.bindPagesDirectory() {
		viewLifecycleScope.launch {
			val df = withContext(Dispatchers.IO) {
				settings.getPagesSaveDir(this@bindPagesDirectory.context)
			}
			summary = df?.getDisplayPath(this@bindPagesDirectory.context)
				?: this@bindPagesDirectory.context.getString(androidx.preference.R.string.not_set)
		}
	}

	private fun updateDownloadsConstraints() {
		val preference = findPreference<Preference>(AppSettings.KEY_DOWNLOADS_WIFI)
		viewLifecycleScope.launch {
			try {
				preference?.isEnabled = false
				withContext(Dispatchers.Default) {
					downloadsScheduler.updateConstraints()
				}
			} catch (e: Exception) {
				e.printStackTraceDebug()
			} finally {
				preference?.isEnabled = true
			}
		}
	}

	private fun DocumentFile.getDisplayPath(context: Context): String {
		return uri.resolveFile(context)?.path ?: uri.toString()
	}

}
