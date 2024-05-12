package com.example.dexreader.favourites.ui.categories.select.adapter

import androidx.core.view.isVisible
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.core.util.ext.setChecked
import com.example.dexreader.databinding.ItemCategoryCheckableBinding
import com.example.dexreader.favourites.ui.categories.select.model.MangaCategoryItem
import com.example.dexreader.list.ui.ListModelDiffCallback
import com.example.dexreader.list.ui.model.ListModel

fun mangaCategoryAD(
	clickListener: OnListItemClickListener<MangaCategoryItem>,
) = adapterDelegateViewBinding<MangaCategoryItem, ListModel, ItemCategoryCheckableBinding>(
	{ inflater, parent -> ItemCategoryCheckableBinding.inflate(inflater, parent, false) },
) {

	itemView.setOnClickListener {
		clickListener.onItemClick(item, itemView)
	}

	bind { payloads ->
		binding.checkableImageView.setChecked(item.isChecked, ListModelDiffCallback.PAYLOAD_CHECKED_CHANGED in payloads)
		binding.textViewTitle.text = item.category.title
		binding.imageViewTracker.isVisible = item.category.isTrackingEnabled && item.isTrackerEnabled
		binding.imageViewVisible.isVisible = item.category.isVisibleInLibrary
	}
}
