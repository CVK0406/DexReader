package com.example.dexreader.search.ui.suggestion.adapter

import android.view.View
import com.example.dexreader.databinding.ItemSearchSuggestionQueryHintBinding
import com.example.dexreader.search.ui.suggestion.SearchSuggestionListener
import com.example.dexreader.search.ui.suggestion.model.SearchSuggestionItem
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding

fun searchSuggestionQueryHintAD(
	listener: SearchSuggestionListener,
) = adapterDelegateViewBinding<SearchSuggestionItem.Hint, SearchSuggestionItem, ItemSearchSuggestionQueryHintBinding>(
	{ inflater, parent -> ItemSearchSuggestionQueryHintBinding.inflate(inflater, parent, false) },
) {

	val viewClickListener = View.OnClickListener { _ ->
		listener.onQueryClick(item.query, true)
	}

	binding.root.setOnClickListener(viewClickListener)

	bind {
		binding.root.text = item.query
	}
}
