package com.example.dexreader.reader.ui.colorfilter

import android.content.DialogInterface
import androidx.activity.OnBackPressedCallback
import com.example.dexreader.R
import com.example.dexreader.core.util.ext.call
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ColorFilterConfigBackPressedDispatcher(
	private val activity: ColorFilterConfigActivity,
	private val viewModel: ColorFilterConfigViewModel,
) : OnBackPressedCallback(true), DialogInterface.OnClickListener {

	override fun handleOnBackPressed() {
		if (viewModel.isChanged) {
			showConfirmation()
		} else {
			viewModel.onDismiss.call(Unit)
		}
	}

	override fun onClick(dialog: DialogInterface, which: Int) {
		when (which) {
			DialogInterface.BUTTON_NEGATIVE -> viewModel.onDismiss.call(Unit)
			DialogInterface.BUTTON_NEUTRAL -> dialog.dismiss()
			DialogInterface.BUTTON_POSITIVE -> activity.showSaveConfirmation()
		}
	}

	private fun showConfirmation() {
		MaterialAlertDialogBuilder(activity)
			.setTitle(R.string.color_correction)
			.setMessage(R.string.text_unsaved_changes_prompt)
			.setNegativeButton(R.string.discard, this)
			.setNeutralButton(android.R.string.cancel, this)
			.setPositiveButton(R.string.save, this)
			.show()
	}
}
