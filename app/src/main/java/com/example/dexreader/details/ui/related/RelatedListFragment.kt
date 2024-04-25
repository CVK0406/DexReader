package com.example.dexreader.details.ui.related

import android.view.Menu
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import com.example.dexreader.R
import com.example.dexreader.core.ui.list.ListSelectionController
import com.example.dexreader.list.ui.MangaListFragment

@AndroidEntryPoint
class RelatedListFragment : MangaListFragment() {

	override val viewModel by viewModels<RelatedListViewModel>()
	override val isSwipeRefreshEnabled = false

	override fun onScrolledToEnd() = Unit

	override fun onCreateActionMode(controller: ListSelectionController, mode: ActionMode, menu: Menu): Boolean {
		mode.menuInflater.inflate(R.menu.mode_remote, menu)
		return super.onCreateActionMode(controller, mode, menu)
	}
}

