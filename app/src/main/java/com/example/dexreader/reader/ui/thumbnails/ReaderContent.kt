package com.example.dexreader.reader.ui

import com.example.dexreader.reader.ui.pager.ReaderPage

data class ReaderContent(
	val pages: List<ReaderPage>,
	val state: ReaderState?
)
