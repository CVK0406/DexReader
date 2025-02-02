package com.example.dexreader.widget.recent

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.executeBlocking
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.RoundedCornersTransformation
import com.example.dexreader.R
import com.example.dexreader.core.parser.MangaIntent
import com.example.dexreader.core.util.ext.getDrawableOrThrow
import com.example.dexreader.history.data.HistoryRepository
import kotlinx.coroutines.runBlocking
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.util.replaceWith

class RecentListFactory(
	private val context: Context,
	private val historyRepository: HistoryRepository,
	private val coil: ImageLoader,
) : RemoteViewsService.RemoteViewsFactory {

	private val dataSet = ArrayList<Manga>()
	private val transformation = RoundedCornersTransformation(
		context.resources.getDimension(R.dimen.appwidget_corner_radius_inner),
	)
	private val coverSize = Size(
		context.resources.getDimensionPixelSize(R.dimen.widget_cover_width),
		context.resources.getDimensionPixelSize(R.dimen.widget_cover_height),
	)

	override fun onCreate() = Unit

	override fun getLoadingView() = null

	override fun getItemId(position: Int) = dataSet.getOrNull(position)?.id ?: 0L

	override fun onDataSetChanged() {
		val data = runBlocking { historyRepository.getList(0, 10) }
		dataSet.replaceWith(data)
	}

	override fun hasStableIds() = true

	override fun getViewAt(position: Int): RemoteViews {
		val views = RemoteViews(context.packageName, R.layout.item_recent)
		val item = dataSet.getOrNull(position) ?: return views
		runCatching {
			coil.executeBlocking(
				ImageRequest.Builder(context)
					.data(item.coverUrl)
					.size(coverSize)
					.tag(item.source)
					.tag(item)
					.transformations(transformation)
					.build(),
			).getDrawableOrThrow().toBitmap()
		}.onSuccess { cover ->
			views.setImageViewBitmap(R.id.imageView_cover, cover)
		}.onFailure {
			views.setImageViewResource(R.id.imageView_cover, R.drawable.ic_placeholder)
		}
		val intent = Intent()
		intent.putExtra(MangaIntent.KEY_ID, item.id)
		views.setOnClickFillInIntent(R.id.imageView_cover, intent)
		return views
	}

	override fun getCount() = dataSet.size

	override fun getViewTypeCount() = 1

	override fun onDestroy() = Unit
}
