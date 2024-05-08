package com.example.dexreader.search.ui.multi.adapter

import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import coil.ImageLoader
import com.example.dexreader.core.ui.BaseListAdapter
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.list.ui.MangaSelectionDecoration
import com.example.dexreader.list.ui.adapter.ListItemType
import com.example.dexreader.list.ui.adapter.MangaListListener
import com.example.dexreader.list.ui.adapter.emptyStateListAD
import com.example.dexreader.list.ui.adapter.errorStateListAD
import com.example.dexreader.list.ui.adapter.loadingFooterAD
import com.example.dexreader.list.ui.adapter.loadingStateAD
import com.example.dexreader.list.ui.model.ListModel
import com.example.dexreader.list.ui.size.ItemSizeResolver
import com.example.dexreader.search.ui.multi.MultiSearchListModel

class MultiSearchAdapter(
	lifecycleOwner: LifecycleOwner,
	coil: ImageLoader,
	listener: MangaListListener,
	itemClickListener: OnListItemClickListener<MultiSearchListModel>,
	sizeResolver: ItemSizeResolver,
	selectionDecoration: MangaSelectionDecoration,
) : BaseListAdapter<ListModel>() {

	init {
		val pool = RecycledViewPool()
		addDelegate(
			ListItemType.MANGA_NESTED_GROUP,
			searchResultsAD(
				sharedPool = pool,
				lifecycleOwner = lifecycleOwner,
				coil = coil,
				sizeResolver = sizeResolver,
				selectionDecoration = selectionDecoration,
				listener = listener,
				itemClickListener = itemClickListener,
			),
		)
		addDelegate(ListItemType.STATE_LOADING, loadingStateAD())
		addDelegate(ListItemType.FOOTER_LOADING, loadingFooterAD())
		addDelegate(ListItemType.STATE_EMPTY, emptyStateListAD(coil, lifecycleOwner, listener))
		addDelegate(ListItemType.STATE_ERROR, errorStateListAD(listener))
	}
}
