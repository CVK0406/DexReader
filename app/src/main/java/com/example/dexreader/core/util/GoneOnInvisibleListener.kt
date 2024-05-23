package com.example.dexreader.core.util

import android.view.View
import android.view.ViewTreeObserver

class GoneOnInvisibleListener(
	private val view: View,
) : ViewTreeObserver.OnGlobalLayoutListener {

	override fun onGlobalLayout() {
		if (view.visibility == View.INVISIBLE) {
			view.visibility = View.GONE
		}
	}

	fun attach() {
		view.viewTreeObserver.addOnGlobalLayoutListener(this)
		onGlobalLayout()
	}

	fun detach() {
		view.viewTreeObserver.removeOnGlobalLayoutListener(this)
	}
}
