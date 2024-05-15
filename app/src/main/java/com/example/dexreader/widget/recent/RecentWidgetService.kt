package com.example.dexreader.widget.recent

import android.content.Intent
import android.widget.RemoteViewsService
import coil.ImageLoader
import com.example.dexreader.history.data.HistoryRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RecentWidgetService : RemoteViewsService() {

	@Inject
	lateinit var historyRepository: HistoryRepository

	@Inject
	lateinit var coil: ImageLoader

	override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
		return RecentListFactory(applicationContext, historyRepository, coil)
	}
}
