package com.example.dexreader.favourites.ui.categories

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.example.dexreader.R
import com.example.dexreader.core.ui.list.decor.AbstractSelectionItemDecoration
import com.example.dexreader.core.util.ext.getItem
import com.example.dexreader.core.util.ext.getThemeColor
import com.example.dexreader.favourites.ui.categories.adapter.CategoryListModel
import com.google.android.material.R as materialR

class CategoriesSelectionDecoration(context: Context) : AbstractSelectionItemDecoration() {

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val radius = context.resources.getDimension(R.dimen.list_selector_corner)
	private val strokeColor = context.getThemeColor(materialR.attr.colorPrimary, Color.RED)
	private val fillColor = ColorUtils.setAlphaComponent(
		ColorUtils.blendARGB(strokeColor, context.getThemeColor(materialR.attr.colorSurface), 0.8f),
		0x74,
	)
	private val padding = context.resources.getDimension(R.dimen.grid_spacing_outer)

	init {
		paint.strokeWidth = context.resources.getDimension(R.dimen.selection_stroke_width)
		hasForeground = true
		hasBackground = false
		isIncludeDecorAndMargins = false
	}

	override fun getItemId(parent: RecyclerView, child: View): Long {
		val holder = parent.getChildViewHolder(child) ?: return RecyclerView.NO_ID
		val item = holder.getItem(CategoryListModel::class.java) ?: return RecyclerView.NO_ID
		return item.category.id
	}

	override fun onDrawForeground(
		canvas: Canvas,
		parent: RecyclerView,
		child: View,
		bounds: RectF,
		state: RecyclerView.State,
	) {
		bounds.inset(padding, padding)
		paint.color = fillColor
		paint.style = Paint.Style.FILL
		canvas.drawRoundRect(bounds, radius, radius, paint)
		paint.color = strokeColor
		paint.style = Paint.Style.STROKE
		canvas.drawRoundRect(bounds, radius, radius, paint)
	}
}
