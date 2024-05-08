package com.example.dexreader.list.ui.adapter

import android.view.View
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.example.dexreader.R
import com.example.dexreader.core.ui.image.CoverSizeResolver
import com.example.dexreader.core.ui.image.TrimTransformation
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.core.util.ext.enqueueWith
import com.example.dexreader.core.util.ext.newImageRequest
import com.example.dexreader.core.util.ext.setOnContextClickListenerCompat
import com.example.dexreader.core.util.ext.source
import com.example.dexreader.databinding.ItemMangaGridBinding
import com.example.dexreader.list.ui.ListModelDiffCallback
import com.example.dexreader.list.ui.model.ListModel
import com.example.dexreader.list.ui.model.MangaGridModel
import com.example.dexreader.list.ui.size.ItemSizeResolver
import com.google.android.material.badge.BadgeDrawable
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.example.dexreader.parsers.model.Manga

fun mangaGridItemAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	sizeResolver: ItemSizeResolver,
	clickListener: OnListItemClickListener<Manga>,
) = adapterDelegateViewBinding<MangaGridModel, ListModel, ItemMangaGridBinding>(
	{ inflater, parent -> ItemMangaGridBinding.inflate(inflater, parent, false) },
) {
	var badge: BadgeDrawable? = null

	val eventListener = object : View.OnClickListener, View.OnLongClickListener {
		override fun onClick(v: View) = clickListener.onItemClick(item.manga, v)
		override fun onLongClick(v: View): Boolean = clickListener.onItemLongClick(item.manga, v)
	}
	itemView.setOnClickListener(eventListener)
	itemView.setOnLongClickListener(eventListener)
	itemView.setOnContextClickListenerCompat(eventListener)
	sizeResolver.attachToView(lifecycleOwner, itemView, binding.textViewTitle, binding.progressView)

	bind { payloads ->
		binding.textViewTitle.text = item.title
		binding.progressView.setPercent(item.progress, ListModelDiffCallback.PAYLOAD_PROGRESS_CHANGED in payloads)
		binding.imageViewCover.newImageRequest(lifecycleOwner, item.coverUrl)?.run {
			size(CoverSizeResolver(binding.imageViewCover))
			placeholder(R.drawable.ic_placeholder)
			fallback(R.drawable.ic_placeholder)
			error(R.drawable.ic_error_placeholder)
			transformations(TrimTransformation())
			allowRgb565(true)
			tag(item.manga)
			source(item.source)
			enqueueWith(coil)
		}
		badge = itemView.bindBadge(badge, item.counter)
	}
}
