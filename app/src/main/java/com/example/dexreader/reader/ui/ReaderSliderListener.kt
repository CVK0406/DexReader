package com.example.dexreader.reader.ui

import com.example.dexreader.reader.ui.pager.ReaderPage
import com.example.dexreader.reader.ui.thumbnails.OnPageSelectListener
import com.google.android.material.slider.Slider

class ReaderSliderListener(
	private val pageSelectListener: OnPageSelectListener,
	private val viewModel: ReaderViewModel,
) : Slider.OnChangeListener, Slider.OnSliderTouchListener {

	private var isChanged = false
	private var isTracking = false

	override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
		if (fromUser) {
			if (isTracking) {
				isChanged = true
			} else {
				switchPageToIndex(value.toInt())
			}
		}
	}

	override fun onStartTrackingTouch(slider: Slider) {
		isChanged = false
		isTracking = true
	}

	override fun onStopTrackingTouch(slider: Slider) {
		isTracking = false
		if (isChanged) {
			switchPageToIndex(slider.value.toInt())
		}
	}

	fun attachToSlider(slider: Slider) {
		slider.addOnChangeListener(this)
		slider.addOnSliderTouchListener(this)
	}

	private fun switchPageToIndex(index: Int) {
		val pages = viewModel.getCurrentChapterPages()
		val page = pages?.getOrNull(index) ?: return
		val chapterId = viewModel.getCurrentState()?.chapterId ?: return
		pageSelectListener.onPageSelected(ReaderPage(page, index, chapterId))
	}
}
