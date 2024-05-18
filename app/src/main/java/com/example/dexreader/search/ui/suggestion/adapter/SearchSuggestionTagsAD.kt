package com.example.dexreader.search.ui.suggestion.adapter

import com.example.dexreader.core.ui.widgets.ChipsView
import com.example.dexreader.databinding.ItemSearchSuggestionTagsBinding
import com.example.dexreader.search.ui.suggestion.SearchSuggestionListener
import com.example.dexreader.search.ui.suggestion.model.SearchSuggestionItem
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.example.dexreader.parsers.model.MangaTag

fun searchSuggestionTagsAD(
	listener: SearchSuggestionListener,
) = adapterDelegateViewBinding<SearchSuggestionItem.Tags, SearchSuggestionItem, ItemSearchSuggestionTagsBinding>(
	{ layoutInflater, parent -> ItemSearchSuggestionTagsBinding.inflate(layoutInflater, parent, false) },
) {

	binding.chipsGenres.onChipClickListener = ChipsView.OnChipClickListener { _, data ->
		listener.onTagClick(data as? MangaTag ?: return@OnChipClickListener)
	}

	bind {
		binding.chipsGenres.setChips(item.tags)
	}
}
