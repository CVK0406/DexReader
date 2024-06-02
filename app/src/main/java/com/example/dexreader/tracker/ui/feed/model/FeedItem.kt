package com.example.dexreader.tracker.ui.feed.model

import com.example.dexreader.list.ui.model.ListModel
import org.example.dexreader.parsers.model.Manga

data class FeedItem(
	val id: Long,
	val imageUrl: String,
	val title: String,
	val manga: Manga,
	val count: Int,
	val isNew: Boolean,
) : ListModel {
	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is FeedItem && other.id == id
	}
}
