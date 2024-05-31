package com.example.dexreader.tracker.ui.feed.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.example.dexreader.R
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.core.util.ext.enqueueWith
import com.example.dexreader.core.util.ext.newImageRequest
import com.example.dexreader.core.util.ext.source
import com.example.dexreader.databinding.ItemFeedBinding
import com.example.dexreader.list.ui.model.ListModel
import org.example.dexreader.parsers.model.Manga
import com.example.dexreader.tracker.ui.feed.model.FeedItem

fun feedItemAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	clickListener: OnListItemClickListener<Manga>,
) = adapterDelegateViewBinding<FeedItem, ListModel, ItemFeedBinding>(
	{ inflater, parent -> ItemFeedBinding.inflate(inflater, parent, false) },
) {
	itemView.setOnClickListener {
		clickListener.onItemClick(item.manga, it)
	}

	bind {
		val alpha = if (item.isNew) 1f else 0.5f
		binding.textViewTitle.alpha = alpha
		binding.textViewSummary.alpha = alpha
		binding.imageViewCover.newImageRequest(lifecycleOwner, item.imageUrl)?.run {
			placeholder(R.drawable.ic_placeholder)
			fallback(R.drawable.ic_placeholder)
			error(R.drawable.ic_error_placeholder)
			allowRgb565(true)
			source(item.manga.source)
			enqueueWith(coil)
		}
		binding.textViewTitle.text = item.title
		binding.textViewSummary.text = context.resources.getQuantityString(
			R.plurals.new_chapters,
			item.count,
			item.count,
		)
	}
}
