package com.example.dexreader.explore.data

import androidx.annotation.StringRes
import com.example.dexreader.R

enum class SourcesSortOrder(
	@StringRes val titleResId: Int,
) {
	ALPHABETIC(R.string.by_name),
	POPULARITY(R.string.popular),
	MANUAL(R.string.manual),
}
