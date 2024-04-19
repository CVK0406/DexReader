package com.example.dexreader.bookmarks.ui

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.dexreader.bookmarks.domain.Bookmark
import com.example.dexreader.core.util.ext.getItem
import com.example.dexreader.list.ui.MangaSelectionDecoration

class BookmarksSelectionDecoration(context: Context) : MangaSelectionDecoration(context) {

	override fun getItemId(parent: RecyclerView, child: View): Long {
		val holder = parent.getChildViewHolder(child) ?: return RecyclerView.NO_ID
		val item = holder.getItem(Bookmark::class.java) ?: return RecyclerView.NO_ID
		return item.pageId
	}
}
