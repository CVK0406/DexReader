package com.example.dexreader.reader.ui.thumbnails.adapter

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.example.dexreader.core.ui.BaseListAdapter
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.core.ui.list.fastscroll.FastScroller
import com.example.dexreader.list.ui.adapter.ListItemType
import com.example.dexreader.list.ui.adapter.listHeaderAD
import com.example.dexreader.list.ui.model.ListModel
import com.example.dexreader.reader.ui.thumbnails.PageThumbnail

class PageThumbnailAdapter(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	clickListener: OnListItemClickListener<PageThumbnail>,
) : BaseListAdapter<ListModel>(), FastScroller.SectionIndexer {

	init {
		addDelegate(ListItemType.PAGE_THUMB, pageThumbnailAD(coil, lifecycleOwner, clickListener))
		addDelegate(ListItemType.HEADER, listHeaderAD(null))
	}

	override fun getSectionText(context: Context, position: Int): CharSequence? {
		return findHeader(position)?.getText(context)
	}
}
