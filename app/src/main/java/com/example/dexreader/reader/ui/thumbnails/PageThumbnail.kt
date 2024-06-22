package com.example.dexreader.reader.ui.thumbnails

import com.example.dexreader.list.ui.model.ListModel
import com.example.dexreader.reader.ui.pager.ReaderPage

data class PageThumbnail(
	val isCurrent: Boolean,
	val page: ReaderPage,
) : ListModel {

	val number
		get() = page.index + 1

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is PageThumbnail && page == other.page
	}
}
