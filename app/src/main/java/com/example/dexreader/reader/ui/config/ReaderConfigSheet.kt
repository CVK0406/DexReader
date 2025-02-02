package com.example.dexreader.reader.ui.config

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.dexreader.R
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.prefs.ReaderMode
import com.example.dexreader.core.prefs.observeAsStateFlow
import com.example.dexreader.core.ui.sheet.BaseAdaptiveSheet
import com.example.dexreader.core.util.ScreenOrientationHelper
import com.example.dexreader.core.util.ext.observe
import com.example.dexreader.core.util.ext.showDistinct
import com.example.dexreader.core.util.ext.viewLifecycleScope
import com.example.dexreader.core.util.ext.withArgs
import com.example.dexreader.databinding.SheetReaderConfigBinding
import com.example.dexreader.settings.SettingsActivity
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import com.example.dexreader.reader.ui.ReaderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import javax.inject.Inject

@AndroidEntryPoint
class ReaderConfigSheet :
	BaseAdaptiveSheet<SheetReaderConfigBinding>(),
	View.OnClickListener,
	MaterialButtonToggleGroup.OnButtonCheckedListener,
	Slider.OnChangeListener,
	CompoundButton.OnCheckedChangeListener {

	private val viewModel by activityViewModels<ReaderViewModel>()

	@Inject
	lateinit var orientationHelper: ScreenOrientationHelper

	private lateinit var mode: ReaderMode

	@Inject
	lateinit var settings: AppSettings

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		mode = arguments?.getInt(ARG_MODE)
			?.let { ReaderMode.valueOf(it) }
			?: ReaderMode.STANDARD
	}

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	): SheetReaderConfigBinding {
		return SheetReaderConfigBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(
		binding: SheetReaderConfigBinding,
		savedInstanceState: Bundle?,
	) {
		super.onViewBindingCreated(binding, savedInstanceState)
		observeScreenOrientation()
		binding.buttonStandard.isChecked = mode == ReaderMode.STANDARD
		binding.buttonReversed.isChecked = mode == ReaderMode.REVERSED
		binding.buttonWebtoon.isChecked = mode == ReaderMode.WEBTOON
		binding.buttonVertical.isChecked = mode == ReaderMode.VERTICAL
		binding.switchDoubleReader.isChecked = settings.isReaderDoubleOnLandscape
		binding.switchDoubleReader.isEnabled = mode == ReaderMode.STANDARD

		binding.checkableGroup.addOnButtonCheckedListener(this)
		binding.buttonSavePage.setOnClickListener(this)
		binding.buttonScreenRotate.setOnClickListener(this)
		binding.buttonSettings.setOnClickListener(this)
		binding.sliderTimer.addOnChangeListener(this)
		binding.switchScrollTimer.setOnCheckedChangeListener(this)
		binding.switchDoubleReader.setOnCheckedChangeListener(this)

		settings.observeAsStateFlow(
			scope = lifecycleScope + Dispatchers.Default,
			key = AppSettings.KEY_READER_AUTOSCROLL_SPEED,
			valueProducer = { readerAutoscrollSpeed },
		).observe(viewLifecycleOwner) {
			binding.sliderTimer.value = it.coerceIn(
				binding.sliderTimer.valueFrom,
				binding.sliderTimer.valueTo,
			)
		}
		findCallback()?.run {
			binding.switchScrollTimer.isChecked = isAutoScrollEnabled
		}
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_settings -> {
				startActivity(SettingsActivity.newReaderSettingsIntent(v.context))
				dismissAllowingStateLoss()
			}

			R.id.button_save_page -> {
				findCallback()?.onSavePageClick() ?: return
				dismissAllowingStateLoss()
			}

			R.id.button_screen_rotate -> {
				orientationHelper.isLandscape = !orientationHelper.isLandscape
			}
		}
	}

	override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
		when (buttonView.id) {
			R.id.switch_scroll_timer -> {
				findCallback()?.isAutoScrollEnabled = isChecked
				requireViewBinding().layoutTimer.isVisible = isChecked
				requireViewBinding().sliderTimer.isVisible = isChecked
			}

			R.id.switch_screen_lock_rotation -> {
				orientationHelper.isLocked = isChecked
			}

			R.id.switch_double_reader -> {
				settings.isReaderDoubleOnLandscape = isChecked
				findCallback()?.onDoubleModeChanged(isChecked)
			}
		}
	}

	override fun onButtonChecked(
		group: MaterialButtonToggleGroup?,
		checkedId: Int,
		isChecked: Boolean,
	) {
		if (!isChecked) {
			return
		}
		val newMode = when (checkedId) {
			R.id.button_standard -> ReaderMode.STANDARD
			R.id.button_webtoon -> ReaderMode.WEBTOON
			R.id.button_reversed -> ReaderMode.REVERSED
			R.id.button_vertical -> ReaderMode.VERTICAL
			else -> return
		}
		viewBinding?.switchDoubleReader?.isEnabled = newMode == ReaderMode.STANDARD || newMode == ReaderMode.REVERSED
		if (newMode == mode) {
			return
		}
		findCallback()?.onReaderModeChanged(newMode) ?: return
		mode = newMode
	}

	override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
		if (fromUser) {
			settings.readerAutoscrollSpeed = value
		}
		(viewBinding ?: return).labelTimerValue.text = getString(R.string.speed_value, value * 10f)
	}

	private fun observeScreenOrientation() {
		orientationHelper.observeAutoOrientation()
			.onEach {
				with(requireViewBinding()) {
					buttonScreenRotate.isGone = it
					switchScreenLockRotation.isVisible = it
					updateOrientationLockSwitch()
				}
			}.launchIn(viewLifecycleScope)
	}

	private fun updateOrientationLockSwitch() {
		val switch = viewBinding?.switchScreenLockRotation ?: return
		switch.setOnCheckedChangeListener(null)
		switch.isChecked = orientationHelper.isLocked
		switch.setOnCheckedChangeListener(this)
	}

	private fun findCallback(): Callback? {
		return (parentFragment as? Callback) ?: (activity as? Callback)
	}

	interface Callback {

		var isAutoScrollEnabled: Boolean

		fun onReaderModeChanged(mode: ReaderMode)

		fun onDoubleModeChanged(isEnabled: Boolean)

		fun onSavePageClick()
	}

	companion object {

		private const val TAG = "ReaderConfigBottomSheet"
		private const val ARG_MODE = "mode"

		fun show(fm: FragmentManager, mode: ReaderMode) = ReaderConfigSheet().withArgs(1) {
			putInt(ARG_MODE, mode.id)
		}.showDistinct(fm, TAG)
	}
}
