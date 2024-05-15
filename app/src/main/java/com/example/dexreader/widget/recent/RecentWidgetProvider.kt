package com.example.dexreader.widget.recent

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.widget.RemoteViews
import androidx.core.app.PendingIntentCompat
import com.example.dexreader.R
import com.example.dexreader.core.prefs.AppWidgetConfig
import com.example.dexreader.core.ui.BaseAppWidgetProvider
import com.example.dexreader.reader.ui.ReaderActivity

class RecentWidgetProvider : BaseAppWidgetProvider() {

	override fun onUpdate(
		context: Context,
		appWidgetManager: AppWidgetManager,
		appWidgetIds: IntArray
	) {
		super.onUpdate(context, appWidgetManager, appWidgetIds)
		appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.stackView)
	}

	override fun onUpdateWidget(context: Context, config: AppWidgetConfig): RemoteViews {
		val views = RemoteViews(context.packageName, R.layout.widget_recent)
		if (!config.hasBackground) {
			views.setInt(R.id.widget_root, "setBackgroundColor", Color.TRANSPARENT)
		} else {
			views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.bg_appwidget_root)
		}
		val adapter = Intent(context, RecentWidgetService::class.java)
		adapter.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, config.widgetId)
		adapter.data = Uri.parse(adapter.toUri(Intent.URI_INTENT_SCHEME))
		views.setRemoteAdapter(R.id.stackView, adapter)
		val intent = Intent(context, ReaderActivity::class.java)
		intent.action = ReaderActivity.ACTION_MANGA_READ
		views.setPendingIntentTemplate(
			R.id.stackView,
			PendingIntentCompat.getActivity(
				context,
				0,
				intent,
				PendingIntent.FLAG_UPDATE_CURRENT,
				true,
			),
		)
		views.setEmptyView(R.id.stackView, R.id.textView_holder)
		return views
	}
}
