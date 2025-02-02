package com.example.dexreader.details.ui.model

import com.example.dexreader.details.ui.model.ChapterListItem.Companion.FLAG_BOOKMARKED
import com.example.dexreader.details.ui.model.ChapterListItem.Companion.FLAG_CURRENT
import com.example.dexreader.details.ui.model.ChapterListItem.Companion.FLAG_DOWNLOADED
import com.example.dexreader.details.ui.model.ChapterListItem.Companion.FLAG_GRID
import com.example.dexreader.details.ui.model.ChapterListItem.Companion.FLAG_NEW
import com.example.dexreader.details.ui.model.ChapterListItem.Companion.FLAG_UNREAD
import org.example.dexreader.parsers.model.MangaChapter

fun MangaChapter.toListItem(
	isCurrent: Boolean,
	isUnread: Boolean,
	isNew: Boolean,
	isDownloaded: Boolean,
	isBookmarked: Boolean,
	isGrid: Boolean,
): ChapterListItem {
	var flags = 0
	if (isCurrent) flags = flags or FLAG_CURRENT
	if (isUnread) flags = flags or FLAG_UNREAD
	if (isNew) flags = flags or FLAG_NEW
	if (isBookmarked) flags = flags or FLAG_BOOKMARKED
	if (isDownloaded) flags = flags or FLAG_DOWNLOADED
	if (isGrid) flags = flags or FLAG_GRID
	return ChapterListItem(
		chapter = this,
		flags = flags,
		uploadDateMs = uploadDate,
	)
}
