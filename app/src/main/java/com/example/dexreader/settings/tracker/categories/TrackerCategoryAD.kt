package com.example.dexreader.settings.tracker.categories

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.example.dexreader.core.model.FavouriteCategory
import com.example.dexreader.core.ui.list.AdapterDelegateClickListenerAdapter
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.databinding.ItemCategoryCheckableMultipleBinding

fun trackerCategoryAD(
	listener: OnListItemClickListener<FavouriteCategory>,
) = adapterDelegateViewBinding<FavouriteCategory, FavouriteCategory, ItemCategoryCheckableMultipleBinding>(
	{ layoutInflater, parent -> ItemCategoryCheckableMultipleBinding.inflate(layoutInflater, parent, false) },
) {
	val eventListener = AdapterDelegateClickListenerAdapter(this, listener)
	itemView.setOnClickListener(eventListener)

	bind {
		binding.root.text = item.title
		binding.root.isChecked = item.isTrackingEnabled
	}
}
