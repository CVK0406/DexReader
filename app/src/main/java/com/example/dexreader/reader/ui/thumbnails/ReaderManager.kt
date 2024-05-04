package com.example.dexreader.reader.ui

import android.content.res.Configuration
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.prefs.ReaderMode
import com.example.dexreader.core.util.ext.findKeyByValue
import com.example.dexreader.reader.ui.pager.BaseReaderFragment
import com.example.dexreader.reader.ui.pager.doublepage.DoubleReaderFragment
import com.example.dexreader.reader.ui.pager.doublereversed.ReversedDoubleReaderFragment
import com.example.dexreader.reader.ui.pager.reversed.ReversedReaderFragment
import com.example.dexreader.reader.ui.pager.standard.PagerReaderFragment
import com.example.dexreader.reader.ui.pager.vertical.VerticalReaderFragment
import com.example.dexreader.reader.ui.pager.webtoon.WebtoonReaderFragment
import java.util.EnumMap

class ReaderManager(
	private val fragmentManager: FragmentManager,
	private val container: FragmentContainerView,
	private val settings: AppSettings,
) {

	private val modeMap = EnumMap<ReaderMode, Class<out BaseReaderFragment<*>>>(ReaderMode::class.java)

	init {
		val useDoublePages = isLandscape() && settings.isReaderDoubleOnLandscape
		invalidateTypesMap(useDoublePages)
	}

	val currentReader: BaseReaderFragment<*>?
		get() = fragmentManager.findFragmentById(container.id) as? BaseReaderFragment<*>

	val currentMode: ReaderMode?
		get() {
			val readerClass = currentReader?.javaClass ?: return null
			return modeMap.findKeyByValue(readerClass)
		}

	fun replace(newMode: ReaderMode) {
		val readerClass = requireNotNull(modeMap[newMode])
		fragmentManager.commit {
			setReorderingAllowed(true)
			replace(container.id, readerClass, null, null)
		}
	}

	fun setDoubleReaderMode(isEnabled: Boolean) {
		val prevMode = currentMode
		invalidateTypesMap(isEnabled && isLandscape())
		val newMode = currentMode ?: return
		if (newMode != prevMode) {
			replace(newMode)
		}
	}

	private fun invalidateTypesMap(useDoublePages: Boolean) {
		modeMap[ReaderMode.STANDARD] = if (useDoublePages) {
			DoubleReaderFragment::class.java
		} else {
			PagerReaderFragment::class.java
		}
		modeMap[ReaderMode.REVERSED] = if (useDoublePages) {
			ReversedDoubleReaderFragment::class.java
		} else {
			ReversedReaderFragment::class.java
		}
		modeMap[ReaderMode.WEBTOON] = WebtoonReaderFragment::class.java
		modeMap[ReaderMode.VERTICAL] = VerticalReaderFragment::class.java
	}

	private fun isLandscape() = container.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}
