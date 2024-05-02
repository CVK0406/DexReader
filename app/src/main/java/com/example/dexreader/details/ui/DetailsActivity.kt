package com.example.dexreader.details.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.style.DynamicDrawableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.RelativeSizeSpan
import android.transition.AutoTransition
import android.transition.Slide
import android.transition.TransitionManager
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.Insets
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.core.view.MenuHost
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.filterNotNull
import com.example.dexreader.R
import com.example.dexreader.core.model.parcelable.ParcelableManga
import com.example.dexreader.core.parser.MangaIntent
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.ui.BaseActivity
import com.example.dexreader.core.ui.util.MenuInvalidator
import com.example.dexreader.core.ui.util.ReversibleActionObserver
import com.example.dexreader.core.util.ext.doOnExpansionsChanged
import com.example.dexreader.core.util.ext.getAnimationDuration
import com.example.dexreader.core.util.ext.getThemeColor
import com.example.dexreader.core.util.ext.isAnimationsEnabled
import com.example.dexreader.core.util.ext.measureHeight
import com.example.dexreader.core.util.ext.menuView
import com.example.dexreader.core.util.ext.observe
import com.example.dexreader.core.util.ext.observeEvent
import com.example.dexreader.core.util.ext.recyclerView
import com.example.dexreader.core.util.ext.setNavigationBarTransparentCompat
import com.example.dexreader.core.util.ext.setNavigationIconSafe
import com.example.dexreader.core.util.ext.setOnContextClickListenerCompat
import com.example.dexreader.core.util.ext.textAndVisible
import com.example.dexreader.databinding.ActivityDetailsBinding
import com.example.dexreader.details.service.MangaPrefetchService
import com.example.dexreader.details.ui.model.ChapterListItem
import com.example.dexreader.details.ui.model.HistoryInfo
import com.example.dexreader.details.ui.pager.DetailsPagerAdapter
import com.example.dexreader.download.ui.worker.DownloadStartedObserver
import com.example.dexreader.main.ui.owners.NoModalBottomSheetOwner
import org.example.dexreader.parsers.model.Manga
import com.example.dexreader.reader.ui.ReaderActivity.IntentBuilder
import com.example.dexreader.reader.ui.thumbnails.PagesThumbnailsSheet
import java.lang.ref.WeakReference
import javax.inject.Inject
import com.google.android.material.R as materialR

@AndroidEntryPoint
class DetailsActivity :
	BaseActivity<ActivityDetailsBinding>(),
	View.OnClickListener,
	NoModalBottomSheetOwner,
	View.OnLongClickListener,
	PopupMenu.OnMenuItemClickListener {

	@Inject
	lateinit var settings: AppSettings

	private var buttonTip: WeakReference<ButtonTip>? = null

	private val viewModel: DetailsViewModel by viewModels()

	val secondaryMenuHost: MenuHost
		get() = viewBinding.toolbarChapters ?: this

	var bottomSheetMediator: ChaptersBottomSheetMediator? = null
		private set

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityDetailsBinding.inflate(layoutInflater))
		supportActionBar?.run {
			setDisplayHomeAsUpEnabled(true)
			setDisplayShowTitleEnabled(false)
		}
		viewBinding.buttonRead.setOnClickListener(this)
		viewBinding.buttonRead.setOnLongClickListener(this)
		viewBinding.buttonRead.setOnContextClickListenerCompat(this)
		viewBinding.buttonDropdown.setOnClickListener(this)

		if (viewBinding.layoutBottom != null) {
			val behavior = BottomSheetBehavior.from(checkNotNull(viewBinding.layoutBottom))
			val bsMediator = ChaptersBottomSheetMediator(behavior, viewBinding.pager, viewBinding.tabs)
			actionModeDelegate.addListener(bsMediator)
			checkNotNull(viewBinding.layoutBsHeader).addOnLayoutChangeListener(bsMediator)
			onBackPressedDispatcher.addCallback(bsMediator)
			bottomSheetMediator = bsMediator
			behavior.doOnExpansionsChanged(::onChaptersSheetStateChanged)
			viewBinding.toolbarChapters?.setNavigationOnClickListener {
				behavior.state = BottomSheetBehavior.STATE_COLLAPSED
			}
			viewBinding.toolbarChapters?.setOnGenericMotionListener(bsMediator)
		}
		initPager()

		viewModel.manga.filterNotNull().observe(this, ::onMangaUpdated)
		viewModel.onMangaRemoved.observeEvent(this, ::onMangaRemoved)
		viewModel.newChaptersCount.observe(this, ::onNewChaptersChanged)
		viewModel.onActionDone.observeEvent(
			this,
			ReversibleActionObserver(viewBinding.containerDetails, viewBinding.layoutBottom),
		)
		viewModel.onShowTip.observeEvent(this) { showTip() }
		viewModel.historyInfo.observe(this, ::onHistoryChanged)
		viewModel.selectedBranch.observe(this) {
			viewBinding.toolbarChapters?.subtitle = it
			viewBinding.textViewSubtitle?.textAndVisible = it
		}
		val chaptersMenuInvalidator = MenuInvalidator(viewBinding.toolbarChapters ?: this)
		viewModel.isChaptersReversed.observe(this, chaptersMenuInvalidator)
		viewModel.isChaptersEmpty.observe(this, chaptersMenuInvalidator)
		val menuInvalidator = MenuInvalidator(this)
		viewModel.favouriteCategories.observe(this, menuInvalidator)
		viewModel.isStatsAvailable.observe(this, menuInvalidator)
		viewModel.remoteManga.observe(this, menuInvalidator)
		viewModel.branches.observe(this) {
			viewBinding.buttonDropdown.isVisible = it.size > 1
		}
		viewModel.chapters.observe(this, PrefetchObserver(this))
		viewModel.onDownloadStarted.observeEvent(
			this,
			DownloadStartedObserver(viewBinding.containerDetails),
		)

		addMenuProvider(
			DetailsMenuProvider(
				activity = this,
				viewModel = viewModel,
				snackbarHost = viewBinding.pager,
			),
		)
	}

	override fun getBottomSheetCollapsedHeight(): Int {
		return viewBinding.layoutBsHeader?.measureHeight() ?: 0
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_read -> openReader()
			R.id.button_dropdown -> showBranchPopupMenu(v)
		}
	}

	override fun onLongClick(v: View): Boolean = when (v.id) {
		R.id.button_read -> {
			buttonTip?.get()?.remove()
			buttonTip = null
			val menu = PopupMenu(v.context, v)
			menu.inflate(R.menu.popup_read)
			menu.menu.findItem(R.id.action_forget)?.isVisible = viewModel.historyInfo.value.history != null
			menu.setOnMenuItemClickListener(this)
			menu.setForceShowIcon(true)
			menu.show()
			true
		}

		else -> false
	}

	override fun onMenuItemClick(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_forget -> {
				viewModel.removeFromHistory()
				true
			}

			R.id.action_pages_thumbs -> {
				val history = viewModel.historyInfo.value.history
				PagesThumbnailsSheet.show(
					fm = supportFragmentManager,
					manga = viewModel.manga.value ?: return false,
					chapterId = history?.chapterId
						?: viewModel.chapters.value.firstOrNull()?.chapter?.id
						?: return false,
					currentPage = history?.page ?: 0,
				)
				true
			}

			else -> false
		}
	}

	private fun onChaptersSheetStateChanged(isExpanded: Boolean) {
		val toolbar = viewBinding.toolbarChapters ?: return
		if (isAnimationsEnabled) {
			val transition = AutoTransition()
			transition.duration = getAnimationDuration(R.integer.config_shorterAnimTime)
			TransitionManager.beginDelayedTransition(toolbar, transition)
		}
		if (isExpanded) {
			toolbar.setNavigationIconSafe(materialR.drawable.abc_ic_clear_material)
		} else {
			toolbar.navigationIcon = null
		}
		toolbar.menuView?.isVisible = isExpanded
		viewBinding.buttonRead.isGone = isExpanded
	}

	private fun onMangaUpdated(manga: Manga) {
		title = manga.title
		val hasChapters = !manga.chapters.isNullOrEmpty()
		viewBinding.buttonRead.isEnabled = hasChapters
		invalidateOptionsMenu()
		showBottomSheet(manga.chapters != null)
		viewBinding.groupHeader?.isVisible = hasChapters
	}

	private fun onMangaRemoved(manga: Manga) {
		Toast.makeText(
			this,
			getString(R.string._s_deleted_from_local_storage, manga.title),
			Toast.LENGTH_SHORT,
		).show()
		finishAfterTransition()
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
		if (insets.bottom > 0) {
			window.setNavigationBarTransparentCompat(
				this,
				viewBinding.layoutBottom?.elevation ?: 0f,
				0.9f,
			)
		}
		viewBinding.cardChapters?.updateLayoutParams<MarginLayoutParams> {
			bottomMargin = insets.bottom + marginEnd
		}
		viewBinding.dragHandle?.updateLayoutParams<MarginLayoutParams> {
			bottomMargin = insets.top
		}
	}

	private fun onHistoryChanged(info: HistoryInfo) {
		with(viewBinding.buttonRead) {
			if (info.history != null) {
				setText(R.string._continue)
				setIconResource(R.drawable.ic_play)
			} else {
				setText(R.string.read)
				setIconResource(R.drawable.ic_play)
			}
		}
		val text = when {
			!info.isValid -> getString(R.string.loading_)
			info.currentChapter >= 0 -> getString(
				R.string.chapter_d_of_d,
				info.currentChapter + 1,
				info.totalChapters,
			)

			info.totalChapters == 0 -> getString(R.string.no_chapters)
			else -> resources.getQuantityString(
				R.plurals.chapters,
				info.totalChapters,
				info.totalChapters,
			)
		}
		viewBinding.toolbarChapters?.title = text
		viewBinding.textViewTitle?.text = text
	}

	private fun onNewChaptersChanged(count: Int) {
		val tab = viewBinding.tabs.getTabAt(0) ?: return
		if (count == 0) {
			tab.removeBadge()
		} else {
			val badge = tab.orCreateBadge
			badge.horizontalOffsetWithText = -resources.getDimensionPixelOffset(R.dimen.margin_small)
			badge.number = count
			badge.isVisible = true
		}
	}

	private fun showBranchPopupMenu(v: View) {
		val menu = PopupMenu(v.context, v)
		val branches = viewModel.branches.value
		for ((i, branch) in branches.withIndex()) {
			val title = buildSpannedString {
				if (branch.isCurrent) {
					inSpans(
						ImageSpan(
							this@DetailsActivity,
							R.drawable.ic_current_chapter,
							DynamicDrawableSpan.ALIGN_BASELINE,
						),
					) {
						append(' ')
					}
					append(' ')
				}
				append(branch.name ?: getString(R.string.system_default))
				append(' ')
				append(' ')
				inSpans(
					ForegroundColorSpan(
						v.context.getThemeColor(
							android.R.attr.textColorSecondary,
							Color.LTGRAY,
						),
					),
					RelativeSizeSpan(0.74f),
				) {
					append(branch.count.toString())
				}
			}
			menu.menu.add(Menu.NONE, Menu.NONE, i, title)
		}
		menu.setOnMenuItemClickListener {
			viewModel.setSelectedBranch(branches.getOrNull(it.order)?.name)
			true
		}
		menu.show()
	}

	private fun openReader() {
		val manga = viewModel.manga.value ?: return
		val chapterId = viewModel.historyInfo.value.history?.chapterId
		if (chapterId != null && manga.chapters?.none { x -> x.id == chapterId } == true) {
			Snackbar.make(viewBinding.containerDetails, R.string.chapter_is_missing, Snackbar.LENGTH_SHORT)
				.show()
		} else {
			startActivity(
				IntentBuilder(this)
					.manga(manga)
					.branch(viewModel.selectedBranchValue)
					.build(),
			)
		}
	}

	private fun initPager() {
		val adapter = DetailsPagerAdapter(this, settings)
		viewBinding.pager.recyclerView?.isNestedScrollingEnabled = false
		viewBinding.pager.offscreenPageLimit = 1
		viewBinding.pager.adapter = adapter
		TabLayoutMediator(viewBinding.tabs, viewBinding.pager, adapter).attach()
		viewBinding.pager.setCurrentItem(settings.defaultDetailsTab, false)
		viewBinding.tabs.isVisible = adapter.itemCount > 1
	}

	private fun showBottomSheet(isVisible: Boolean) {
		val view = viewBinding.layoutBottom ?: return
		if (view.isVisible == isVisible) return
		val transition = Slide(Gravity.BOTTOM)
		transition.addTarget(view)
		transition.interpolator = AccelerateDecelerateInterpolator()
		TransitionManager.beginDelayedTransition(viewBinding.root as ViewGroup, transition)
		view.isVisible = isVisible
	}

	private class PrefetchObserver(
		private val context: Context,
	) : FlowCollector<List<ChapterListItem>?> {

		private var isCalled = false

		override suspend fun emit(value: List<ChapterListItem>?) {
			if (value.isNullOrEmpty()) {
				return
			}
			if (!isCalled) {
				isCalled = true
				val item = value.find { it.isCurrent } ?: value.first()
				MangaPrefetchService.prefetchPages(context, item.chapter)
			}
		}
	}

	private fun showTip() {
		val tip = ButtonTip(viewBinding.root as ViewGroup, insetsDelegate, viewModel)
		tip.addToRoot()
		buttonTip = WeakReference(tip)
	}

	companion object {

		const val TIP_BUTTON = "btn_read"

		fun newIntent(context: Context, manga: Manga): Intent {
			return Intent(context, DetailsActivity::class.java)
				.putExtra(MangaIntent.KEY_MANGA, ParcelableManga(manga))
		}

		fun newIntent(context: Context, mangaId: Long): Intent {
			return Intent(context, DetailsActivity::class.java)
				.putExtra(MangaIntent.KEY_ID, mangaId)
		}
	}
}
