package com.example.dexreader.search.ui.suggestion.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.example.dexreader.core.ui.BaseListAdapter
import com.example.dexreader.search.ui.suggestion.SearchSuggestionListener
import com.example.dexreader.search.ui.suggestion.model.SearchSuggestionItem

const val SEARCH_SUGGESTION_ITEM_TYPE_QUERY = 0

class SearchSuggestionAdapter(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	listener: SearchSuggestionListener,
) : BaseListAdapter<SearchSuggestionItem>() {

	init {
		delegatesManager
			.addDelegate(SEARCH_SUGGESTION_ITEM_TYPE_QUERY, searchSuggestionQueryAD(listener))
			.addDelegate(searchSuggestionSourceAD(coil, lifecycleOwner, listener))
			.addDelegate(searchSuggestionTagsAD(listener))
			.addDelegate(searchSuggestionMangaListAD(coil, lifecycleOwner, listener))
			.addDelegate(searchSuggestionQueryHintAD(listener))
	}
}
