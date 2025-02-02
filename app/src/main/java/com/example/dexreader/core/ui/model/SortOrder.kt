package com.example.dexreader.core.ui.model

import androidx.annotation.StringRes
import com.example.dexreader.R
import org.example.dexreader.parsers.model.SortOrder

@get:StringRes
val SortOrder.titleRes: Int
	get() = when (this) {
		SortOrder.UPDATED -> R.string.updated
		SortOrder.POPULARITY -> R.string.popular
		SortOrder.RATING -> R.string.by_rating
		SortOrder.NEWEST -> R.string.newest
		SortOrder.ALPHABETICAL -> R.string.by_name
		SortOrder.ALPHABETICAL_DESC -> R.string.by_name_reverse
	}
