package com.example.dexreader.reader.ui

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.fragment.app.FragmentActivity
import com.example.dexreader.R

class ReaderTopMenuProvider(
	private val activity: FragmentActivity,
	private val viewModel: ReaderViewModel,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_reader_top, menu)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		return when (menuItem.itemId) {
			R.id.action_chapters -> {
				ChaptersSheet.show(activity.supportFragmentManager)
				true
			}

			else -> false
		}
	}
}
