package com.example.dexreader.settings.sources.catalog

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.dexreader.list.ui.model.ListModel
import org.example.dexreader.parsers.model.MangaSource

sealed interface SourceCatalogItem : ListModel {

	data class Source(
		val source: MangaSource,
		val showSummary: Boolean,
	) : SourceCatalogItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is Source && other.source == source
		}
	}

	data class Hint(
		@DrawableRes val icon: Int,
		@StringRes val title: Int,
		@StringRes val text: Int,
	) : SourceCatalogItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is Hint && other.title == title
		}
	}
}
