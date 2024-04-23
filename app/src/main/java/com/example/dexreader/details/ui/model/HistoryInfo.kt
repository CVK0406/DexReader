package com.example.dexreader.details.ui.model

import com.example.dexreader.core.model.MangaHistory
import org.example.dexreader.parsers.model.Manga

data class HistoryInfo(
	val totalChapters: Int,
	val currentChapter: Int,
	val history: MangaHistory?,
) {
	val isValid: Boolean
		get() = totalChapters >= 0
}

fun HistoryInfo(
	manga: Manga?,
	branch: String?,
	history: MangaHistory?,
): HistoryInfo {
	val chapters = manga?.getChapters(branch)
	return HistoryInfo(
		totalChapters = chapters?.size ?: -1,
		currentChapter = if (history != null && !chapters.isNullOrEmpty()) {
			chapters.indexOfFirst { it.id == history.chapterId }
		} else {
			-1
		},
		history = history,
	)
}
