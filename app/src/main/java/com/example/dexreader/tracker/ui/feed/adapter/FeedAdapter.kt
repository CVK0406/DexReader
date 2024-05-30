package com.example.dexreader.tracker.ui.feed.adapter

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.example.dexreader.core.ui.BaseListAdapter
import com.example.dexreader.core.ui.list.fastscroll.FastScroller
import com.example.dexreader.list.ui.adapter.ListItemType
import com.example.dexreader.list.ui.adapter.MangaListListener
import com.example.dexreader.list.ui.adapter.emptyStateListAD
import com.example.dexreader.list.ui.adapter.errorFooterAD
import com.example.dexreader.list.ui.adapter.errorStateListAD
import com.example.dexreader.list.ui.adapter.listHeaderAD
import com.example.dexreader.list.ui.adapter.loadingFooterAD
import com.example.dexreader.list.ui.adapter.loadingStateAD
import com.example.dexreader.list.ui.model.ListModel
import com.example.dexreader.list.ui.size.ItemSizeResolver

class FeedAdapter(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	listener: MangaListListener,
	sizeResolver: ItemSizeResolver,
) : BaseListAdapter<ListModel>(), FastScroller.SectionIndexer {

	init {
		addDelegate(ListItemType.FEED, feedItemAD(coil, lifecycleOwner, listener))
		addDelegate(
			ListItemType.MANGA_NESTED_GROUP,
			updatedMangaAD(
				lifecycleOwner = lifecycleOwner,
				coil = coil,
				sizeResolver = sizeResolver,
				listener = listener,
				headerClickListener = listener,
			),
		)
		addDelegate(ListItemType.FOOTER_LOADING, loadingFooterAD())
		addDelegate(ListItemType.STATE_LOADING, loadingStateAD())
		addDelegate(ListItemType.FOOTER_ERROR, errorFooterAD(listener))
		addDelegate(ListItemType.STATE_ERROR, errorStateListAD(listener))
		addDelegate(ListItemType.HEADER, listHeaderAD(listener))
		addDelegate(ListItemType.STATE_EMPTY, emptyStateListAD(coil, lifecycleOwner, listener))
	}

	override fun getSectionText(context: Context, position: Int): CharSequence? {
		return findHeader(position)?.getText(context)
	}
}
