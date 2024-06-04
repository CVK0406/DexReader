package com.example.dexreader.core.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.dexreader.R
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.ui.util.RecyclerViewOwner
import com.example.dexreader.core.ui.util.WindowInsetsDelegate
import com.example.dexreader.core.util.ext.getThemeColor
import com.example.dexreader.core.util.ext.parentView
import com.example.dexreader.settings.SettingsActivity
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
abstract class BasePreferenceFragment(@StringRes private val titleId: Int) :
	PreferenceFragmentCompat(),
	WindowInsetsDelegate.WindowInsetsListener,
	RecyclerViewOwner {

	@Inject
	lateinit var settings: AppSettings

	@JvmField
	protected val insetsDelegate = WindowInsetsDelegate()

	override val recyclerView: RecyclerView
		get() = listView

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val themedContext = (view.parentView ?: view).context
		view.setBackgroundColor(themedContext.getThemeColor(android.R.attr.colorBackground))
		listView.clipToPadding = false
		insetsDelegate.onViewCreated(view)
		insetsDelegate.addInsetsListener(this)
	}

	override fun onDestroyView() {
		insetsDelegate.removeInsetsListener(this)
		insetsDelegate.onDestroyView()
		super.onDestroyView()
	}

	override fun onResume() {
		super.onResume()
		setTitle(if (titleId != 0) getString(titleId) else null)
	}

	@CallSuper
	override fun onWindowInsetsChanged(insets: Insets) {
		listView.updatePadding(
			bottom = insets.bottom,
		)
	}

	protected fun setTitle(title: CharSequence?) {
		(activity as? SettingsActivity)?.setSectionTitle(title)
	}

	protected fun startActivitySafe(intent: Intent) {
		try {
			startActivity(intent)
		} catch (_: ActivityNotFoundException) {
			Snackbar.make(listView, R.string.operation_not_supported, Snackbar.LENGTH_SHORT).show()
		}
	}
}
