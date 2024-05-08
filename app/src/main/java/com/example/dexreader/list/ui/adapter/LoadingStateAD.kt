package com.example.dexreader.list.ui.adapter

import com.example.dexreader.R
import com.example.dexreader.list.ui.model.ListModel
import com.example.dexreader.list.ui.model.LoadingState
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate

fun loadingStateAD() = adapterDelegate<LoadingState, ListModel>(R.layout.item_loading_state) {
}
