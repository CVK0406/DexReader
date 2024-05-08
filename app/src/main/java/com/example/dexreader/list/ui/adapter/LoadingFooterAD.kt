package com.example.dexreader.list.ui.adapter

import com.example.dexreader.R
import com.example.dexreader.list.ui.model.ListModel
import com.example.dexreader.list.ui.model.LoadingFooter
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate

fun loadingFooterAD() = adapterDelegate<LoadingFooter, ListModel>(R.layout.item_loading_footer) {
}
