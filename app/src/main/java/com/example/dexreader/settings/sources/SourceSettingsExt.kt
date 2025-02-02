package com.example.dexreader.settings.sources

import android.view.inputmethod.EditorInfo
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.example.dexreader.R
import com.example.dexreader.core.parser.RemoteMangaRepository
import org.example.dexreader.parsers.config.ConfigKey
import org.example.dexreader.parsers.network.UserAgents
import com.example.dexreader.settings.utils.AutoCompleteTextViewPreference
import com.example.dexreader.settings.utils.EditTextBindListener
import com.example.dexreader.settings.utils.EditTextDefaultSummaryProvider
import com.example.dexreader.settings.utils.validation.DomainValidator
import com.example.dexreader.settings.utils.validation.HeaderValidator

fun PreferenceFragmentCompat.addPreferencesFromRepository(repository: RemoteMangaRepository) {
	val configKeys = repository.getConfigKeys()
	val screen = preferenceScreen
	for (key in configKeys) {
		val preference: Preference = when (key) {
			is ConfigKey.Domain -> {
				val presetValues = key.presetValues
				if (presetValues.size <= 1) {
					EditTextPreference(requireContext())
				} else {
					AutoCompleteTextViewPreference(requireContext()).apply {
						entries = presetValues.toStringArray()
					}
				}.apply {
					summaryProvider = EditTextDefaultSummaryProvider(key.defaultValue)
					setOnBindEditTextListener(
						EditTextBindListener(
							inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_URI,
							hint = key.defaultValue,
							validator = DomainValidator(),
						),
					)
					setTitle(R.string.domain)
					setDialogTitle(R.string.domain)
				}
			}

			is ConfigKey.UserAgent -> {
				AutoCompleteTextViewPreference(requireContext()).apply {
					entries = arrayOf(
						UserAgents.FIREFOX_MOBILE,
						UserAgents.CHROME_MOBILE,
						UserAgents.FIREFOX_DESKTOP,
						UserAgents.CHROME_DESKTOP,
					)
					summaryProvider = EditTextDefaultSummaryProvider(key.defaultValue)
					setOnBindEditTextListener(
						EditTextBindListener(
							inputType = EditorInfo.TYPE_CLASS_TEXT,
							hint = key.defaultValue,
							validator = HeaderValidator(),
						),
					)
					setTitle(R.string.user_agent)
					setDialogTitle(R.string.user_agent)
				}
			}

			is ConfigKey.ShowSuspiciousContent -> {
				SwitchPreferenceCompat(requireContext()).apply {
					setDefaultValue(key.defaultValue)
					setTitle(R.string.show_suspicious_content)
				}
			}

			is ConfigKey.SplitByTranslations -> {
				SwitchPreferenceCompat(requireContext()).apply {
					setDefaultValue(key.defaultValue)
					setTitle(R.string.split_by_translations)
					setSummary(R.string.split_by_translations_summary)
				}
			}
		}
		preference.isIconSpaceReserved = false
		preference.key = key.key
		preference.order = 10
		screen.addPreference(preference)
	}
}

private fun Array<out String>.toStringArray(): Array<String> {
	return Array(size) { i -> this[i] as? String ?: "" }
}
