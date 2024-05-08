package com.example.dexreader.list.ui.adapter

import android.view.View
import com.example.dexreader.core.ui.list.OnListItemClickListener
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.model.MangaTag

interface MangaDetailsClickListener : OnListItemClickListener<Manga> {

	fun onReadClick(manga: Manga, view: View)

	fun onTagClick(manga: Manga, tag: MangaTag, view: View)
}
