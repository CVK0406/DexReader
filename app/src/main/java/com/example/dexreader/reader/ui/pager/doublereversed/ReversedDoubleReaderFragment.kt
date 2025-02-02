package com.example.dexreader.reader.ui.pager.doublereversed

import com.example.dexreader.reader.ui.pager.ReaderPage
import com.example.dexreader.reader.ui.pager.doublepage.DoubleReaderFragment
import com.example.dexreader.reader.ui.ReaderState

class ReversedDoubleReaderFragment : DoubleReaderFragment() {

	override fun switchPageBy(delta: Int) {
		super.switchPageBy(-delta)
	}

	override fun switchPageTo(position: Int, smooth: Boolean) {
		super.switchPageTo(reversed(position), smooth)
	}

	override suspend fun onPagesChanged(pages: List<ReaderPage>, pendingState: ReaderState?) {
		super.onPagesChanged(pages.reversed(), pendingState)
	}

	override fun notifyPageChanged(lowerPos: Int, upperPos: Int) {
		viewModel.onCurrentPageChanged(reversed(upperPos), reversed(lowerPos))
	}

	private fun reversed(position: Int): Int {
		return ((readerAdapter?.itemCount ?: 0) - position - 1).coerceAtLeast(0)
	}
}
