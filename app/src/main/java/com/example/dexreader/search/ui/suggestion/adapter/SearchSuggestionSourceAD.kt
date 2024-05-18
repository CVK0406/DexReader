package com.example.dexreader.search.ui.suggestion.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.example.dexreader.R
import com.example.dexreader.core.model.getSummary
import com.example.dexreader.core.model.getTitle
import com.example.dexreader.core.parser.favicon.faviconUri
import com.example.dexreader.core.ui.image.FaviconDrawable
import com.example.dexreader.core.util.ext.enqueueWith
import com.example.dexreader.core.util.ext.newImageRequest
import com.example.dexreader.core.util.ext.source
import com.example.dexreader.databinding.ItemSearchSuggestionSourceBinding
import com.example.dexreader.search.ui.suggestion.SearchSuggestionListener
import com.example.dexreader.search.ui.suggestion.model.SearchSuggestionItem
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding

fun searchSuggestionSourceAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	listener: SearchSuggestionListener,
) = adapterDelegateViewBinding<SearchSuggestionItem.Source, SearchSuggestionItem, ItemSearchSuggestionSourceBinding>(
	{ inflater, parent -> ItemSearchSuggestionSourceBinding.inflate(inflater, parent, false) },
) {

	binding.switchLocal.setOnCheckedChangeListener { _, isChecked ->
		listener.onSourceToggle(item.source, isChecked)
	}
	binding.root.setOnClickListener {
		listener.onSourceClick(item.source)
	}

	bind {
		binding.textViewTitle.text = item.source.getTitle(context)
		binding.textViewSubtitle.text = item.source.getSummary(context)
		binding.switchLocal.isChecked = item.isEnabled
		val fallbackIcon = FaviconDrawable(context, R.style.FaviconDrawable_Small, item.source.name)
		binding.imageViewCover.newImageRequest(lifecycleOwner, item.source.faviconUri())?.run {
			fallback(fallbackIcon)
			placeholder(fallbackIcon)
			error(fallbackIcon)
			source(item.source)
			enqueueWith(coil)
		}
	}
}
