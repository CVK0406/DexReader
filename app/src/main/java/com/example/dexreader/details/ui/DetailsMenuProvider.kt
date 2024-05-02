package com.example.dexreader.details.ui

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.core.view.MenuProvider
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.dexreader.R
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.core.util.ShareHelper
import com.example.dexreader.download.ui.dialog.DownloadOption
import com.example.dexreader.favourites.ui.categories.select.FavoriteSheet
import org.example.dexreader.parsers.model.MangaSource
import com.example.dexreader.stats.ui.sheet.MangaStatsSheet

class DetailsMenuProvider(
	private val activity: FragmentActivity,
	private val viewModel: DetailsViewModel,
	private val snackbarHost: View,
) : MenuProvider, OnListItemClickListener<DownloadOption> {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_details, menu)
	}

	override fun onPrepareMenu(menu: Menu) {
		val manga = viewModel.manga.value
		menu.findItem(R.id.action_save).isVisible = manga?.source != null && manga.source != MangaSource.LOCAL
		menu.findItem(R.id.action_delete).isVisible = manga?.source == MangaSource.LOCAL
		menu.findItem(R.id.action_stats).isVisible = viewModel.isStatsAvailable.value
		menu.findItem(R.id.action_favourite).setIcon(
			if (viewModel.favouriteCategories.value) R.drawable.ic_heart else R.drawable.ic_heart_outline,
		)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		when (menuItem.itemId) {
			R.id.action_favourite -> {
				viewModel.manga.value?.let {
					FavoriteSheet.show(activity.supportFragmentManager, it)
				}
			}

			R.id.action_delete -> {
				val title = viewModel.manga.value?.title.orEmpty()
				MaterialAlertDialogBuilder(activity)
					.setTitle(R.string.delete_manga)
					.setMessage(activity.getString(R.string.text_delete_local_manga, title))
					.setPositiveButton(R.string.delete) { _, _ ->
						viewModel.deleteLocal()
					}
					.setNegativeButton(android.R.string.cancel, null)
					.show()
			}

			R.id.action_save -> {
				DownloadDialogHelper(snackbarHost, viewModel).show(this)
			}

			R.id.action_stats -> {
				viewModel.manga.value?.let {
					MangaStatsSheet.show(activity.supportFragmentManager, it)
				}
			}

			else -> return false
		}
		return true
	}

	override fun onItemClick(item: DownloadOption, view: View) {
		val chaptersIds: Set<Long>? = when (item) {
			is DownloadOption.WholeManga -> null
			is DownloadOption.SelectionHint -> {
				viewModel.startChaptersSelection()
				return
			}

			else -> item.chaptersIds
		}
		viewModel.download(chaptersIds)
	}
}
