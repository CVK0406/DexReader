package com.example.dexreader.explore.ui.model

import com.example.dexreader.list.ui.model.ListModel
import org.example.dexreader.parsers.model.Manga

data class RecommendationsItem(
	val manga: Manga
) : ListModel {
	val summary: String = manga.tags.joinToString { it.title }

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is RecommendationsItem
	}
}
