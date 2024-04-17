package com.example.dexreader.explore.ui.model

import com.example.dexreader.list.ui.model.ListModel
import org.example.dexreader.parsers.model.MangaSource

data class MangaSourceItem(
	val source: MangaSource,
	val isGrid: Boolean,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is MangaSourceItem && other.source == source
	}
}
