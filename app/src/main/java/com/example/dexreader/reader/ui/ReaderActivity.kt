package com.example.dexreader.reader.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.transition.Fade
import android.transition.Slide
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import androidx.activity.result.ActivityResultCallback
import androidx.activity.viewModels
import androidx.core.graphics.Insets
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.example.dexreader.BuildConfig
import com.example.dexreader.R
import com.example.dexreader.bookmarks.domain.Bookmark
import com.example.dexreader.core.model.parcelable.ParcelableManga
import com.example.dexreader.core.parser.MangaIntent
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.prefs.ReaderMode
import com.example.dexreader.core.ui.BaseFullscreenActivity
import com.example.dexreader.core.ui.util.MenuInvalidator
import com.example.dexreader.core.ui.widgets.ZoomControl
import com.example.dexreader.core.util.IdlingDetector
import com.example.dexreader.core.util.ShareHelper
import com.example.dexreader.core.util.ext.hasGlobalPoint
import com.example.dexreader.core.util.ext.isAnimationsEnabled
import com.example.dexreader.core.util.ext.observe
import com.example.dexreader.core.util.ext.observeEvent
import com.example.dexreader.core.util.ext.postDelayed
import com.example.dexreader.core.util.ext.zipWithPrevious
import com.example.dexreader.databinding.ActivityReaderBinding
import com.example.dexreader.details.ui.DetailsActivity
import com.example.dexreader.reader.data.TapGridSettings
import com.example.dexreader.reader.domain.TapGridArea
import com.example.dexreader.reader.ui.config.ReaderConfigSheet
import com.example.dexreader.reader.ui.pager.ReaderPage
import com.example.dexreader.reader.ui.pager.ReaderUiState
import com.example.dexreader.reader.ui.tapgrid.TapGridDispatcher
import com.example.dexreader.reader.ui.thumbnails.OnPageSelectListener
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.model.MangaChapter
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class ReaderActivity :
	BaseFullscreenActivity<ActivityReaderBinding>(),
	ChaptersSheet.OnChapterChangeListener,
	TapGridDispatcher.OnGridTouchListener,
	OnPageSelectListener,
	ReaderConfigSheet.Callback,
	ReaderControlDelegate.OnInteractionListener,
	OnApplyWindowInsetsListener,
	IdlingDetector.Callback,
	ActivityResultCallback<Uri?>,
	ZoomControl.ZoomControlListener {

	@Inject
	lateinit var settings: AppSettings

	@Inject
	lateinit var tapGridSettings: TapGridSettings

	private val idlingDetector = IdlingDetector(TimeUnit.SECONDS.toMillis(10), this)
	private val savePageRequest = registerForActivityResult(PageSaveContract(), this)

	private val viewModel: ReaderViewModel by viewModels()

	override val readerMode: ReaderMode?
		get() = readerManager.currentMode

	override var isAutoScrollEnabled: Boolean
		get() = scrollTimer.isEnabled
		set(value) {
			scrollTimer.isEnabled = value
		}

	@Inject
	lateinit var scrollTimerFactory: ScrollTimer.Factory

	private lateinit var scrollTimer: ScrollTimer
	private lateinit var touchHelper: TapGridDispatcher
	private lateinit var controlDelegate: ReaderControlDelegate
	private var gestureInsets: Insets = Insets.NONE
	private lateinit var readerManager: ReaderManager
	private val hideUiRunnable = Runnable { setUiIsVisible(false) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityReaderBinding.inflate(layoutInflater))
		readerManager = ReaderManager(supportFragmentManager, viewBinding.container, settings)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		touchHelper = TapGridDispatcher(this, this)
		scrollTimer = scrollTimerFactory.create(this, this)
		controlDelegate = ReaderControlDelegate(resources, settings, tapGridSettings, this)
		viewBinding.zoomControl.listener = this
		insetsDelegate.interceptingWindowInsetsListener = this
		idlingDetector.bindToLifecycle(this)
		viewModel.readerMode.observe(this, Lifecycle.State.STARTED, this::onInitReader)
		viewModel.onPageSaved.observeEvent(this, this::onPageSaved)
		viewModel.uiState.zipWithPrevious().observe(this, this::onUiStateChanged)
		viewModel.isLoading.observe(this, this::onLoadingStateChanged)
		viewModel.content.observe(this) {
			onLoadingStateChanged(viewModel.isLoading.value)
		}
		viewModel.isScreenshotsBlockEnabled.observe(this, this::setWindowSecure)
		viewModel.isKeepScreenOnEnabled.observe(this, this::setKeepScreenOn)
		viewModel.isInfoBarEnabled.observe(this, ::onReaderBarChanged)
		viewModel.isBookmarkAdded.observe(this, MenuInvalidator(viewBinding.toolbarBottom))
		viewModel.onShowToast.observeEvent(this) { msgId ->
			Snackbar.make(viewBinding.container, msgId, Snackbar.LENGTH_SHORT)
				.setAnchorView(viewBinding.appbarBottom)
				.show()
		}
		viewModel.isZoomControlsEnabled.observe(this) {
			viewBinding.zoomControl.isVisible = it
		}
		addMenuProvider(ReaderTopMenuProvider(this, viewModel))
		viewBinding.toolbarBottom.addMenuProvider(ReaderBottomMenuProvider(this, readerManager, viewModel))
	}

	override fun onActivityResult(result: Uri?) {
		viewModel.onActivityResult(result)
	}

	override fun getParentActivityIntent(): Intent? {
		val manga = viewModel.manga?.toManga() ?: return null
		return DetailsActivity.newIntent(this, manga)
	}

	override fun onUserInteraction() {
		super.onUserInteraction()
		scrollTimer.onUserInteraction()
		idlingDetector.onUserInteraction()
	}

	override fun onPause() {
		super.onPause()
		viewModel.onPause()
	}

	override fun onIdle() {
		viewModel.saveCurrentState(readerManager.currentReader?.getCurrentState())
	}

	override fun onZoomIn() {
		readerManager.currentReader?.onZoomIn()
	}

	override fun onZoomOut() {
		readerManager.currentReader?.onZoomOut()
	}

	private fun onInitReader(mode: ReaderMode?) {
		if (mode == null) {
			return
		}
		if (readerManager.currentMode != mode) {
			readerManager.replace(mode)
		}
		if (viewBinding.appbarTop.isVisible) {
			lifecycle.postDelayed(TimeUnit.SECONDS.toMillis(1), hideUiRunnable)
		}
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		val hasPages = viewModel.content.value.pages.isNotEmpty()
		viewBinding.layoutLoading.isVisible = isLoading && !hasPages
		if (isLoading && hasPages) {
			viewBinding.toastView.show(R.string.loading_)
		} else {
			viewBinding.toastView.hide()
		}
		viewBinding.toolbarBottom.invalidateMenu()
	}

	override fun onGridTouch(area: TapGridArea): Boolean {
		return isReaderResumed() && controlDelegate.onGridTouch(area)
	}

	override fun onGridLongTouch(area: TapGridArea) {
		if (isReaderResumed()) {
			controlDelegate.onGridLongTouch(area)
		}
	}

	override fun onProcessTouch(rawX: Int, rawY: Int): Boolean {
		return if (
			rawX <= gestureInsets.left ||
			rawY <= gestureInsets.top ||
			rawX >= viewBinding.root.width - gestureInsets.right ||
			rawY >= viewBinding.root.height - gestureInsets.bottom ||
			viewBinding.appbarTop.hasGlobalPoint(rawX, rawY) ||
			viewBinding.appbarBottom?.hasGlobalPoint(rawX, rawY) == true
		) {
			false
		} else {
			val touchables = window.peekDecorView()?.touchables
			touchables?.none { it.hasGlobalPoint(rawX, rawY) } ?: true
		}
	}

	override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
		touchHelper.dispatchTouchEvent(ev)
		scrollTimer.onTouchEvent(ev)
		return super.dispatchTouchEvent(ev)
	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
		return controlDelegate.onKeyDown(keyCode) || super.onKeyDown(keyCode, event)
	}

	override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
		return controlDelegate.onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event)
	}

	override fun onChapterChanged(chapter: MangaChapter) {
		viewModel.switchChapter(chapter.id, 0)
	}

	override fun onPageSelected(page: ReaderPage) {
		lifecycleScope.launch(Dispatchers.Default) {
			val pages = viewModel.content.value.pages
			val index = pages.indexOfFirst { it.chapterId == page.chapterId && it.id == page.id }
			if (index != -1) {
				withContext(Dispatchers.Main) {
					readerManager.currentReader?.switchPageTo(index, true)
				}
			} else {
				viewModel.switchChapter(page.chapterId, page.index)
			}
		}
	}

	override fun onReaderModeChanged(mode: ReaderMode) {
		viewModel.saveCurrentState(readerManager.currentReader?.getCurrentState())
		viewModel.switchMode(mode)
	}

	override fun onDoubleModeChanged(isEnabled: Boolean) {
		readerManager.setDoubleReaderMode(isEnabled)
	}

	private fun onPageSaved(uri: Uri?) {
		if (uri != null) {
			Snackbar.make(viewBinding.container, R.string.page_saved, Snackbar.LENGTH_LONG)
				.setAction(R.string.share) {
					ShareHelper(this).shareImage(uri)
				}
		} else {
			Snackbar.make(viewBinding.container, R.string.error_occurred, Snackbar.LENGTH_SHORT)
		}.setAnchorView(viewBinding.appbarBottom)
			.show()
	}

	private fun setWindowSecure(isSecure: Boolean) {
		if (isSecure) {
			window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
		} else {
			window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
		}
	}

	private fun setKeepScreenOn(isKeep: Boolean) {
		if (isKeep) {
			window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		} else {
			window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		}
	}

	private fun setUiIsVisible(isUiVisible: Boolean) {
		if (viewBinding.appbarTop.isVisible != isUiVisible) {
			if (isAnimationsEnabled) {
				val transition = TransitionSet()
					.setOrdering(TransitionSet.ORDERING_TOGETHER)
					.addTransition(Slide(Gravity.TOP).addTarget(viewBinding.appbarTop))
					.addTransition(Fade().addTarget(viewBinding.infoBar))
				viewBinding.appbarBottom?.let { bottomBar ->
					transition.addTransition(Slide(Gravity.BOTTOM).addTarget(bottomBar))
				}
				TransitionManager.beginDelayedTransition(viewBinding.root, transition)
			}
			val isFullscreen = settings.isReaderFullscreenEnabled
			viewBinding.appbarTop.isVisible = isUiVisible
			viewBinding.appbarBottom?.isVisible = isUiVisible
			viewBinding.infoBar.isGone = isUiVisible || (!viewModel.isInfoBarEnabled.value)
			viewBinding.infoBar.isTimeVisible = isFullscreen
			systemUiController.setSystemUiVisible(isUiVisible || !isFullscreen)
		}
	}

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		gestureInsets = insets.getInsets(WindowInsetsCompat.Type.systemGestures())
		val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
		viewBinding.appbarTop.updatePadding(
			top = systemBars.top,
			right = systemBars.right,
			left = systemBars.left,
		)
		viewBinding.appbarBottom?.updateLayoutParams<MarginLayoutParams> {
			bottomMargin = systemBars.bottom + topMargin
			rightMargin = systemBars.right + topMargin
			leftMargin = systemBars.left + topMargin
		}
		viewBinding.infoBar.updatePadding(
			top = systemBars.top,
		)
		return WindowInsetsCompat.Builder(insets)
			.setInsets(WindowInsetsCompat.Type.systemBars(), Insets.NONE)
			.build()
	}

	override fun onWindowInsetsChanged(insets: Insets) = Unit

	override fun switchPageBy(delta: Int) {
		readerManager.currentReader?.switchPageBy(delta)
	}

	override fun switchChapterBy(delta: Int) {
		viewModel.switchChapterBy(delta)
	}

	override fun openMenu() {
		viewModel.saveCurrentState(readerManager.currentReader?.getCurrentState())
		val currentMode = readerManager.currentMode ?: return
		ReaderConfigSheet.show(supportFragmentManager, currentMode)
	}

	override fun scrollBy(delta: Int, smooth: Boolean): Boolean {
		return readerManager.currentReader?.scrollBy(delta, smooth) ?: false
	}

	override fun toggleUiVisibility() {
		setUiIsVisible(!viewBinding.appbarTop.isVisible)
	}

	override fun isReaderResumed(): Boolean {
		val reader = readerManager.currentReader ?: return false
		return reader.isResumed && supportFragmentManager.fragments.lastOrNull() === reader
	}

	override fun onSavePageClick() {
		val page = viewModel.getCurrentPage() ?: return
		viewModel.saveCurrentPage(page, savePageRequest)
	}

	private fun onReaderBarChanged(isBarEnabled: Boolean) {
		viewBinding.infoBar.isVisible = isBarEnabled && viewBinding.appbarTop.isGone
	}

	private fun onUiStateChanged(pair: Pair<ReaderUiState?, ReaderUiState?>) {
		val (previous: ReaderUiState?, uiState: ReaderUiState?) = pair
		title = uiState?.resolveTitle(this) ?: getString(R.string.loading_)
		viewBinding.infoBar.update(uiState)
		if (uiState == null) {
			supportActionBar?.subtitle = null
			return
		}
		supportActionBar?.subtitle = uiState.chapterName
		if (previous?.chapterName != null && uiState.chapterName != previous.chapterName) {
			if (!uiState.chapterName.isNullOrEmpty()) {
				viewBinding.toastView.showTemporary(uiState.chapterName, TOAST_DURATION)
			}
		}
	}

	class IntentBuilder(context: Context) {

		private val intent = Intent(context, ReaderActivity::class.java)
			.setAction(ACTION_MANGA_READ)

		fun manga(manga: Manga) = apply {
			intent.putExtra(MangaIntent.KEY_MANGA, ParcelableManga(manga))
		}

		fun mangaId(mangaId: Long) = apply {
			intent.putExtra(MangaIntent.KEY_ID, mangaId)
		}

		fun branch(branch: String?) = apply {
			intent.putExtra(EXTRA_BRANCH, branch)
		}

		fun state(state: ReaderState?) = apply {
			intent.putExtra(EXTRA_STATE, state)
		}

		fun bookmark(bookmark: Bookmark) = manga(
			bookmark.manga,
		).state(
			ReaderState(
				chapterId = bookmark.chapterId,
				page = bookmark.page,
				scroll = bookmark.scroll,
			),
		)

		fun build() = intent
	}

	companion object {

		const val ACTION_MANGA_READ = "${BuildConfig.APPLICATION_ID}.action.READ_MANGA"
		const val EXTRA_STATE = "state"
		const val EXTRA_BRANCH = "branch"
		private const val TOAST_DURATION = 1500L
	}
}
