package com.example.dexreader.favourites.domain.model

import org.example.dexreader.parsers.model.MangaSource
import org.example.dexreader.parsers.util.find

data class Cover(
	val url: String,
	val source: String,
) {
	val mangaSource: MangaSource?
		get() = if (source.isEmpty()) null else MangaSource.entries.find(source)
}
