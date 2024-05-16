package com.example.dexreader.favourites.ui.container

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import com.example.dexreader.R
import com.example.dexreader.favourites.ui.categories.FavouriteCategoriesActivity

class FavouritesContainerMenuProvider(
	private val context: Context,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_favourites_container, menu)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		when (menuItem.itemId) {
			R.id.action_manage -> {
				context.startActivity(FavouriteCategoriesActivity.newIntent(context))
			}

			else -> return false
		}
		return true
	}
}
