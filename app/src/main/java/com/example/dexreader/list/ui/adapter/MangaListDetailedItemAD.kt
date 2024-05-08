package com.example.dexreader.list.ui.adapter

import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.example.dexreader.R
import com.example.dexreader.core.ui.image.CoverSizeResolver
import com.example.dexreader.core.ui.image.TrimTransformation
import com.example.dexreader.core.ui.widgets.ChipsView
import com.example.dexreader.core.util.ext.enqueueWith
import com.example.dexreader.core.util.ext.newImageRequest
import com.example.dexreader.core.util.ext.setOnContextClickListenerCompat
import com.example.dexreader.core.util.ext.source
import com.example.dexreader.core.util.ext.textAndVisible
import com.example.dexreader.databinding.ItemMangaListDetailsBinding
import com.example.dexreader.list.ui.ListModelDiffCallback
import com.example.dexreader.list.ui.model.ListModel
import com.example.dexreader.list.ui.model.MangaListDetailedModel
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.chip.Chip
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.example.dexreader.parsers.model.MangaTag

fun mangaListDetailedItemAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	clickListener: MangaDetailsClickListener,
) = adapterDelegateViewBinding<MangaListDetailedModel, ListModel, ItemMangaListDetailsBinding>(
	{ inflater, parent -> ItemMangaListDetailsBinding.inflate(inflater, parent, false) },
) {
	var badge: BadgeDrawable? = null

	val listenerAdapter = object : View.OnClickListener, View.OnLongClickListener, ChipsView.OnChipClickListener {
		override fun onClick(v: View) = when (v.id) {
			R.id.button_read -> clickListener.onReadClick(item.manga, v)
			else -> clickListener.onItemClick(item.manga, v)
		}

		override fun onLongClick(v: View): Boolean = clickListener.onItemLongClick(item.manga, v)

		override fun onChipClick(chip: Chip, data: Any?) {
			val tag = data as? MangaTag ?: return
			clickListener.onTagClick(item.manga, tag, chip)
		}
	}
	itemView.setOnClickListener(listenerAdapter)
	itemView.setOnLongClickListener(listenerAdapter)
	itemView.setOnContextClickListenerCompat(listenerAdapter)
	binding.buttonRead.setOnClickListener(listenerAdapter)
	binding.chipsTags.onChipClickListener = listenerAdapter

	bind { payloads ->
		binding.textViewTitle.text = item.title
		binding.textViewSubtitle.textAndVisible = item.subtitle
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
		if (payloads.isEmpty()) {
			binding.scrollViewTags.scrollTo(0, 0)
		}
		binding.chipsTags.setChips(item.tags)
		binding.ratingBar.isVisible = item.manga.hasRating
		binding.ratingBar.rating = binding.ratingBar.numStars * item.manga.rating
		badge = itemView.bindBadge(badge, item.counter)
	}
}
