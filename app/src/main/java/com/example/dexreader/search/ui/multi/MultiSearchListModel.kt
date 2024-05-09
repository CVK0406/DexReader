package com.example.dexreader.search.ui.multi

import com.example.dexreader.list.ui.ListModelDiffCallback
import com.example.dexreader.list.ui.model.ListModel
import com.example.dexreader.list.ui.model.MangaItemModel
import org.example.dexreader.parsers.model.MangaSource

data class MultiSearchListModel(
	val source: MangaSource,
	val hasMore: Boolean,
	val list: List<MangaItemModel>,
	val error: Throwable?,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is MultiSearchListModel && source == other.source
	}

	override fun getChangePayload(previousState: ListModel): Any? {
		return if (previousState is MultiSearchListModel && previousState.list != list) {
			ListModelDiffCallback.PAYLOAD_NESTED_LIST_CHANGED
		} else {
			super.getChangePayload(previousState)
		}
	}
}
