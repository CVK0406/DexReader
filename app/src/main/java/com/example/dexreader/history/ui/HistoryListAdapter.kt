package com.example.dexreader.history.ui

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.example.dexreader.core.ui.list.fastscroll.FastScroller
import com.example.dexreader.list.ui.adapter.MangaListAdapter
import com.example.dexreader.list.ui.adapter.MangaListListener
import com.example.dexreader.list.ui.size.ItemSizeResolver

class HistoryListAdapter(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	listener: MangaListListener,
	sizeResolver: ItemSizeResolver,
) : MangaListAdapter(coil, lifecycleOwner, listener, sizeResolver), FastScroller.SectionIndexer {

	override fun getSectionText(context: Context, position: Int): CharSequence? {
		return findHeader(position)?.getText(context)
	}
}
