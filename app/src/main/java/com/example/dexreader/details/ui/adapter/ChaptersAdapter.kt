package com.example.dexreader.details.ui.adapter

import android.content.Context
import com.example.dexreader.core.ui.BaseListAdapter
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.core.ui.list.fastscroll.FastScroller
import com.example.dexreader.details.ui.model.ChapterListItem
import com.example.dexreader.list.ui.adapter.ListItemType
import com.example.dexreader.list.ui.adapter.listHeaderAD
import com.example.dexreader.list.ui.model.ListModel

class ChaptersAdapter(
	private val onItemClickListener: OnListItemClickListener<ChapterListItem>,
) : BaseListAdapter<ListModel>(), FastScroller.SectionIndexer {

	init {
		addDelegate(ListItemType.HEADER, listHeaderAD(null))
		addDelegate(ListItemType.CHAPTER_LIST, chapterListItemAD(onItemClickListener))
		addDelegate(ListItemType.CHAPTER_GRID, chapterGridItemAD(onItemClickListener))
	}

	override fun getSectionText(context: Context, position: Int): CharSequence? {
		return findHeader(position)?.getText(context)
	}
}
