package com.example.dexreader.settings

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import dagger.hilt.android.AndroidEntryPoint
import com.example.dexreader.R
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.ui.BasePreferenceFragment
import com.example.dexreader.core.util.ext.observe

@AndroidEntryPoint
class RootSettingsFragment : BasePreferenceFragment(0) {

	private val viewModel: RootSettingsViewModel by viewModels()

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_root)
		bindPreferenceSummary("appearance", R.string.theme, R.string.list_mode, R.string.language)
		bindPreferenceSummary("reader", R.string.read_mode, R.string.switch_pages)
		bindPreferenceSummary("userdata", R.string.backup_restore)
		bindPreferenceSummary("downloads", R.string.manga_save_location)
		bindPreferenceSummary("tracker", R.string.track_sources, R.string.notifications_settings)
		bindPreferenceSummary("services", R.string.suggestions)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		findPreference<Preference>(AppSettings.KEY_REMOTE_SOURCES)?.let { pref ->
			val total = viewModel.totalSourcesCount
			viewModel.enabledSourcesCount.observe(viewLifecycleOwner) {
				pref.summary = if (it >= 0) {
					getString(R.string.enabled_d_of_d, it, total)
				} else {
					resources.getQuantityString(R.plurals.items, total, total)
				}
			}
		}
	}

	private fun bindPreferenceSummary(key: String, @StringRes vararg items: Int) {
		findPreference<Preference>(key)?.summary = items.joinToString { getString(it) }
	}
}
