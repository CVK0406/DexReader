package com.example.dexreader.favourites.ui.categories.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.example.dexreader.core.ui.ReorderableListAdapter
import com.example.dexreader.favourites.ui.categories.FavouriteCategoriesListListener
import com.example.dexreader.list.ui.adapter.ListItemType
import com.example.dexreader.list.ui.adapter.ListStateHolderListener
import com.example.dexreader.list.ui.adapter.emptyStateListAD
import com.example.dexreader.list.ui.adapter.loadingStateAD
import com.example.dexreader.list.ui.model.ListModel

class CategoriesAdapter(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	onItemClickListener: FavouriteCategoriesListListener,
	listListener: ListStateHolderListener,
) : ReorderableListAdapter<ListModel>() {

	init {
		addDelegate(ListItemType.CATEGORY_LARGE, categoryAD(coil, lifecycleOwner, onItemClickListener))
		addDelegate(ListItemType.NAV_ITEM, allCategoriesAD(coil, lifecycleOwner, onItemClickListener))
		addDelegate(ListItemType.STATE_EMPTY, emptyStateListAD(coil, lifecycleOwner, listListener))
		addDelegate(ListItemType.STATE_LOADING, loadingStateAD())
	}
}
