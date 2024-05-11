package com.example.dexreader.search.ui.suggestion

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import com.example.dexreader.R
import com.example.dexreader.core.util.ext.DIALOG_THEME_CENTERED
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SearchSuggestionMenuProvider(
	private val context: Context,
	private val viewModel: SearchSuggestionViewModel,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_search_suggestion, menu)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		return when (menuItem.itemId) {
			R.id.action_clear -> {
				clearSearchHistory()
				true
			}
			else -> false
		}
	}

	private fun clearSearchHistory() {
		MaterialAlertDialogBuilder(context, DIALOG_THEME_CENTERED)
			.setTitle(R.string.clear_search_history)
			.setIcon(R.drawable.ic_clear_all)
			.setMessage(R.string.text_clear_search_history_prompt)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.clear) { _, _ ->
				viewModel.clearSearchHistory()
			}.show()
	}
}
