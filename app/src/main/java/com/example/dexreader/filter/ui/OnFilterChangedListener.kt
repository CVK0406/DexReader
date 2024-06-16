package com.example.dexreader.filter.ui

import com.example.dexreader.list.ui.adapter.ListHeaderClickListener
import org.example.dexreader.parsers.model.ContentRating
import org.example.dexreader.parsers.model.MangaState
import org.example.dexreader.parsers.model.MangaTag
import org.example.dexreader.parsers.model.SortOrder
import java.util.Locale

interface OnFilterChangedListener : ListHeaderClickListener {

	fun setSortOrder(value: SortOrder)

	fun setLanguage(value: Locale?)

	fun setTag(value: MangaTag, addOrRemove: Boolean)

	fun setTagExcluded(value: MangaTag, addOrRemove: Boolean)

	fun setState(value: MangaState, addOrRemove: Boolean)

	fun setContentRating(value: ContentRating, addOrRemove: Boolean)
}
