package com.example.dexreader.list.ui.model

import org.example.dexreader.parsers.model.Manga

data class MangaGridModel(
	override val id: Long,
	override val title: String,
	override val coverUrl: String,
	override val manga: Manga,
	override val counter: Int,
	override val progress: Float,
) : MangaItemModel()
