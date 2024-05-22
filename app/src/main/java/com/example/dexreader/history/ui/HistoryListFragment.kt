package com.example.dexreader.history.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import com.example.dexreader.R
import com.example.dexreader.core.ui.list.ListSelectionController
import com.example.dexreader.core.ui.list.RecyclerScrollKeeper
import com.example.dexreader.core.ui.util.MenuInvalidator
import com.example.dexreader.core.util.ext.addMenuProvider
import com.example.dexreader.core.util.ext.observe
import com.example.dexreader.databinding.FragmentListBinding
import com.example.dexreader.list.ui.MangaListFragment
import com.example.dexreader.list.ui.size.DynamicItemSizeResolver
import org.example.dexreader.parsers.model.MangaSource

@AndroidEntryPoint
class HistoryListFragment : MangaListFragment() {

	override val viewModel by viewModels<HistoryListViewModel>()
	override val isSwipeRefreshEnabled = false

	override fun onViewBindingCreated(binding: FragmentListBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		RecyclerScrollKeeper(binding.recyclerView).attach()
		addMenuProvider(HistoryListMenuProvider(binding.root.context, viewModel))
		viewModel.isStatsEnabled.observe(viewLifecycleOwner, MenuInvalidator(requireActivity()))
	}

	override fun onScrolledToEnd() = Unit

	override fun onCreateActionMode(controller: ListSelectionController, mode: ActionMode, menu: Menu): Boolean {
		mode.menuInflater.inflate(R.menu.mode_history, menu)
		return super.onCreateActionMode(controller, mode, menu)
	}

	override fun onPrepareActionMode(controller: ListSelectionController, mode: ActionMode, menu: Menu): Boolean {
		menu.findItem(R.id.action_save)?.isVisible = selectedItems.none {
			it.source == MangaSource.LOCAL
		}
		return super.onPrepareActionMode(controller, mode, menu)
	}

	override fun onActionItemClicked(controller: ListSelectionController, mode: ActionMode, item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_remove -> {
				viewModel.removeFromHistory(selectedItemsIds)
				mode.finish()
				true
			}

			R.id.action_mark_current -> {
				MaterialAlertDialogBuilder(context ?: return false)
					.setTitle(item.title)
					.setMessage(R.string.mark_as_completed_prompt)
					.setNegativeButton(android.R.string.cancel, null)
					.setPositiveButton(android.R.string.ok) { _, _ ->
						viewModel.markAsRead(selectedItems)
						mode.finish()
					}.show()
				true
			}

			else -> super.onActionItemClicked(controller, mode, item)
		}
	}

	override fun onCreateAdapter() = HistoryListAdapter(
		coil,
		viewLifecycleOwner,
		this,
		DynamicItemSizeResolver(resources, settings, adjustWidth = false),
	)
}
