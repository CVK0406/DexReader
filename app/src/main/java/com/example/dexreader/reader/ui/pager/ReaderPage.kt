package com.example.dexreader.reader.ui.pager

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.example.dexreader.parsers.model.MangaPage
import org.example.dexreader.parsers.model.MangaSource

@Parcelize
data class ReaderPage(
	val id: Long,
	val url: String,
	val preview: String?,
	val chapterId: Long,
	val index: Int,
	val source: MangaSource,
) : Parcelable {

	constructor(page: MangaPage, index: Int, chapterId: Long) : this(
		id = page.id,
		url = page.url,
		preview = page.preview,
		chapterId = chapterId,
		index = index,
		source = page.source,
	)

	fun toMangaPage() = MangaPage(
		id = id,
		url = url,
		preview = preview,
		source = source,
	)
}
