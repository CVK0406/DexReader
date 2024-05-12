package com.example.dexreader.favourites.ui.categories.select.adapter

import com.example.dexreader.core.ui.BaseListAdapter
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.favourites.ui.categories.select.model.MangaCategoryItem
import com.example.dexreader.list.ui.model.ListModel

class MangaCategoriesAdapter(
	clickListener: OnListItemClickListener<MangaCategoryItem>,
) : BaseListAdapter<ListModel>() {

	init {
		delegatesManager.addDelegate(mangaCategoryAD(clickListener))
			.addDelegate(categoriesHeaderAD())
	}
}
