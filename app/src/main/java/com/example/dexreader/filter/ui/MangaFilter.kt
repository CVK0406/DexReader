package com.example.dexreader.filter.ui

import com.example.dexreader.filter.ui.model.FilterHeaderModel
import com.example.dexreader.filter.ui.model.FilterProperty
import com.example.dexreader.list.ui.model.ListModel
import kotlinx.coroutines.flow.StateFlow
import org.example.dexreader.parsers.model.ContentRating
import org.example.dexreader.parsers.model.MangaState
import org.example.dexreader.parsers.model.MangaTag
import org.example.dexreader.parsers.model.SortOrder
import java.util.Locale

interface MangaFilter : OnFilterChangedListener {

	val allTags: StateFlow<List<ListModel>>

	val filterTags: StateFlow<FilterProperty<MangaTag>>

	val filterTagsExcluded: StateFlow<FilterProperty<MangaTag>>

	val filterSortOrder: StateFlow<FilterProperty<SortOrder>>

	val filterState: StateFlow<FilterProperty<MangaState>>

	val filterContentRating: StateFlow<FilterProperty<ContentRating>>

	val filterLocale: StateFlow<FilterProperty<Locale?>>

	val header: StateFlow<FilterHeaderModel>

	fun applyFilter(tags: Set<MangaTag>)
}
