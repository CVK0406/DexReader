package com.example.dexreader.settings.tracker.categories

import com.example.dexreader.core.model.FavouriteCategory
import com.example.dexreader.core.ui.BaseListAdapter
import com.example.dexreader.core.ui.list.OnListItemClickListener

class TrackerCategoriesConfigAdapter(
	listener: OnListItemClickListener<FavouriteCategory>,
) : BaseListAdapter<FavouriteCategory>() {

	init {
		delegatesManager.addDelegate(trackerCategoryAD(listener))
	}
}
