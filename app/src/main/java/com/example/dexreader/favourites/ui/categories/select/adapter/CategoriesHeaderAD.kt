package com.example.dexreader.favourites.ui.categories.select.adapter

import android.view.View
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.example.dexreader.R
import com.example.dexreader.databinding.ItemCategoriesHeaderBinding
import com.example.dexreader.favourites.ui.categories.FavouriteCategoriesActivity
import com.example.dexreader.favourites.ui.categories.edit.FavouritesCategoryEditActivity
import com.example.dexreader.favourites.ui.categories.select.model.CategoriesHeaderItem
import com.example.dexreader.list.ui.model.ListModel

fun categoriesHeaderAD() = adapterDelegateViewBinding<CategoriesHeaderItem, ListModel, ItemCategoriesHeaderBinding>(
	{ inflater, parent -> ItemCategoriesHeaderBinding.inflate(inflater, parent, false) },
) {

	val onClickListener = View.OnClickListener { v ->
		val intent = when (v.id) {
			R.id.chip_create -> FavouritesCategoryEditActivity.newIntent(v.context)
			R.id.chip_manage -> FavouriteCategoriesActivity.newIntent(v.context)
			else -> return@OnClickListener
		}
		v.context.startActivity(intent)
	}

	binding.chipCreate.setOnClickListener(onClickListener)
	binding.chipManage.setOnClickListener(onClickListener)
}
