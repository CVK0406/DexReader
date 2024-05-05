package com.example.dexreader.settings.utils

import androidx.preference.EditTextPreference
import androidx.preference.Preference
import com.example.dexreader.R

class EditTextDefaultSummaryProvider(
	private val defaultValue: String
) : Preference.SummaryProvider<EditTextPreference> {

	override fun provideSummary(preference: EditTextPreference): CharSequence {
		val text = preference.text
		return if (text.isNullOrEmpty()) {
			preference.context.getString(R.string.default_s, defaultValue)
		} else {
			text
		}
	}
}
