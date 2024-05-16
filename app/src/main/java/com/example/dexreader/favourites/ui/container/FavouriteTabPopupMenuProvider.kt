package com.example.dexreader.favourites.ui.container

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.dexreader.R
import com.example.dexreader.core.util.ext.DIALOG_THEME_CENTERED
import com.example.dexreader.favourites.ui.categories.edit.FavouritesCategoryEditActivity
import com.example.dexreader.favourites.ui.list.FavouritesListFragment.Companion.NO_ID

class FavouriteTabPopupMenuProvider(
	private val context: Context,
	private val viewModel: FavouritesContainerViewModel,
	private val categoryId: Long
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		val menuResId = if (categoryId == NO_ID) {
			R.menu.popup_fav_tab_all
		} else {
			R.menu.popup_fav_tab
		}
		menuInflater.inflate(menuResId, menu)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		when (menuItem.itemId) {
			R.id.action_hide -> viewModel.hide(categoryId)
			R.id.action_edit -> context.startActivity(
				FavouritesCategoryEditActivity.newIntent(context, categoryId),
			)

			R.id.action_delete -> confirmDelete()

			else -> return false
		}
		return true
	}

	private fun confirmDelete() {
		MaterialAlertDialogBuilder(context, DIALOG_THEME_CENTERED)
			.setMessage(R.string.categories_delete_confirm)
			.setTitle(R.string.remove_category)
			.setIcon(R.drawable.ic_delete)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.remove) { _, _ ->
				viewModel.deleteCategory(categoryId)
			}.show()
	}
}
