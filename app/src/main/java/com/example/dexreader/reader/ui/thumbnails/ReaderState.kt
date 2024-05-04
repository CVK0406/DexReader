package com.example.dexreader.reader.ui

import android.os.Parcelable
import com.example.dexreader.core.model.MangaHistory
import kotlinx.parcelize.Parcelize
import org.example.dexreader.parsers.model.Manga

@Parcelize
data class ReaderState(
	val chapterId: Long,
	val page: Int,
	val scroll: Int,
) : Parcelable {

	constructor(history: MangaHistory) : this(
		chapterId = history.chapterId,
		page = history.page,
		scroll = history.scroll,
	)

	constructor(manga: Manga, branch: String?) : this(
		chapterId = manga.chapters?.firstOrNull {
			it.branch == branch
		}?.id ?: error("Cannot find first chapter"),
		page = 0,
		scroll = 0,
	)
}
