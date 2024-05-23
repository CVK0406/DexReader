package com.example.dexreader.core.util.progress

import android.content.Context
import com.example.dexreader.R
import com.google.android.material.slider.LabelFormatter

class IntPercentLabelFormatter(context: Context) : LabelFormatter {

	private val pattern = context.getString(R.string.percent_string_pattern)

	override fun getFormattedValue(value: Float) = pattern.format(value.toInt().toString())
}
