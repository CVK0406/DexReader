package com.example.dexreader.details.ui

import android.content.Context
import com.example.dexreader.R
import com.example.dexreader.bookmarks.domain.Bookmark
import com.example.dexreader.core.model.MangaHistory
import com.example.dexreader.details.data.MangaDetails
import com.example.dexreader.details.ui.model.ChapterListItem
import com.example.dexreader.details.ui.model.toListItem
import com.example.dexreader.list.ui.model.ListHeader
import com.example.dexreader.list.ui.model.ListModel
import org.example.dexreader.parsers.util.mapToSet

fun MangaDetails.mapChapters(
	history: MangaHistory?,
	newCount: Int,
	branch: String?,
	bookmarks: List<Bookmark>,
	isGrid: Boolean,
): List<ChapterListItem> {
	val remoteChapters = chapters[branch].orEmpty()
	val localChapters = local?.manga?.getChapters(branch).orEmpty()
	if (remoteChapters.isEmpty() && localChapters.isEmpty()) {
		return emptyList()
	}
	val bookmarked = bookmarks.mapToSet { it.chapterId }
	val currentId = history?.chapterId ?: 0L
	val newFrom = if (newCount == 0 || remoteChapters.isEmpty()) Int.MAX_VALUE else remoteChapters.size - newCount
	val ids = buildSet(maxOf(remoteChapters.size, localChapters.size)) {
		remoteChapters.mapTo(this) { it.id }
		localChapters.mapTo(this) { it.id }
	}
	val result = ArrayList<ChapterListItem>(ids.size)
	val localMap = if (localChapters.isNotEmpty()) {
		localChapters.associateByTo(LinkedHashMap(localChapters.size)) { it.id }
	} else {
		null
	}
	var isUnread = currentId !in ids
	for (chapter in remoteChapters) {
		val local = localMap?.remove(chapter.id)
		if (chapter.id == currentId) {
			isUnread = true
		}
		result += (local ?: chapter).toListItem(
			isCurrent = chapter.id == currentId,
			isUnread = isUnread,
			isNew = isUnread && result.size >= newFrom,
			isDownloaded = local != null,
			isBookmarked = chapter.id in bookmarked,
			isGrid = isGrid,
		)
	}
	if (!localMap.isNullOrEmpty()) {
		for (chapter in localMap.values) {
			if (chapter.id == currentId) {
				isUnread = true
			}
			result += chapter.toListItem(
				isCurrent = chapter.id == currentId,
				isUnread = isUnread,
				isNew = false,
				isDownloaded = !isLocal,
				isBookmarked = chapter.id in bookmarked,
				isGrid = isGrid,
			)
		}
	}
	return result
}

fun List<ChapterListItem>.withVolumeHeaders(context: Context): List<ListModel> {
	var prevVolume = 0
	val result = ArrayList<ListModel>((size * 1.4).toInt())
	for (item in this) {
		val chapter = item.chapter
		if (chapter.volume != prevVolume) {
			val text = if (chapter.volume == 0) {
				context.getString(R.string.volume_unknown)
			} else {
				context.getString(R.string.volume_, chapter.volume)
			}
			result.add(ListHeader(text))
			prevVolume = chapter.volume
		}
		result.add(item)
	}
	return result
}
