package com.example.dexreader.settings.sources.catalog

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.Insets
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import coil.ImageLoader
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import com.example.dexreader.R
import com.example.dexreader.core.ui.BaseActivity
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.core.ui.util.ReversibleActionObserver
import com.example.dexreader.core.util.ext.getDisplayName
import com.example.dexreader.core.util.ext.observe
import com.example.dexreader.core.util.ext.observeEvent
import com.example.dexreader.core.util.ext.toLocale
import com.example.dexreader.databinding.ActivitySourcesCatalogBinding
import com.example.dexreader.main.ui.owners.AppBarOwner
import com.example.dexreader.settings.newsources.NewSourcesDialogFragment
import javax.inject.Inject

@AndroidEntryPoint
class SourcesCatalogActivity : BaseActivity<ActivitySourcesCatalogBinding>(),
	OnListItemClickListener<SourceCatalogItem.Source>,
	AppBarOwner, MenuItem.OnActionExpandListener {

	@Inject
	lateinit var coil: ImageLoader

	private var newSourcesSnackbar: Snackbar? = null

	override val appBar: AppBarLayout
		get() = viewBinding.appbar

	private val viewModel by viewModels<SourcesCatalogViewModel>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivitySourcesCatalogBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		val pagerAdapter = SourcesCatalogPagerAdapter(this, coil, this)
		viewBinding.pager.adapter = pagerAdapter
		val tabMediator = TabLayoutMediator(viewBinding.tabs, viewBinding.pager, pagerAdapter)
		tabMediator.attach()
		viewModel.content.observe(this, pagerAdapter)
		viewModel.hasNewSources.observe(this, ::onHasNewSourcesChanged)
		viewModel.onActionDone.observeEvent(
			this,
			ReversibleActionObserver(viewBinding.pager),
		)
		viewModel.locale.observe(this) {
			supportActionBar?.subtitle = it?.toLocale().getDisplayName(this)
		}
		addMenuProvider(SourcesCatalogMenuProvider(this, viewModel, this))
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
	}

	override fun onItemClick(item: SourceCatalogItem.Source, view: View) {
		viewModel.addSource(item.source)
	}

	override fun onMenuItemActionExpand(item: MenuItem): Boolean {
		viewBinding.tabs.isVisible = false
		viewBinding.pager.isUserInputEnabled = false
		val sq = (item.actionView as? SearchView)?.query?.trim()?.toString().orEmpty()
		viewModel.performSearch(sq)
		return true
	}

	override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
		viewBinding.tabs.isVisible = true
		viewBinding.pager.isUserInputEnabled = true
		viewModel.performSearch(null)
		return true
	}

	private fun onHasNewSourcesChanged(hasNewSources: Boolean) {
		if (hasNewSources) {
			if (newSourcesSnackbar?.isShownOrQueued == true) {
				return
			}
			val snackbar = Snackbar.make(viewBinding.pager, R.string.new_sources_text, Snackbar.LENGTH_INDEFINITE)
			snackbar.setAction(R.string.explore) {
				NewSourcesDialogFragment.show(supportFragmentManager)
			}
			snackbar.addCallback(
				object : Snackbar.Callback() {
					override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
						super.onDismissed(transientBottomBar, event)
						if (event == DISMISS_EVENT_SWIPE) {
							viewModel.skipNewSources()
						}
					}
				},
			)
			snackbar.show()
			newSourcesSnackbar = snackbar
		} else {
			newSourcesSnackbar?.dismiss()
			newSourcesSnackbar = null
		}
	}
}
