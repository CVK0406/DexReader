package com.example.dexreader.core.parser

import com.example.dexreader.core.exceptions.UnsupportedSourceException
import org.example.dexreader.parsers.MangaLoaderContext
import org.example.dexreader.parsers.MangaParser
import org.example.dexreader.parsers.config.ConfigKey
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.model.MangaChapter
import org.example.dexreader.parsers.model.MangaListFilter
import org.example.dexreader.parsers.model.MangaPage
import org.example.dexreader.parsers.model.MangaSource
import org.example.dexreader.parsers.model.MangaTag
import org.example.dexreader.parsers.model.SortOrder
import java.util.EnumSet

class DummyParser(context: MangaLoaderContext) : MangaParser(context, MangaSource.DUMMY) {

	override val configKeyDomain: ConfigKey.Domain
		get() = ConfigKey.Domain("localhost")

	override val availableSortOrders: Set<SortOrder>
		get() = EnumSet.allOf(SortOrder::class.java)

	override suspend fun getDetails(manga: Manga): Manga = stub(manga)

	override suspend fun getList(offset: Int, filter: MangaListFilter?): List<Manga> = stub(null)

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> = stub(null)

	override suspend fun getAvailableTags(): Set<MangaTag> = stub(null)

	private fun stub(manga: Manga?): Nothing {
		throw UnsupportedSourceException("Usage of Dummy parser", manga)
	}
}
