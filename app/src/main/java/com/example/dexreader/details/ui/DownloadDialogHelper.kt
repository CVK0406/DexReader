package com.example.dexreader.details.ui

import android.content.DialogInterface
import android.view.View
import com.example.dexreader.R
import com.example.dexreader.core.model.ids
import com.example.dexreader.core.ui.dialog.RecyclerViewAlertDialog
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.download.ui.dialog.DownloadOption
import com.example.dexreader.download.ui.dialog.downloadOptionAD
import com.example.dexreader.settings.SettingsActivity

class DownloadDialogHelper(
	private val host: View,
	private val viewModel: DetailsViewModel,
) {

	fun show(callback: OnListItemClickListener<DownloadOption>) {
		val branch = viewModel.selectedBranchValue
		val allChapters = viewModel.manga.value?.chapters ?: return
		val branchChapters = viewModel.manga.value?.getChapters(branch).orEmpty()
		val history = viewModel.history.value

		val options = buildList {
			add(DownloadOption.WholeManga(allChapters.ids()))
			if (branch != null && branchChapters.isNotEmpty()) {
				add(DownloadOption.AllChapters(branch, branchChapters.ids()))
			}

			if (history != null) {
				val unreadChapters = branchChapters.takeLastWhile { it.id != history.chapterId }
				if (unreadChapters.isNotEmpty() && unreadChapters.size < branchChapters.size) {
					add(DownloadOption.AllUnreadChapters(unreadChapters.ids(), branch))
					if (unreadChapters.size > 5) {
						add(DownloadOption.NextUnreadChapters(unreadChapters.take(5).ids()))
						if (unreadChapters.size > 10) {
							add(DownloadOption.NextUnreadChapters(unreadChapters.take(10).ids()))
						}
					}
				}
			} else {
				if (branchChapters.size > 5) {
					add(DownloadOption.FirstChapters(branchChapters.take(5).ids()))
					if (branchChapters.size > 10) {
						add(DownloadOption.FirstChapters(branchChapters.take(10).ids()))
					}
				}
			}
			add(DownloadOption.SelectionHint())
		}
		var dialog: DialogInterface? = null
		val listener = OnListItemClickListener<DownloadOption> { item, _ ->
			callback.onItemClick(item, host)
			dialog?.dismiss()
		}
		dialog = RecyclerViewAlertDialog.Builder<DownloadOption>(host.context)
			.addAdapterDelegate(downloadOptionAD(listener))
			.setCancelable(true)
			.setTitle(R.string.download)
			.setNegativeButton(android.R.string.cancel)
			.setNeutralButton(R.string.settings) { _, _ ->
				host.context.startActivity(SettingsActivity.newDownloadsSettingsIntent(host.context))
			}
			.setItems(options)
			.create()
			.also { it.show() }
	}
}
