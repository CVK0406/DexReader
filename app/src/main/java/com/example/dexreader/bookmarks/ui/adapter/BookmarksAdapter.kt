package com.example.dexreader.bookmarks.ui.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.example.dexreader.bookmarks.domain.Bookmark
import com.example.dexreader.core.ui.BaseListAdapter
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.list.ui.adapter.ListItemType

class BookmarksAdapter(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	clickListener: OnListItemClickListener<Bookmark>,
) : BaseListAdapter<Bookmark>() {

	init {
		addDelegate(ListItemType.PAGE_THUMB, bookmarkListAD(coil, lifecycleOwner, clickListener))
	}
}
