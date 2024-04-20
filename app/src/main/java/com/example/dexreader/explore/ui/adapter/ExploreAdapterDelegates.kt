package com.example.dexreader.explore.ui.adapter

import android.view.View
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.example.dexreader.R
import com.example.dexreader.core.model.getSummary
import com.example.dexreader.core.model.getTitle
import com.example.dexreader.core.parser.favicon.faviconUri
import com.example.dexreader.core.ui.image.FaviconDrawable
import com.example.dexreader.core.ui.image.TrimTransformation
import com.example.dexreader.core.ui.list.AdapterDelegateClickListenerAdapter
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.core.util.ext.enqueueWith
import com.example.dexreader.core.util.ext.newImageRequest
import com.example.dexreader.core.util.ext.setOnContextClickListenerCompat
import com.example.dexreader.core.util.ext.setProgressIcon
import com.example.dexreader.core.util.ext.source
import com.example.dexreader.core.util.ext.textAndVisible
import com.example.dexreader.databinding.ItemExploreButtonsBinding
import com.example.dexreader.databinding.ItemExploreSourceGridBinding
import com.example.dexreader.databinding.ItemExploreSourceListBinding
import com.example.dexreader.explore.ui.model.ExploreButtons
import com.example.dexreader.explore.ui.model.MangaSourceItem
import com.example.dexreader.list.ui.model.ListModel
import org.example.dexreader.parsers.model.Manga

fun exploreButtonsAD(
	clickListener: View.OnClickListener,
) = adapterDelegateViewBinding<ExploreButtons, ListModel, ItemExploreButtonsBinding>(
	{ layoutInflater, parent -> ItemExploreButtonsBinding.inflate(layoutInflater, parent, false) },
) {

	binding.buttonBookmarks.setOnClickListener(clickListener)
	binding.buttonDownloads.setOnClickListener(clickListener)
	binding.buttonLocal.setOnClickListener(clickListener)
	binding.buttonRandom.setOnClickListener(clickListener)

	bind {
		if (item.isRandomLoading) {
			binding.buttonRandom.setProgressIcon()
		} else {
			binding.buttonRandom.setIconResource(R.drawable.ic_dice)
		}
		binding.buttonRandom.isClickable = !item.isRandomLoading
	}
}

fun exploreSourceListItemAD(
	coil: ImageLoader,
	listener: OnListItemClickListener<MangaSourceItem>,
	lifecycleOwner: LifecycleOwner,
) = adapterDelegateViewBinding<MangaSourceItem, ListModel, ItemExploreSourceListBinding>(
	{ layoutInflater, parent ->
		ItemExploreSourceListBinding.inflate(
			layoutInflater,
			parent,
			false,
		)
	},
	on = { item, _, _ -> item is MangaSourceItem && !item.isGrid },
) {

	val eventListener = AdapterDelegateClickListenerAdapter(this, listener)

	binding.root.setOnClickListener(eventListener)
	binding.root.setOnLongClickListener(eventListener)
	binding.root.setOnContextClickListenerCompat(eventListener)

	bind {
		binding.textViewTitle.text = item.source.getTitle(context)
		binding.textViewSubtitle.text = item.source.getSummary(context)
		val fallbackIcon = FaviconDrawable(context, R.style.FaviconDrawable_Small, item.source.name)
		binding.imageViewIcon.newImageRequest(lifecycleOwner, item.source.faviconUri())?.run {
			fallback(fallbackIcon)
			placeholder(fallbackIcon)
			error(fallbackIcon)
			source(item.source)
			enqueueWith(coil)
		}
	}
}

fun exploreSourceGridItemAD(
	coil: ImageLoader,
	listener: OnListItemClickListener<MangaSourceItem>,
	lifecycleOwner: LifecycleOwner,
) = adapterDelegateViewBinding<MangaSourceItem, ListModel, ItemExploreSourceGridBinding>(
	{ layoutInflater, parent ->
		ItemExploreSourceGridBinding.inflate(
			layoutInflater,
			parent,
			false,
		)
	},
	on = { item, _, _ -> item is MangaSourceItem && item.isGrid },
) {

	val eventListener = AdapterDelegateClickListenerAdapter(this, listener)

	binding.root.setOnClickListener(eventListener)
	binding.root.setOnLongClickListener(eventListener)
	binding.root.setOnContextClickListenerCompat(eventListener)

	bind {
		binding.textViewTitle.text = item.source.getTitle(context)
		val fallbackIcon = FaviconDrawable(context, R.style.FaviconDrawable_Large, item.source.name)
		binding.imageViewIcon.newImageRequest(lifecycleOwner, item.source.faviconUri())?.run {
			fallback(fallbackIcon)
			placeholder(fallbackIcon)
			error(fallbackIcon)
			source(item.source)
			enqueueWith(coil)
		}
	}
}
