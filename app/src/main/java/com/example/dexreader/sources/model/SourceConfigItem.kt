package com.example.dexreader.settings.sources.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.dexreader.list.ui.model.ListModel
import org.example.dexreader.parsers.model.ContentType
import org.example.dexreader.parsers.model.MangaSource

sealed interface SourceConfigItem : ListModel {

	data class SourceItem(
		val source: MangaSource,
		val isEnabled: Boolean,
		val isDraggable: Boolean,
		val isAvailable: Boolean,
	) : SourceConfigItem {

		val isNsfw: Boolean
			get() = source.contentType == ContentType.HENTAI

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is SourceItem && other.source == source
		}
	}

	data class Tip(
		val key: String,
		@DrawableRes val iconResId: Int,
		@StringRes val textResId: Int,
	) : SourceConfigItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is Tip && other.key == key
		}
	}

	data object EmptySearchResult : SourceConfigItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is EmptySearchResult
		}
	}
}
