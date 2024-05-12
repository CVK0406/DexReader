package com.example.dexreader.favourites.ui.categories.select.model

import com.example.dexreader.list.ui.model.ListModel

class CategoriesHeaderItem : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is CategoriesHeaderItem
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		return javaClass == other?.javaClass
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}
}
