package com.example.dexreader.download.ui.worker

import android.view.View
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.FlowCollector
import com.example.dexreader.R
import com.example.dexreader.core.util.ext.findActivity
import com.example.dexreader.download.ui.list.DownloadsActivity
import com.example.dexreader.main.ui.owners.BottomNavOwner

class DownloadStartedObserver(
	private val snackbarHost: View,
) : FlowCollector<Unit> {

	override suspend fun emit(value: Unit) {
		val snackbar = Snackbar.make(snackbarHost, R.string.download_started, Snackbar.LENGTH_LONG)
		(snackbarHost.context.findActivity() as? BottomNavOwner)?.let {
			snackbar.anchorView = it.bottomNav
		}
		snackbar.setAction(R.string.details) {
			it.context.startActivity(DownloadsActivity.newIntent(it.context))
		}
		snackbar.show()
	}
}
