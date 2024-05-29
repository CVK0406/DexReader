package com.example.dexreader.history.data

import com.example.dexreader.core.model.MangaHistory
import java.time.Instant

fun HistoryEntity.toMangaHistory() = MangaHistory(
	createdAt = Instant.ofEpochMilli(createdAt),
	updatedAt = Instant.ofEpochMilli(updatedAt),
	chapterId = chapterId,
	page = page,
	scroll = scroll.toInt(),
	percent = percent,
)
