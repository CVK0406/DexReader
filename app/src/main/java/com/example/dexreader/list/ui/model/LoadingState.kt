package com.example.dexreader.list.ui.model

object LoadingState : ListModel {

	override fun equals(other: Any?): Boolean = other === LoadingState

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is LoadingState
	}
}
