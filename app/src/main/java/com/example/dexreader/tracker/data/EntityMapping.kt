package com.example.dexreader.tracker.data

import com.example.dexreader.core.db.entity.toManga
import com.example.dexreader.core.db.entity.toMangaTags
import com.example.dexreader.tracker.domain.model.TrackingLogItem
import java.time.Instant

fun TrackLogWithManga.toTrackingLogItem(counters: MutableMap<Long, Int>): TrackingLogItem {
	val chaptersList = trackLog.chapters.split('\n').filterNot { x -> x.isEmpty() }
	return TrackingLogItem(
		id = trackLog.id,
		chapters = chaptersList,
		manga = manga.toManga(tags.toMangaTags()),
		createdAt = Instant.ofEpochMilli(trackLog.createdAt),
		isNew = counters.decrement(trackLog.mangaId, chaptersList.size),
	)
}

private fun MutableMap<Long, Int>.decrement(key: Long, count: Int): Boolean = synchronized(this) {
	val counter = get(key)
	if (counter == null || counter <= 0) {
		return false
	}
	if (counter < count) {
		remove(key)
	} else {
		put(key, counter - count)
	}
	return true
}
