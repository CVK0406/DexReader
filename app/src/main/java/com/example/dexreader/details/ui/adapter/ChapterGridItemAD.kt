package com.example.dexreader.details.ui.adapter

import android.graphics.Typeface
import androidx.core.view.isVisible
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.example.dexreader.core.model.formatNumber
import com.example.dexreader.core.ui.list.AdapterDelegateClickListenerAdapter
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.core.util.ext.getThemeColorStateList
import com.example.dexreader.databinding.ItemChapterGridBinding
import com.example.dexreader.details.ui.model.ChapterListItem
import com.example.dexreader.list.ui.model.ListModel

fun chapterGridItemAD(
	clickListener: OnListItemClickListener<ChapterListItem>,
) = adapterDelegateViewBinding<ChapterListItem, ListModel, ItemChapterGridBinding>(
	viewBinding = { inflater, parent -> ItemChapterGridBinding.inflate(inflater, parent, false) },
	on = { item, _, _ -> item is ChapterListItem && item.isGrid },
) {

	val eventListener = AdapterDelegateClickListenerAdapter(this, clickListener)
	itemView.setOnClickListener(eventListener)
	itemView.setOnLongClickListener(eventListener)

	bind { payloads ->
		if (payloads.isEmpty()) {
			binding.textViewTitle.text = item.chapter.formatNumber() ?: "?"
		}
		binding.imageViewNew.isVisible = item.isNew
		binding.imageViewCurrent.isVisible = item.isCurrent
		binding.imageViewBookmarked.isVisible = item.isBookmarked
		binding.imageViewDownloaded.isVisible = item.isDownloaded

		when {
			item.isCurrent -> {
				binding.textViewTitle.setTextColor(context.getThemeColorStateList(android.R.attr.textColorPrimary))
				binding.textViewTitle.typeface = Typeface.DEFAULT_BOLD
			}

			item.isUnread -> {
				binding.textViewTitle.setTextColor(context.getThemeColorStateList(android.R.attr.textColorPrimary))
				binding.textViewTitle.typeface = Typeface.DEFAULT
			}

			else -> {
				binding.textViewTitle.setTextColor(context.getThemeColorStateList(android.R.attr.textColorHint))
				binding.textViewTitle.typeface = Typeface.DEFAULT
			}
		}
	}
}

