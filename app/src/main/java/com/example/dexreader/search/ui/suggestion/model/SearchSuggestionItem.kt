package com.example.dexreader.search.ui.suggestion.model

import com.example.dexreader.core.ui.widgets.ChipsView
import com.example.dexreader.list.ui.ListModelDiffCallback
import com.example.dexreader.list.ui.model.ListModel
import org.example.dexreader.parsers.model.ContentType
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.model.MangaSource

sealed interface SearchSuggestionItem : ListModel {

	data class MangaList(
		val items: List<Manga>,
	) : SearchSuggestionItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is MangaList
		}
	}

	data class RecentQuery(
		val query: String,
	) : SearchSuggestionItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is RecentQuery && query == other.query
		}
	}

	data class Hint(
		val query: String,
	) : SearchSuggestionItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is Hint && query == other.query
		}
	}

	data class Source(
		val source: MangaSource,
		val isEnabled: Boolean,
	) : SearchSuggestionItem {

		val isNsfw: Boolean
			get() = source.contentType == ContentType.HENTAI

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is Source && other.source == source
		}

		override fun getChangePayload(previousState: ListModel): Any? {
			if (previousState !is Source) {
				return super.getChangePayload(previousState)
			}
			return if (isEnabled != previousState.isEnabled) {
				ListModelDiffCallback.PAYLOAD_CHECKED_CHANGED
			} else {
				null
			}
		}
	}

	data class Tags(
		val tags: List<ChipsView.ChipModel>,
	) : SearchSuggestionItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is Tags
		}

		override fun getChangePayload(previousState: ListModel): Any {
			return ListModelDiffCallback.PAYLOAD_NESTED_LIST_CHANGED
		}
	}
}
