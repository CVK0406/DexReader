package com.example.dexreader.search.ui.suggestion

import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.model.MangaSource
import org.example.dexreader.parsers.model.MangaTag

interface SearchSuggestionListener {

	fun onMangaClick(manga: Manga)

	fun onQueryClick(query: String, submit: Boolean)

	fun onQueryChanged(query: String)

	fun onSourceToggle(source: MangaSource, isEnabled: Boolean)

	fun onSourceClick(source: MangaSource)

	fun onTagClick(tag: MangaTag)
}
