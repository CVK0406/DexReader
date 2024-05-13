package com.example.dexreader.favourites.ui.categories.adapter

import com.example.dexreader.favourites.domain.model.Cover
import com.example.dexreader.list.ui.ListModelDiffCallback
import com.example.dexreader.list.ui.model.ListModel

data class AllCategoriesListModel(
	val mangaCount: Int,
	val covers: List<Cover>,
	val isVisible: Boolean,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is AllCategoriesListModel
	}

	override fun getChangePayload(previousState: ListModel): Any? {
		return if (previousState is AllCategoriesListModel && previousState.isVisible != isVisible) {
			ListModelDiffCallback.PAYLOAD_CHECKED_CHANGED
		} else {
			super.getChangePayload(previousState)
		}
	}
}
