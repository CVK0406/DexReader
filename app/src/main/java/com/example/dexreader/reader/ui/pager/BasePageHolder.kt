package com.example.dexreader.reader.ui.pager

import android.content.Context
import androidx.annotation.CallSuper
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.example.dexreader.core.exceptions.resolve.ExceptionResolver
import com.example.dexreader.core.os.NetworkState
import com.example.dexreader.core.ui.list.lifecycle.LifecycleAwareViewHolder
import com.example.dexreader.core.util.ext.isLowRamDevice
import com.example.dexreader.databinding.LayoutPageInfoBinding
import com.example.dexreader.reader.domain.PageLoader
import com.example.dexreader.reader.ui.config.ReaderSettings
import com.example.dexreader.reader.ui.pager.PageHolderDelegate.State

abstract class BasePageHolder<B : ViewBinding>(
	protected val binding: B,
	loader: PageLoader,
	protected val settings: ReaderSettings,
	networkState: NetworkState,
	exceptionResolver: ExceptionResolver,
	lifecycleOwner: LifecycleOwner,
) : LifecycleAwareViewHolder(binding.root, lifecycleOwner), PageHolderDelegate.Callback {

	@Suppress("LeakingThis")
	protected val delegate = PageHolderDelegate(loader, settings, this, networkState, exceptionResolver)
	protected val bindingInfo = LayoutPageInfoBinding.bind(binding.root)

	val context: Context
		get() = itemView.context

	var boundData: ReaderPage? = null
		private set

	override fun onConfigChanged() {
		settings.applyBackground(itemView)
	}

	fun requireData(): ReaderPage {
		return checkNotNull(boundData) { "Calling requireData() before bind()" }
	}

	fun bind(data: ReaderPage) {
		boundData = data
		onBind(data)
	}

	protected abstract fun onBind(data: ReaderPage)

	override fun onResume() {
		super.onResume()
		if (delegate.state == State.ERROR && !delegate.isLoading()) {
			boundData?.let { delegate.retry(it.toMangaPage(), isFromUser = false) }
		}
	}

	@CallSuper
	open fun onAttachedToWindow() {
		delegate.onAttachedToWindow()
	}

	@CallSuper
	open fun onDetachedFromWindow() {
		delegate.onDetachedFromWindow()
	}

	@CallSuper
	open fun onRecycled() {
		delegate.onRecycle()
	}

	protected fun SubsamplingScaleImageView.applyDownsampling(isForeground: Boolean) {
		downsampling = when {
			isForeground || !settings.isReaderOptimizationEnabled -> 1
			context.isLowRamDevice() -> 8
			else -> 4
		}
	}
}
