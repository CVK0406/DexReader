package com.example.dexreader.search.ui

import android.view.Menu
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.viewModels
import com.example.dexreader.R
import com.example.dexreader.core.ui.list.ListSelectionController
import com.example.dexreader.core.util.ext.withArgs
import com.example.dexreader.list.ui.MangaListFragment
import dagger.hilt.android.AndroidEntryPoint
import org.example.dexreader.parsers.model.MangaSource

@AndroidEntryPoint
class SearchFragment : MangaListFragment() {

	override val viewModel by viewModels<SearchViewModel>()

	override fun onScrolledToEnd() {
		viewModel.loadNextPage()
	}

	override fun onCreateActionMode(controller: ListSelectionController, mode: ActionMode, menu: Menu): Boolean {
		mode.menuInflater.inflate(R.menu.mode_remote, menu)
		return super.onCreateActionMode(controller, mode, menu)
	}

	companion object {

		const val ARG_QUERY = "query"
		const val ARG_SOURCE = "source"

		fun newInstance(source: MangaSource, query: String) = SearchFragment().withArgs(2) {
			putSerializable(ARG_SOURCE, source)
			putString(ARG_QUERY, query)
		}
	}
}
