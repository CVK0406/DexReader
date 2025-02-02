package com.example.dexreader.bookmarks.domain

import com.example.dexreader.list.ui.model.ListModel
import com.example.dexreader.local.data.hasImageExtension
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.model.MangaPage
import java.time.Instant

data class Bookmark(
	val manga: Manga,
	val pageId: Long,
	val chapterId: Long,
	val page: Int,
	val scroll: Int,
	val imageUrl: String,
	val createdAt: Instant,
	val percent: Float,
) : ListModel {
	val imageLoadData: Any
		get() = if (isImageUrlDirect()) imageUrl else toMangaPage()

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is Bookmark &&
			manga.id == other.manga.id &&
			chapterId == other.chapterId &&
			page == other.page
	}

	fun toMangaPage() = MangaPage(
		id = pageId,
		url = imageUrl,
		preview = null,
		source = manga.source,
	)

	private fun isImageUrlDirect(): Boolean {
		return hasImageExtension(imageUrl)
	}
}
