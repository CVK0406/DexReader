package com.example.dexreader.settings

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import dagger.hilt.android.AndroidEntryPoint
import com.example.dexreader.R
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.ui.BasePreferenceFragment
import com.example.dexreader.settings.utils.SplitSwitchPreference
import com.example.dexreader.stats.ui.StatsActivity

@AndroidEntryPoint
class ServicesSettingsFragment : BasePreferenceFragment(R.string.services),
	SharedPreferences.OnSharedPreferenceChangeListener {

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_services)
		findPreference<SplitSwitchPreference>(AppSettings.KEY_STATS_ENABLED)?.let {
			it.onContainerClickListener = Preference.OnPreferenceClickListener {
				it.context.startActivity(Intent(it.context, StatsActivity::class.java))
				true
			}
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		bindSuggestionsSummary()
		bindStatsSummary()
		settings.subscribe(this)
	}

	override fun onDestroyView() {
		settings.unsubscribe(this)
		super.onDestroyView()
	}

	override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
		when (key) {
			AppSettings.KEY_SUGGESTIONS -> bindSuggestionsSummary()
			AppSettings.KEY_STATS_ENABLED -> bindStatsSummary()
		}
	}

	private fun bindSuggestionsSummary() {
		findPreference<Preference>(AppSettings.KEY_SUGGESTIONS)?.setSummary(
			if (settings.isSuggestionsEnabled) R.string.enabled else R.string.disabled,
		)
	}

	private fun bindStatsSummary() {
		findPreference<Preference>(AppSettings.KEY_STATS_ENABLED)?.setSummary(
			if (settings.isStatsEnabled) R.string.enabled else R.string.disabled,
		)
	}
}
