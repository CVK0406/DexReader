package com.example.dexreader.reader.ui.pager

import android.os.Bundle
import androidx.core.graphics.Insets
import androidx.fragment.app.activityViewModels
import androidx.viewbinding.ViewBinding
import com.example.dexreader.core.prefs.ReaderAnimation
import com.example.dexreader.core.ui.BaseFragment
import com.example.dexreader.reader.ui.ReaderState
import com.example.dexreader.reader.ui.ReaderViewModel
import com.example.dexreader.core.ui.widgets.ZoomControl
import com.example.dexreader.core.util.ext.getParcelableCompat
import com.example.dexreader.core.util.ext.isAnimationsEnabled
import com.example.dexreader.core.util.ext.observe

private const val KEY_STATE = "state"

abstract class BaseReaderFragment<B : ViewBinding> : BaseFragment<B>(), ZoomControl.ZoomControlListener {

	protected val viewModel by activityViewModels<ReaderViewModel>()
	private var stateToSave: ReaderState? = null

	protected var readerAdapter: BaseReaderAdapter<*>? = null
		private set

	override fun onViewBindingCreated(binding: B, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		var restoredState = savedInstanceState?.getParcelableCompat<ReaderState>(KEY_STATE)
		readerAdapter = onCreateAdapter()

		viewModel.content.observe(viewLifecycleOwner) {
			var pendingState = restoredState ?: it.state
			if (pendingState == null && it.pages.isNotEmpty() && readerAdapter?.hasItems != true) {
				pendingState = viewModel.getCurrentState()
			}
			onPagesChanged(it.pages, pendingState)
			restoredState = null
		}
	}

	override fun onPause() {
		super.onPause()
		viewModel.saveCurrentState(getCurrentState())
	}

	override fun onDestroyView() {
		stateToSave = getCurrentState()
		readerAdapter = null
		super.onDestroyView()
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		getCurrentState()?.let {
			stateToSave = it
		}
		outState.putParcelable(KEY_STATE, stateToSave)
	}

	protected fun requireAdapter() = checkNotNull(readerAdapter) {
		"Adapter was not created or already destroyed"
	}

	protected fun isAnimationEnabled(): Boolean {
		return context?.isAnimationsEnabled == true && viewModel.pageAnimation.value != ReaderAnimation.NONE
	}

	override fun onWindowInsetsChanged(insets: Insets) = Unit

	abstract fun switchPageBy(delta: Int)

	abstract fun switchPageTo(position: Int, smooth: Boolean)

	open fun scrollBy(delta: Int, smooth: Boolean): Boolean = false

	abstract fun getCurrentState(): ReaderState?

	protected abstract fun onCreateAdapter(): BaseReaderAdapter<*>

	protected abstract suspend fun onPagesChanged(pages: List<ReaderPage>, pendingState: ReaderState?)
}
