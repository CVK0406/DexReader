package com.example.dexreader.favourites.ui.categories.select.model

import com.example.dexreader.core.model.FavouriteCategory
import com.example.dexreader.list.ui.ListModelDiffCallback
import com.example.dexreader.list.ui.model.ListModel

data class MangaCategoryItem(
	val category: FavouriteCategory,
	val isChecked: Boolean,
	val isTrackerEnabled: Boolean,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is MangaCategoryItem && other.category.id == category.id
	}

	override fun getChangePayload(previousState: ListModel): Any? {
		return if (previousState is MangaCategoryItem && previousState.isChecked != isChecked) {
			ListModelDiffCallback.PAYLOAD_CHECKED_CHANGED
		} else {
			super.getChangePayload(previousState)
		}
	}
}
