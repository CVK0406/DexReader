package com.example.dexreader.history.domain.model

import com.example.dexreader.core.model.MangaHistory
import org.example.dexreader.parsers.model.Manga

data class MangaWithHistory(
	val manga: Manga,
	val history: MangaHistory
)
