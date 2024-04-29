package com.example.dexreader.reader.ui.pager.standard

import android.annotation.SuppressLint
import android.graphics.PointF
import android.net.Uri
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.example.dexreader.R
import com.example.dexreader.core.exceptions.resolve.ExceptionResolver
import com.example.dexreader.core.model.ZoomMode
import com.example.dexreader.core.os.NetworkState
import com.example.dexreader.core.ui.widgets.ZoomControl
import com.example.dexreader.core.util.ext.getDisplayMessage
import com.example.dexreader.core.util.ext.ifZero
import com.example.dexreader.core.util.ext.isLowRamDevice
import com.example.dexreader.databinding.ItemPageBinding
import com.example.dexreader.reader.domain.PageLoader
import com.example.dexreader.reader.ui.config.ReaderSettings
import com.example.dexreader.reader.ui.pager.BasePageHolder
import com.example.dexreader.reader.ui.pager.ReaderPage

open class PageHolder(
	owner: LifecycleOwner,
	binding: ItemPageBinding,
	loader: PageLoader,
	settings: ReaderSettings,
	networkState: NetworkState,
	exceptionResolver: ExceptionResolver,
) : BasePageHolder<ItemPageBinding>(binding, loader, settings, networkState, exceptionResolver, owner),
	View.OnClickListener,
	ZoomControl.ZoomControlListener {

	init {
		binding.ssiv.bindToLifecycle(owner)
		binding.ssiv.isEagerLoadingEnabled = !context.isLowRamDevice()
		binding.ssiv.addOnImageEventListener(delegate)
		@Suppress("LeakingThis")
		bindingInfo.buttonRetry.setOnClickListener(this)
		@Suppress("LeakingThis")
		bindingInfo.buttonErrorDetails.setOnClickListener(this)
	}

	override fun onResume() {
		super.onResume()
		binding.ssiv.applyDownsampling(isForeground = true)
	}

	override fun onPause() {
		super.onPause()
		binding.ssiv.applyDownsampling(isForeground = false)
	}

	override fun onConfigChanged() {
		super.onConfigChanged()
		if (settings.applyBitmapConfig(binding.ssiv)) {
			delegate.reload()
		}
		binding.ssiv.applyDownsampling(isResumed())
		binding.textViewNumber.isVisible = settings.isPagesNumbersEnabled
	}

	@SuppressLint("SetTextI18n")
	override fun onBind(data: ReaderPage) {
		delegate.onBind(data.toMangaPage())
		binding.textViewNumber.text = (data.index + 1).toString()
	}

	override fun onRecycled() {
		super.onRecycled()
		binding.ssiv.recycle()
	}

	override fun onLoadingStarted() {
		bindingInfo.layoutError.isVisible = false
		bindingInfo.progressBar.show()
		binding.ssiv.recycle()
	}

	override fun onProgressChanged(progress: Int) {
		if (progress in 0..100) {
			bindingInfo.progressBar.isIndeterminate = false
			bindingInfo.progressBar.setProgressCompat(progress, true)
		} else {
			bindingInfo.progressBar.isIndeterminate = true
		}
	}

	override fun onImageReady(uri: Uri) {
		binding.ssiv.setImage(ImageSource.Uri(uri))
	}

	override fun onImageShowing(settings: ReaderSettings) {
		binding.ssiv.maxScale = 2f * maxOf(
			binding.ssiv.width / binding.ssiv.sWidth.toFloat(),
			binding.ssiv.height / binding.ssiv.sHeight.toFloat(),
		)
		binding.ssiv.colorFilter = settings.colorFilter?.toColorFilter()
		when (settings.zoomMode) {
			ZoomMode.FIT_CENTER -> {
				binding.ssiv.minimumScaleType = SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE
				binding.ssiv.resetScaleAndCenter()
			}

			ZoomMode.FIT_HEIGHT -> {
				binding.ssiv.minimumScaleType = SubsamplingScaleImageView.SCALE_TYPE_CUSTOM
				binding.ssiv.minScale = binding.ssiv.height / binding.ssiv.sHeight.toFloat()
				binding.ssiv.setScaleAndCenter(
					binding.ssiv.minScale,
					PointF(0f, binding.ssiv.sHeight / 2f),
				)
			}

			ZoomMode.FIT_WIDTH -> {
				binding.ssiv.minimumScaleType = SubsamplingScaleImageView.SCALE_TYPE_CUSTOM
				binding.ssiv.minScale = binding.ssiv.width / binding.ssiv.sWidth.toFloat()
				binding.ssiv.setScaleAndCenter(
					binding.ssiv.minScale,
					PointF(binding.ssiv.sWidth / 2f, 0f),
				)
			}

			ZoomMode.KEEP_START -> {
				binding.ssiv.minimumScaleType = SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE
				binding.ssiv.setScaleAndCenter(
					binding.ssiv.maxScale,
					PointF(0f, 0f),
				)
			}
		}
	}

	override fun onImageShown() {
		bindingInfo.progressBar.hide()
	}

	final override fun onClick(v: View) {
		when (v.id) {
			R.id.button_retry -> delegate.retry(boundData?.toMangaPage() ?: return, isFromUser = true)
		}
	}

	override fun onError(e: Throwable) {
		bindingInfo.textViewError.text = e.getDisplayMessage(context.resources)
		bindingInfo.buttonRetry.setText(
			ExceptionResolver.getResolveStringId(e).ifZero { R.string.try_again },
		)
		bindingInfo.layoutError.isVisible = true
		bindingInfo.progressBar.hide()
	}

	override fun onZoomIn() {
		scaleBy(1.2f)
	}

	override fun onZoomOut() {
		scaleBy(0.8f)
	}

	private fun scaleBy(factor: Float) {
		val ssiv = binding.ssiv
		val center = ssiv.getCenter() ?: return
		val newScale = ssiv.scale * factor
		ssiv.animateScaleAndCenter(newScale, center)?.apply {
			withDuration(ssiv.resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
			withInterpolator(DecelerateInterpolator())
			start()
		}
	}
}
