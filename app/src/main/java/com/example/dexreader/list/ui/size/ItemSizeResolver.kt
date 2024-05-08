package com.example.dexreader.list.ui.size

import android.view.View
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import com.example.dexreader.history.ui.util.ReadingProgressView

interface ItemSizeResolver {

	val cellWidth: Int

	fun attachToView(
		lifecycleOwner: LifecycleOwner,
		view: View,
		textView: TextView?,
		progressView: ReadingProgressView?,
	)
}
