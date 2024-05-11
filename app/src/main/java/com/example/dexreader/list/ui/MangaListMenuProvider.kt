package com.example.dexreader.list.ui

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import com.example.dexreader.R
import com.example.dexreader.favourites.ui.list.FavouritesListFragment
import com.example.dexreader.history.ui.HistoryListFragment
import com.example.dexreader.list.ui.config.ListConfigBottomSheet
import com.example.dexreader.list.ui.config.ListConfigSection

class MangaListMenuProvider(
	private val fragment: Fragment,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_list, menu)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
		R.id.action_list_mode -> {
			val section: ListConfigSection = when (fragment) {
				is HistoryListFragment -> ListConfigSection.History
				is FavouritesListFragment -> ListConfigSection.Favorites(fragment.categoryId)
				else -> ListConfigSection.General
			}
			ListConfigBottomSheet.show(fragment.childFragmentManager, section)
			true
		}

		else -> false
	}
}
