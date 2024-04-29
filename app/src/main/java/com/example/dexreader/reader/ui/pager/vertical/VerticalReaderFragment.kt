package com.example.dexreader.reader.ui.pager.vertical

import androidx.viewpager2.widget.ViewPager2
import com.example.dexreader.reader.ui.pager.BasePagerReaderFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VerticalReaderFragment : BasePagerReaderFragment() {

	override fun onInitPager(pager: ViewPager2) {
		super.onInitPager(pager)
		pager.orientation = ViewPager2.ORIENTATION_VERTICAL
	}

	override fun onCreateAdvancedTransformer(): ViewPager2.PageTransformer = VerticalPageAnimTransformer()
}
