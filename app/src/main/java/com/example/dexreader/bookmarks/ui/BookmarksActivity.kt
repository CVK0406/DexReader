package com.example.dexreader.bookmarks.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.fragment.app.commit
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import com.example.dexreader.R
import com.example.dexreader.core.ui.BaseActivity
import com.example.dexreader.databinding.ActivityContainerBinding
import com.example.dexreader.main.ui.owners.AppBarOwner
import com.example.dexreader.main.ui.owners.SnackbarOwner

@AndroidEntryPoint
class BookmarksActivity :
	BaseActivity<ActivityContainerBinding>(),
	AppBarOwner,
	SnackbarOwner {

	override val appBar: AppBarLayout
		get() = viewBinding.appbar

	override val snackbarHost: CoordinatorLayout
		get() = viewBinding.root

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityContainerBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		val fm = supportFragmentManager
		if (fm.findFragmentById(R.id.container) == null) {
			fm.commit {
				setReorderingAllowed(true)
				replace(R.id.container, BookmarksFragment::class.java, null)
			}
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
	}

	companion object {

		fun newIntent(context: Context) = Intent(context, BookmarksActivity::class.java)
	}
}
