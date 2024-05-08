package com.example.dexreader.list.ui.adapter

import android.view.View
import com.example.dexreader.list.ui.model.ListHeader

interface ListHeaderClickListener {

	fun onListHeaderClick(item: ListHeader, view: View)
}
