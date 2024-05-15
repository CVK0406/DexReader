package com.example.dexreader.widget.shelf

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.widget.RemoteViewsService
import coil.ImageLoader
import com.example.dexreader.favourites.domain.FavouritesRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ShelfWidgetService : RemoteViewsService() {

	@Inject
	lateinit var favouritesRepository: FavouritesRepository

	@Inject
	lateinit var coil: ImageLoader

	override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
		val widgetId = intent.getIntExtra(
			AppWidgetManager.EXTRA_APPWIDGET_ID,
			AppWidgetManager.INVALID_APPWIDGET_ID,
		)
		return ShelfListFactory(applicationContext, favouritesRepository, coil, widgetId)
	}
}
