package com.example.dexreader.list.ui.adapter

import android.view.View
import org.example.dexreader.parsers.model.MangaTag

interface MangaListListener : MangaDetailsClickListener, ListStateHolderListener, ListHeaderClickListener {

	fun onUpdateFilter(tags: Set<MangaTag>)

	fun onFilterClick(view: View?)
}
