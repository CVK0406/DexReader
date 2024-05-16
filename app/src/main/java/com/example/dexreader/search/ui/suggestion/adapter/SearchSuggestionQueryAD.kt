package com.example.dexreader.search.ui.suggestion.adapter

import android.view.View
import com.example.dexreader.R
import com.example.dexreader.databinding.ItemSearchSuggestionQueryBinding
import com.example.dexreader.search.ui.suggestion.SearchSuggestionListener
import com.example.dexreader.search.ui.suggestion.model.SearchSuggestionItem
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding

fun searchSuggestionQueryAD(
	listener: SearchSuggestionListener,
) = adapterDelegateViewBinding<SearchSuggestionItem.RecentQuery, SearchSuggestionItem, ItemSearchSuggestionQueryBinding>(
	{ inflater, parent -> ItemSearchSuggestionQueryBinding.inflate(inflater, parent, false) }
) {

	val viewClickListener = View.OnClickListener { v ->
		listener.onQueryClick(item.query, v.id != R.id.button_complete)
	}

	binding.root.setOnClickListener(viewClickListener)
	binding.buttonComplete.setOnClickListener(viewClickListener)

	bind {
		binding.textViewTitle.text = item.query
	}
}
