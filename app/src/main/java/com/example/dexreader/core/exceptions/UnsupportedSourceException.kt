package com.example.dexreader.core.exceptions

import org.example.dexreader.parsers.model.Manga

class UnsupportedSourceException(
	message: String?,
	val manga: Manga?,
) : IllegalArgumentException(message)
