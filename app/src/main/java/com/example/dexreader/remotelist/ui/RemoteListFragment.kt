package com.example.dexreader.remotelist.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import com.example.dexreader.R
import com.example.dexreader.core.ui.list.ListSelectionController
import com.example.dexreader.core.ui.util.MenuInvalidator
import com.example.dexreader.core.util.ext.addMenuProvider
import com.example.dexreader.core.util.ext.observe
import com.example.dexreader.core.util.ext.observeEvent
import com.example.dexreader.core.util.ext.withArgs
import com.example.dexreader.databinding.FragmentListBinding
import com.example.dexreader.details.ui.DetailsActivity
import com.example.dexreader.filter.ui.FilterOwner
import com.example.dexreader.filter.ui.MangaFilter
import com.example.dexreader.filter.ui.sheet.FilterSheetFragment
import com.example.dexreader.list.ui.MangaListFragment
import com.example.dexreader.main.ui.owners.AppBarOwner
import org.example.dexreader.parsers.model.MangaSource
import com.example.dexreader.search.ui.SearchActivity
import com.example.dexreader.settings.SettingsActivity

@AndroidEntryPoint
class RemoteListFragment : MangaListFragment(), FilterOwner {

	override val viewModel by viewModels<RemoteListViewModel>()

	override val filter: MangaFilter
		get() = viewModel

	override fun onViewBindingCreated(binding: FragmentListBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		addMenuProvider(RemoteListMenuProvider())
		viewModel.isRandomLoading.observe(viewLifecycleOwner, MenuInvalidator(requireActivity()))
		viewModel.onOpenManga.observeEvent(viewLifecycleOwner) {
			startActivity(DetailsActivity.newIntent(binding.root.context, it))
		}
		viewModel.header.distinctUntilChangedBy { it.isFilterApplied }
			.drop(1)
			.observe(viewLifecycleOwner) {
				activity?.invalidateMenu()
			}
	}

	override fun onScrolledToEnd() {
		viewModel.loadNextPage()
	}

	override fun onCreateActionMode(controller: ListSelectionController, mode: ActionMode, menu: Menu): Boolean {
		mode.menuInflater.inflate(R.menu.mode_remote, menu)
		return super.onCreateActionMode(controller, mode, menu)
	}

	override fun onFilterClick(view: View?) {
		FilterSheetFragment.show(childFragmentManager)
	}

	override fun onEmptyActionClick() {
		viewModel.resetFilter()
	}

	private inner class RemoteListMenuProvider :
		MenuProvider,
		SearchView.OnQueryTextListener,
		MenuItem.OnActionExpandListener {

		override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
			menuInflater.inflate(R.menu.opt_list_remote, menu)
			val searchMenuItem = menu.findItem(R.id.action_search)
			searchMenuItem.setOnActionExpandListener(this)
			val searchView = searchMenuItem.actionView as SearchView
			searchView.setOnQueryTextListener(this)
			searchView.setIconifiedByDefault(false)
			searchView.queryHint = searchMenuItem.title
		}

		override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
			R.id.action_source_settings -> {
				startActivity(SettingsActivity.newSourceSettingsIntent(requireContext(), viewModel.source))
				true
			}

			R.id.action_random -> {
				viewModel.openRandom()
				true
			}

			R.id.action_filter -> {
				onFilterClick(null)
				true
			}

			R.id.action_filter_reset -> {
				viewModel.resetFilter()
				true
			}

			else -> false
		}

		override fun onPrepareMenu(menu: Menu) {
			super.onPrepareMenu(menu)
			menu.findItem(R.id.action_search)?.isVisible = viewModel.isSearchAvailable
			menu.findItem(R.id.action_random)?.isEnabled = !viewModel.isRandomLoading.value
			menu.findItem(R.id.action_filter_reset)?.isVisible = viewModel.header.value.isFilterApplied
		}

		override fun onQueryTextSubmit(query: String?): Boolean {
			if (query.isNullOrEmpty()) {
				return false
			}
			val intent = SearchActivity.newIntent(
				context = this@RemoteListFragment.context ?: return false,
				source = viewModel.source,
				query = query,
			)
			startActivity(intent)
			return true
		}

		override fun onQueryTextChange(newText: String?): Boolean = false

		override fun onMenuItemActionExpand(item: MenuItem): Boolean {
			(activity as? AppBarOwner)?.appBar?.setExpanded(false, true)
			return true
		}

		override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
			val searchView = (item.actionView as? SearchView) ?: return false
			searchView.setQuery("", false)
			return true
		}
	}

	companion object {

		const val ARG_SOURCE = "provider"

		fun newInstance(provider: MangaSource) = RemoteListFragment().withArgs(1) {
			putSerializable(ARG_SOURCE, provider)
		}
	}
}
