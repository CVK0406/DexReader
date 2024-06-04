package com.example.dexreader.tracker.ui.feed.model

import com.example.dexreader.tracker.domain.model.TrackingLogItem

fun TrackingLogItem.toFeedItem() = FeedItem(
	id = id,
	imageUrl = manga.coverUrl,
	title = manga.title,
	count = chapters.size,
	manga = manga,
	isNew = isNew,
)
