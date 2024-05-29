package com.example.dexreader.tracker.work

import com.example.dexreader.tracker.domain.model.MangaTracking

data class TrackingItem(
	val tracking: MangaTracking,
	val channelId: String?,
)
