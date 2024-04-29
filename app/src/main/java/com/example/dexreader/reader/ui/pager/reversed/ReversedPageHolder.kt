package com.example.dexreader.reader.ui.pager.reversed

import android.graphics.PointF
import android.view.Gravity
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.example.dexreader.core.exceptions.resolve.ExceptionResolver
import com.example.dexreader.core.model.ZoomMode
import com.example.dexreader.core.os.NetworkState
import com.example.dexreader.databinding.ItemPageBinding
import com.example.dexreader.reader.domain.PageLoader
import com.example.dexreader.reader.ui.config.ReaderSettings
import com.example.dexreader.reader.ui.pager.standard.PageHolder

class ReversedPageHolder(
	owner: LifecycleOwner,
	binding: ItemPageBinding,
	loader: PageLoader,
	settings: ReaderSettings,
	networkState: NetworkState,
	exceptionResolver: ExceptionResolver,
) : PageHolder(owner, binding, loader, settings, networkState, exceptionResolver) {

	init {
		(binding.textViewNumber.layoutParams as FrameLayout.LayoutParams)
			.gravity = Gravity.START or Gravity.BOTTOM
	}

	override fun onImageShowing(settings: ReaderSettings) {
		with(binding.ssiv) {
			maxScale = 2f * maxOf(
				width / sWidth.toFloat(),
				height / sHeight.toFloat(),
			)
			binding.ssiv.colorFilter = settings.colorFilter?.toColorFilter()
			when (settings.zoomMode) {
				ZoomMode.FIT_CENTER -> {
					minimumScaleType = SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE
					resetScaleAndCenter()
				}

				ZoomMode.FIT_HEIGHT -> {
					minimumScaleType = SubsamplingScaleImageView.SCALE_TYPE_CUSTOM
					minScale = height / sHeight.toFloat()
					setScaleAndCenter(
						minScale,
						PointF(sWidth.toFloat(), sHeight / 2f),
					)
				}

				ZoomMode.FIT_WIDTH -> {
					minimumScaleType = SubsamplingScaleImageView.SCALE_TYPE_CUSTOM
					minScale = width / sWidth.toFloat()
					setScaleAndCenter(
						minScale,
						PointF(sWidth / 2f, 0f),
					)
				}

				ZoomMode.KEEP_START -> {
					minimumScaleType = SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE
					setScaleAndCenter(
						maxScale,
						PointF(sWidth.toFloat(), 0f),
					)
				}
			}
		}
	}
}
