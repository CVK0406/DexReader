package com.example.dexreader.reader.ui.pager

import android.os.Build
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.PageTransformer
import com.example.dexreader.R
import com.example.dexreader.core.os.NetworkState
import com.example.dexreader.core.prefs.ReaderAnimation
import com.example.dexreader.core.ui.list.lifecycle.PagerLifecycleDispatcher
import com.example.dexreader.core.util.ext.doOnPageChanged
import com.example.dexreader.core.util.ext.findCurrentViewHolder
import com.example.dexreader.core.util.ext.observe
import com.example.dexreader.core.util.ext.recyclerView
import com.example.dexreader.core.util.ext.resetTransformations
import com.example.dexreader.databinding.FragmentReaderPagerBinding
import com.example.dexreader.reader.domain.PageLoader
import com.example.dexreader.reader.ui.pager.standard.NoAnimPageTransformer
import com.example.dexreader.reader.ui.pager.standard.PageAnimTransformer
import com.example.dexreader.reader.ui.pager.standard.PageHolder
import com.example.dexreader.reader.ui.pager.standard.PagerEventSupplier
import com.example.dexreader.reader.ui.pager.standard.PagesAdapter
import com.google.android.material.snackbar.Snackbar
import com.example.dexreader.reader.ui.ReaderState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import javax.inject.Inject
import kotlin.math.absoluteValue
import kotlin.math.sign

@AndroidEntryPoint
abstract class BasePagerReaderFragment : BaseReaderFragment<FragmentReaderPagerBinding>(),
	View.OnGenericMotionListener {

	@Inject
	lateinit var networkState: NetworkState

	@Inject
	lateinit var pageLoader: PageLoader

	private var pagerLifecycleDispatcher: PagerLifecycleDispatcher? = null

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = FragmentReaderPagerBinding.inflate(inflater, container, false)

	override fun onViewBindingCreated(
		binding: FragmentReaderPagerBinding,
		savedInstanceState: Bundle?,
	) {
		super.onViewBindingCreated(binding, savedInstanceState)
		with(binding.pager) {
			onInitPager(this)
			doOnPageChanged(::notifyPageChanged)
			setOnGenericMotionListener(this@BasePagerReaderFragment)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				recyclerView?.defaultFocusHighlightEnabled = false
			}
			PagerEventSupplier(this).attach()
			pagerLifecycleDispatcher = PagerLifecycleDispatcher(this).also {
				registerOnPageChangeCallback(it)
			}
			adapter = readerAdapter
		}

		viewModel.pageAnimation.observe(viewLifecycleOwner) {
			val transformer = when (it) {
				ReaderAnimation.NONE -> NoAnimPageTransformer(binding.pager.orientation)
				ReaderAnimation.DEFAULT -> null
				ReaderAnimation.ADVANCED -> onCreateAdvancedTransformer()
			}
			binding.pager.setPageTransformer(transformer)
			if (transformer == null) {
				binding.pager.recyclerView?.children?.forEach { view ->
					view.resetTransformations()
				}
			}
		}
	}

	override fun onDestroyView() {
		pagerLifecycleDispatcher = null
		requireViewBinding().pager.adapter = null
		super.onDestroyView()
	}

	override fun onZoomIn() {
		(viewBinding?.pager?.findCurrentViewHolder() as? PageHolder)?.onZoomIn()
	}

	override fun onZoomOut() {
		(viewBinding?.pager?.findCurrentViewHolder() as? PageHolder)?.onZoomOut()
	}

	override fun onGenericMotion(v: View?, event: MotionEvent): Boolean {
		if (event.source and InputDevice.SOURCE_CLASS_POINTER != 0) {
			if (event.actionMasked == MotionEvent.ACTION_SCROLL) {
				val axisValue = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
				val withCtrl = event.metaState and KeyEvent.META_CTRL_MASK != 0
				if (!withCtrl) {
					onWheelScroll(axisValue)
					return true
				}
			}
		}
		return false
	}

	override suspend fun onPagesChanged(pages: List<ReaderPage>, pendingState: ReaderState?) = coroutineScope {
		val items = launch {
			requireAdapter().setItems(pages)
			yield()
			pagerLifecycleDispatcher?.invalidate()
		}
		if (pendingState != null) {
			val position = pages.indexOfFirst {
				it.chapterId == pendingState.chapterId && it.index == pendingState.page
			}
			items.join()
			if (position != -1) {
				requireViewBinding().pager.setCurrentItem(position, false)
				notifyPageChanged(position)
			} else {
				Snackbar.make(requireView(), R.string.not_found_404, Snackbar.LENGTH_SHORT)
					.show()
			}
		} else {
			items.join()
		}
	}

	override fun onCreateAdapter(): BaseReaderAdapter<*> = PagesAdapter(
		lifecycleOwner = viewLifecycleOwner,
		loader = pageLoader,
		settings = viewModel.readerSettings,
		networkState = networkState,
		exceptionResolver = exceptionResolver,
	)

	override fun switchPageBy(delta: Int) {
		with(requireViewBinding().pager) {
			setCurrentItem(currentItem + delta, isAnimationEnabled())
		}
	}

	override fun switchPageTo(position: Int, smooth: Boolean) {
		with(requireViewBinding().pager) {
			setCurrentItem(
				position,
				smooth && isAnimationEnabled() && (currentItem - position).absoluteValue < SMOOTH_SCROLL_LIMIT,
			)
		}
	}

	override fun getCurrentState(): ReaderState? = viewBinding?.run {
		val adapter = pager.adapter as? BaseReaderAdapter<*>
		val page = adapter?.getItemOrNull(pager.currentItem) ?: return@run null
		ReaderState(
			chapterId = page.chapterId,
			page = page.index,
			scroll = 0,
		)
	}

	protected open fun onWheelScroll(axisValue: Float) {
		switchPageBy(-axisValue.sign.toInt())
	}

	protected open fun onCreateAdvancedTransformer(): PageTransformer = PageAnimTransformer()

	protected open fun onInitPager(pager: ViewPager2) {
		pager.offscreenPageLimit = 2
	}

	protected open fun notifyPageChanged(page: Int) {
		viewModel.onCurrentPageChanged(page, page)
	}

	companion object {

		const val SMOOTH_SCROLL_LIMIT = 3
	}
}
