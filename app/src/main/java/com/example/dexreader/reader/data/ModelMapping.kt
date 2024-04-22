package com.example.dexreader.reader.data

import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.model.MangaChapter

fun Manga.filterChapters(branch: String?): Manga {
	if (chapters.isNullOrEmpty()) return this
	return withChapters(chapters = chapters?.filter { it.branch == branch })
}

private fun Manga.withChapters(chapters: List<MangaChapter>?) = Manga(
	id = id,
	title = title,
	altTitle = altTitle,
	url = url,
	publicUrl = publicUrl,
	rating = rating,
	isNsfw = isNsfw,
	coverUrl = coverUrl,
	tags = tags,
	state = state,
	author = author,
	largeCoverUrl = largeCoverUrl,
	description = description,
	chapters = chapters,
	source = source,
)
