package com.example.dexreader.tracker.ui.feed

import android.content.Context
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import com.example.dexreader.R
import com.example.dexreader.core.ui.dialog.CheckBoxAlertDialog
import com.example.dexreader.tracker.ui.updates.UpdatesActivity

class FeedMenuProvider(
	private val snackbarHost: View,
	private val viewModel: FeedViewModel,
) : MenuProvider {

	private val context: Context
		get() = snackbarHost.context

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_feed, menu)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
		R.id.action_update -> {
			viewModel.update()
			true
		}

		R.id.action_clear_feed -> {
			CheckBoxAlertDialog.Builder(context)
				.setTitle(R.string.clear_updates_feed)
				.setMessage(R.string.text_clear_updates_feed_prompt)
				.setNegativeButton(android.R.string.cancel, null)
				.setCheckBoxChecked(true)
				.setCheckBoxText(R.string.clear_new_chapters_counters)
				.setPositiveButton(R.string.clear) { _, isChecked ->
					viewModel.clearFeed(isChecked)
				}.create().show()
			true
		}

		R.id.action_updated -> {
			context.startActivity(UpdatesActivity.newIntent(context))
			true
		}

		else -> false
	}
}
