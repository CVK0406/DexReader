package com.example.dexreader.settings.sources.catalog

import com.example.dexreader.list.ui.ListModelDiffCallback
import com.example.dexreader.list.ui.model.ListModel
import org.example.dexreader.parsers.model.ContentType

data class SourceCatalogPage(
	val type: ContentType,
	val items: List<SourceCatalogItem>,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is SourceCatalogPage && other.type == type
	}

	override fun getChangePayload(previousState: ListModel): Any? {
		return ListModelDiffCallback.PAYLOAD_NESTED_LIST_CHANGED
	}
}
