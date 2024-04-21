package com.example.dexreader.bookmarks.ui.sheet

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.example.dexreader.R
import com.example.dexreader.bookmarks.domain.Bookmark
import com.example.dexreader.core.ui.image.CoverSizeResolver
import com.example.dexreader.core.ui.list.AdapterDelegateClickListenerAdapter
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.core.util.ext.decodeRegion
import com.example.dexreader.core.util.ext.enqueueWith
import com.example.dexreader.core.util.ext.newImageRequest
import com.example.dexreader.core.util.ext.source
import com.example.dexreader.databinding.ItemBookmarkLargeBinding
import com.example.dexreader.list.ui.model.ListModel

fun bookmarkLargeAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	clickListener: OnListItemClickListener<Bookmark>,
) = adapterDelegateViewBinding<Bookmark, ListModel, ItemBookmarkLargeBinding>(
	{ inflater, parent -> ItemBookmarkLargeBinding.inflate(inflater, parent, false) },
) {
	val listener = AdapterDelegateClickListenerAdapter(this, clickListener)

	binding.root.setOnClickListener(listener)
	binding.root.setOnLongClickListener(listener)

	bind {
		binding.imageViewThumb.newImageRequest(lifecycleOwner, item.imageLoadData)?.run {
			size(CoverSizeResolver(binding.imageViewThumb))
			placeholder(R.drawable.ic_placeholder)
			fallback(R.drawable.ic_placeholder)
			error(R.drawable.ic_error_placeholder)
			allowRgb565(true)
			tag(item)
			decodeRegion(item.scroll)
			source(item.manga.source)
			enqueueWith(coil)
		}
		binding.progressView.percent = item.percent
	}
}
