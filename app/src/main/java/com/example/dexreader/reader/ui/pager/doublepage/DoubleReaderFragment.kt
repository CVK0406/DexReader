package com.example.dexreader.reader.ui.pager.doublepage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dexreader.R
import com.example.dexreader.core.os.NetworkState
import com.example.dexreader.core.ui.list.lifecycle.RecyclerViewLifecycleDispatcher
import com.example.dexreader.core.util.ext.firstVisibleItemPosition
import com.example.dexreader.databinding.FragmentReaderDoubleBinding
import com.example.dexreader.reader.domain.PageLoader
import com.example.dexreader.reader.ui.pager.BaseReaderAdapter
import com.example.dexreader.reader.ui.pager.BaseReaderFragment
import com.example.dexreader.reader.ui.pager.ReaderPage
import com.example.dexreader.reader.ui.ReaderState
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import javax.inject.Inject
import kotlin.math.absoluteValue

@AndroidEntryPoint
open class DoubleReaderFragment : BaseReaderFragment<FragmentReaderDoubleBinding>() {

	@Inject
	lateinit var networkState: NetworkState

	@Inject
	lateinit var pageLoader: PageLoader

	private var recyclerLifecycleDispatcher: RecyclerViewLifecycleDispatcher? = null

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = FragmentReaderDoubleBinding.inflate(inflater, container, false)

	override fun onViewBindingCreated(
		binding: FragmentReaderDoubleBinding,
		savedInstanceState: Bundle?,
	) {
		super.onViewBindingCreated(binding, savedInstanceState)
		with(binding.recyclerView) {
			adapter = readerAdapter
			recyclerLifecycleDispatcher = RecyclerViewLifecycleDispatcher().also {
				addOnScrollListener(it)
			}
			addOnScrollListener(PageScrollListener())
			DoublePageSnapHelper().attachToRecyclerView(this)
		}
	}

	override fun onDestroyView() {
		recyclerLifecycleDispatcher = null
		requireViewBinding().recyclerView.adapter = null
		super.onDestroyView()
	}

	override suspend fun onPagesChanged(pages: List<ReaderPage>, pendingState: ReaderState?) = coroutineScope {
		val items = launch {
			requireAdapter().setItems(pages)
			yield()
			viewBinding?.recyclerView?.let { rv ->
				recyclerLifecycleDispatcher?.invalidate(rv)
			}
		}
		if (pendingState != null) {
			var position = pages.indexOfFirst {
				it.chapterId == pendingState.chapterId && it.index == pendingState.page
			}
			items.join()
			if (position != -1) {
				position = position.toPagePosition()
				requireViewBinding().recyclerView.firstVisibleItemPosition = position
				notifyPageChanged(position, position + 1)
			} else {
				Snackbar.make(requireView(), R.string.not_found_404, Snackbar.LENGTH_SHORT)
					.show()
			}
		} else {
			items.join()
		}
	}

	override fun onCreateAdapter() = DoublePagesAdapter(
		lifecycleOwner = viewLifecycleOwner,
		loader = pageLoader,
		settings = viewModel.readerSettings,
		networkState = networkState,
		exceptionResolver = exceptionResolver,
	)

	override fun onZoomIn() {
		(viewBinding ?: return).recyclerView.visiblePageHolders()
			.forEach { it.onZoomIn() }
	}

	override fun onZoomOut() {
		(viewBinding ?: return).recyclerView.visiblePageHolders()
			.forEach { it.onZoomOut() }
	}

	override fun switchPageBy(delta: Int) {
		if (delta.absoluteValue > 1 || !isAnimationEnabled()) {
			switchPageTo(getCurrentItem() + delta + delta, false)
			return
		}
		val rv = viewBinding?.recyclerView ?: return
		val distance = rv.width * delta
		rv.smoothScrollBy(distance, 0, AccelerateDecelerateInterpolator())
	}

	override fun switchPageTo(position: Int, smooth: Boolean) {
		val lm = viewBinding?.recyclerView?.layoutManager as? LinearLayoutManager ?: return
		val targetPosition = position.toPagePosition()
		lm.scrollToPositionWithOffset(targetPosition, 0)
	}

	override fun getCurrentState(): ReaderState? = viewBinding?.run {
		val adapter = recyclerView.adapter as? BaseReaderAdapter<*>
		val page = adapter?.getItemOrNull(getCurrentItem()) ?: return@run null
		ReaderState(
			chapterId = page.chapterId,
			page = page.index,
			scroll = 0,
		)
	}

	protected open fun notifyPageChanged(lowerPos: Int, upperPos: Int) {
		viewModel.onCurrentPageChanged(lowerPos, upperPos)
	}

	private fun getCurrentItem() = (requireViewBinding().recyclerView.layoutManager as LinearLayoutManager)
		.findFirstCompletelyVisibleItemPosition().toPagePosition()

	private fun Int.toPagePosition() = this and 1.inv()

	private inner class PageScrollListener : RecyclerView.OnScrollListener() {

		private var firstPos = RecyclerView.NO_POSITION
		private var lastPos = RecyclerView.NO_POSITION

		override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
			super.onScrolled(recyclerView, dx, dy)
			val lm = recyclerView.layoutManager as? LinearLayoutManager
			if (lm == null) {
				firstPos = RecyclerView.NO_POSITION
				lastPos = RecyclerView.NO_POSITION
				return
			}
			val newFirstPos = lm.findFirstVisibleItemPosition()
			val newLastPos = lm.findLastVisibleItemPosition()
			if (newFirstPos != firstPos || newLastPos != lastPos) {
				firstPos = newFirstPos
				lastPos = newLastPos
				notifyPageChanged(newFirstPos, newLastPos)
			}
		}
	}
}
