package com.example.dexreader.main.ui

import android.Manifest
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import androidx.transition.TransitionManager
import com.example.dexreader.R
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.prefs.NavItem
import com.example.dexreader.core.ui.BaseActivity
import com.example.dexreader.core.ui.widgets.SlidingBottomNavigationView
import com.example.dexreader.core.util.ext.hideKeyboard
import com.example.dexreader.core.util.ext.observe
import com.example.dexreader.core.util.ext.observeEvent
import com.example.dexreader.core.util.ext.scaleUpActivityOptionsOf
import com.example.dexreader.databinding.ActivityMainBinding
import com.example.dexreader.details.service.MangaPrefetchService
import com.example.dexreader.details.ui.DetailsActivity
import com.example.dexreader.history.ui.HistoryListFragment
import com.example.dexreader.local.ui.LocalStorageCleanupWorker
import com.example.dexreader.main.ui.owners.AppBarOwner
import com.example.dexreader.main.ui.owners.BottomNavOwner
import com.example.dexreader.reader.ui.ReaderActivity.IntentBuilder
import com.example.dexreader.search.ui.MangaListActivity
import com.example.dexreader.search.ui.multi.MultiSearchActivity
import com.example.dexreader.search.ui.suggestion.SearchSuggestionFragment
import com.example.dexreader.search.ui.suggestion.SearchSuggestionListener
import com.example.dexreader.search.ui.suggestion.SearchSuggestionViewModel
import com.example.dexreader.settings.SettingsActivity
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.model.MangaSource
import org.example.dexreader.parsers.model.MangaTag
import javax.inject.Inject
import com.google.android.material.R as materialR

private const val TAG_SEARCH = "search"

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>(), AppBarOwner, BottomNavOwner,
	View.OnClickListener,
	View.OnFocusChangeListener, SearchSuggestionListener,
	MainNavigationDelegate.OnFragmentChangedListener {

	@Inject
	lateinit var settings: AppSettings

	private val viewModel by viewModels<MainViewModel>()
	private val searchSuggestionViewModel by viewModels<SearchSuggestionViewModel>()
	private val closeSearchCallback = CloseSearchCallback()
	private lateinit var navigationDelegate: MainNavigationDelegate

	override val appBar: AppBarLayout
		get() = viewBinding.appbar

	override val bottomNav: SlidingBottomNavigationView?
		get() = viewBinding.bottomNav

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityMainBinding.inflate(layoutInflater))

		with(viewBinding.searchView) {
			onFocusChangeListener = this@MainActivity
			searchSuggestionListener = this@MainActivity
		}

		viewBinding.fab?.setOnClickListener(this)
		viewBinding.navRail?.headerView?.setOnClickListener(this)

		navigationDelegate = MainNavigationDelegate(
			navBar = checkNotNull(bottomNav ?: viewBinding.navRail),
			fragmentManager = supportFragmentManager,
			settings = settings,
		)
		navigationDelegate.addOnFragmentChangedListener(this)
		navigationDelegate.onCreate(this, savedInstanceState)

		onBackPressedDispatcher.addCallback(ExitCallback(this, viewBinding.container))
		onBackPressedDispatcher.addCallback(navigationDelegate)
		onBackPressedDispatcher.addCallback(closeSearchCallback)

		if (savedInstanceState == null) {
			onFirstStart()
		}

		viewModel.onOpenReader.observeEvent(this, this::onOpenReader)
		viewModel.isLoading.observe(this, this::onLoadingStateChanged)
		viewModel.isResumeEnabled.observe(this, this::onResumeEnabledChanged)
		viewModel.feedCounter.observe(this, ::onFeedCounterChanged)
	}

	override fun onRestoreInstanceState(savedInstanceState: Bundle) {
		super.onRestoreInstanceState(savedInstanceState)
		adjustSearchUI(isSearchOpened(), animate = false)
	}

	override fun onFragmentChanged(fragment: Fragment, fromUser: Boolean) {
		adjustFabVisibility(topFragment = fragment)
		if (fromUser) {
			actionModeDelegate.finishActionMode()
			closeSearchCallback.handleOnBackPressed()
			viewBinding.appbar.setExpanded(true)
		}
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		super.onCreateOptionsMenu(menu)
		menuInflater.inflate(R.menu.opt_main, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
		android.R.id.home -> if (isSearchOpened()) {
			closeSearchCallback.handleOnBackPressed()
			true
		} else {
			viewBinding.searchView.requestFocus()
			true
		}

		R.id.action_settings -> {
			startActivity(SettingsActivity.newIntent(this))
			true
		}

		else -> super.onOptionsItemSelected(item)
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.fab, R.id.railFab -> viewModel.openLastReader()
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
	}

	override fun onFocusChange(v: View?, hasFocus: Boolean) {
		val fragment = supportFragmentManager.findFragmentByTag(TAG_SEARCH)
		if (v?.id == R.id.searchView && hasFocus) {
			if (fragment == null) {
				supportFragmentManager.commit {
					setReorderingAllowed(true)
					add(R.id.container, SearchSuggestionFragment.newInstance(), TAG_SEARCH)
					navigationDelegate.primaryFragment?.let {
						setMaxLifecycle(it, Lifecycle.State.STARTED)
					}
					setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
					runOnCommit { onSearchOpened() }
				}
			}
		}
	}

	override fun onMangaClick(manga: Manga) {
		startActivity(DetailsActivity.newIntent(this, manga))
	}

	override fun onQueryClick(query: String, submit: Boolean) {
		viewBinding.searchView.query = query
		if (submit && query.isNotEmpty()) {
			startActivity(MultiSearchActivity.newIntent(this, query))
			searchSuggestionViewModel.saveQuery(query)
			viewBinding.searchView.post {
				closeSearchCallback.handleOnBackPressed()
			}
		}
	}

	override fun onTagClick(tag: MangaTag) {
		startActivity(MangaListActivity.newIntent(this, setOf(tag)))
	}

	override fun onQueryChanged(query: String) {
		searchSuggestionViewModel.onQueryChanged(query)
	}

	override fun onSourceToggle(source: MangaSource, isEnabled: Boolean) {
		searchSuggestionViewModel.onSourceToggle(source, isEnabled)
	}

	override fun onSourceClick(source: MangaSource) {
		val intent = MangaListActivity.newIntent(this, source)
		startActivity(intent)
	}

	override fun onSupportActionModeStarted(mode: ActionMode) {
		super.onSupportActionModeStarted(mode)
		adjustFabVisibility()
		bottomNav?.hide()
	}

	override fun onSupportActionModeFinished(mode: ActionMode) {
		super.onSupportActionModeFinished(mode)
		adjustFabVisibility()
		bottomNav?.show()
	}

	private fun onOpenReader(manga: Manga) {
		val fab = viewBinding.fab ?: viewBinding.navRail?.headerView
		val options = fab?.let {
			scaleUpActivityOptionsOf(it)
		}
		startActivity(IntentBuilder(this).manga(manga).build(), options)
	}

	private fun onFeedCounterChanged(counter: Int) {
		navigationDelegate.setCounter(NavItem.FEED, counter)
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		viewBinding.fab?.isEnabled = !isLoading
	}

	private fun onResumeEnabledChanged(isEnabled: Boolean) {
		adjustFabVisibility(isResumeEnabled = isEnabled)
	}

	private fun onSearchOpened() {
		adjustSearchUI(isOpened = true, animate = true)
	}

	private fun onSearchClosed() {
		viewBinding.searchView.hideKeyboard()
		adjustSearchUI(isOpened = false, animate = true)
	}

	private fun isSearchOpened(): Boolean {
		return supportFragmentManager.findFragmentByTag(TAG_SEARCH) != null
	}

	private fun onFirstStart() {
		lifecycleScope.launch(Dispatchers.Main) { // not a default `Main.immediate` dispatcher
			withContext(Dispatchers.Default) {
				LocalStorageCleanupWorker.enqueue(applicationContext)
			}
			withResumed {
				MangaPrefetchService.prefetchLast(this@MainActivity)
				requestNotificationsPermission()
			}
		}
	}

	private fun adjustFabVisibility(
		isResumeEnabled: Boolean = viewModel.isResumeEnabled.value,
		topFragment: Fragment? = navigationDelegate.primaryFragment,
		isSearchOpened: Boolean = isSearchOpened(),
	) {
		val fab = viewBinding.fab ?: return
		if (isResumeEnabled && !actionModeDelegate.isActionModeStarted && !isSearchOpened && topFragment is HistoryListFragment) {
			if (!fab.isVisible) {
				fab.show()
			}
		} else {
			if (fab.isVisible) {
				fab.hide()
			}
		}
	}

	private fun adjustSearchUI(isOpened: Boolean, animate: Boolean) {
		if (animate) {
			TransitionManager.beginDelayedTransition(viewBinding.appbar)
		}
		val appBarScrollFlags = if (isOpened) {
			SCROLL_FLAG_NO_SCROLL
		} else {
			SCROLL_FLAG_SCROLL or SCROLL_FLAG_ENTER_ALWAYS or SCROLL_FLAG_SNAP
		}
		viewBinding.toolbarCard.updateLayoutParams<AppBarLayout.LayoutParams> {
			scrollFlags = appBarScrollFlags
		}
		viewBinding.insetsHolder.updateLayoutParams<AppBarLayout.LayoutParams> {
			scrollFlags = appBarScrollFlags
		}
		viewBinding.toolbarCard.background = if (isOpened) {
			null
		} else {
			ContextCompat.getDrawable(this, R.drawable.search_bar_background)
		}
		val padding = if (isOpened) 0 else resources.getDimensionPixelOffset(R.dimen.margin_normal)
		viewBinding.appbar.updatePadding(left = padding, right = padding)
		adjustFabVisibility(isSearchOpened = isOpened)
		supportActionBar?.apply {
			setHomeAsUpIndicator(
				when {
					isOpened -> materialR.drawable.abc_ic_ab_back_material
					else -> materialR.drawable.abc_ic_search_api_material
				},
			)
			setHomeActionContentDescription(
				if (isOpened) R.string.back else R.string.search,
			)
		}
		viewBinding.searchView.setHintCompat(
			if (isOpened) R.string.search_hint else R.string.search_manga,
		)
		bottomNav?.showOrHide(!isOpened)
		closeSearchCallback.isEnabled = isOpened
	}

	private fun requestNotificationsPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(
				this,
				Manifest.permission.POST_NOTIFICATIONS,
			) != PERMISSION_GRANTED
		) {
			ActivityCompat.requestPermissions(
				this,
				arrayOf(Manifest.permission.POST_NOTIFICATIONS),
				1,
			)
		}
	}

	private inner class CloseSearchCallback : OnBackPressedCallback(false) {

		override fun handleOnBackPressed() {
			val fm = supportFragmentManager
			val fragment = fm.findFragmentByTag(TAG_SEARCH)
			viewBinding.searchView.clearFocus()
			if (fragment == null) {
				isEnabled = false
				return
			}
			fm.commit {
				setReorderingAllowed(true)
				remove(fragment)
				navigationDelegate.primaryFragment?.let {
					setMaxLifecycle(it, Lifecycle.State.RESUMED)
				}
				setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
				runOnCommit { onSearchClosed() }
			}
		}
	}
}
