package com.example.dexreader.core.parser

import org.example.dexreader.parsers.MangaLoaderContext
import org.example.dexreader.parsers.MangaParser
import org.example.dexreader.parsers.model.MangaSource

fun MangaParser(source: MangaSource, loaderContext: MangaLoaderContext): MangaParser {
	return if (source == MangaSource.DUMMY) {
		DummyParser(loaderContext)
	} else {
		loaderContext.newParserInstance(source)
	}
}
