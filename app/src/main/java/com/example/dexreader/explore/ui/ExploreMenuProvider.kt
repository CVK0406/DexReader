package com.example.dexreader.explore.ui

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import com.example.dexreader.R
import com.example.dexreader.settings.SettingsActivity

class ExploreMenuProvider(
	private val context: Context,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_explore, menu)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		return when (menuItem.itemId) {
			R.id.action_manage -> {
				context.startActivity(SettingsActivity.newSourcesSettingsIntent(context))
				true
			}
			else -> false
		}
	}
}
