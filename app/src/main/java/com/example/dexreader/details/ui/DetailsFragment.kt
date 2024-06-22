package com.example.dexreader.details.ui

import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.text.method.LinkMovementMethodCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.util.CoilUtils
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import com.example.dexreader.R
import com.example.dexreader.bookmarks.domain.Bookmark
import com.example.dexreader.bookmarks.ui.adapter.BookmarksAdapter
import com.example.dexreader.bookmarks.ui.sheet.BookmarksSheet
import com.example.dexreader.core.model.countChaptersByBranch
import com.example.dexreader.core.model.iconResId
import com.example.dexreader.core.model.titleResId
import com.example.dexreader.core.ui.BaseFragment
import com.example.dexreader.core.ui.BaseListAdapter
import com.example.dexreader.core.ui.image.CoverSizeResolver
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.core.ui.list.decor.SpacingItemDecoration
import com.example.dexreader.core.ui.widgets.ChipsView
import com.example.dexreader.core.util.FileSize
import com.example.dexreader.core.util.ext.crossfade
import com.example.dexreader.core.util.ext.drawableTop
import com.example.dexreader.core.util.ext.enqueueWith
import com.example.dexreader.core.util.ext.ifNullOrEmpty
import com.example.dexreader.core.util.ext.isTextTruncated
import com.example.dexreader.core.util.ext.observe
import com.example.dexreader.core.util.ext.parentView
import com.example.dexreader.core.util.ext.resolveDp
import com.example.dexreader.core.util.ext.scaleUpActivityOptionsOf
import com.example.dexreader.core.util.ext.showOrHide
import com.example.dexreader.core.util.ext.textAndVisible
import com.example.dexreader.databinding.FragmentDetailsBinding
import com.example.dexreader.details.data.ReadingTime
import com.example.dexreader.details.ui.model.ChapterListItem
import com.example.dexreader.details.ui.model.HistoryInfo
import com.example.dexreader.details.ui.related.RelatedMangaActivity

import com.example.dexreader.history.data.PROGRESS_NONE
import com.example.dexreader.image.ui.ImageActivity
import com.example.dexreader.list.domain.ListExtraProvider
import com.example.dexreader.list.ui.adapter.ListItemType
import com.example.dexreader.list.ui.adapter.mangaGridItemAD
import com.example.dexreader.list.ui.model.ListModel
import com.example.dexreader.list.ui.model.MangaItemModel
import com.example.dexreader.list.ui.size.StaticItemSizeResolver
import com.example.dexreader.local.ui.info.LocalInfoDialog
import com.example.dexreader.main.ui.owners.NoModalBottomSheetOwner
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.model.MangaSource
import org.example.dexreader.parsers.model.MangaTag
import com.example.dexreader.search.ui.MangaListActivity
import com.example.dexreader.search.ui.SearchActivity
import com.example.dexreader.reader.ui.ReaderActivity
import javax.inject.Inject

@AndroidEntryPoint
class DetailsFragment :
	BaseFragment<FragmentDetailsBinding>(),
	View.OnClickListener,
	ChipsView.OnChipClickListener,
	OnListItemClickListener<Bookmark>, ViewTreeObserver.OnDrawListener, View.OnLayoutChangeListener {

	@Inject
	lateinit var coil: ImageLoader

	@Inject
	lateinit var tagHighlighter: ListExtraProvider

	private val viewModel by activityViewModels<DetailsViewModel>()

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = FragmentDetailsBinding.inflate(inflater, container, false)

	override fun onViewBindingCreated(binding: FragmentDetailsBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		binding.textViewAuthor.setOnClickListener(this)
		binding.imageViewCover.setOnClickListener(this)
		binding.buttonDescriptionMore.setOnClickListener(this)
		binding.buttonBookmarksMore.setOnClickListener(this)
		binding.infoLayout.textViewSource.setOnClickListener(this)
		binding.infoLayout.textViewSize.setOnClickListener(this)
		binding.textViewDescription.addOnLayoutChangeListener(this)
		binding.textViewDescription.viewTreeObserver.addOnDrawListener(this)
		binding.textViewDescription.movementMethod = LinkMovementMethodCompat.getInstance()
		binding.chipsTags.onChipClickListener = this
		TitleScrollCoordinator(binding.textViewTitle).attach(binding.scrollView)
		viewModel.manga.filterNotNull().observe(viewLifecycleOwner, ::onMangaUpdated)
		viewModel.isLoading.observe(viewLifecycleOwner, ::onLoadingStateChanged)
		viewModel.historyInfo.observe(viewLifecycleOwner, ::onHistoryChanged)
		viewModel.bookmarks.observe(viewLifecycleOwner, ::onBookmarksChanged)
		viewModel.description.observe(viewLifecycleOwner, ::onDescriptionChanged)
		viewModel.localSize.observe(viewLifecycleOwner, ::onLocalSizeChanged)
		viewModel.chapters.observe(viewLifecycleOwner, ::onChaptersChanged)
	}

	override fun onItemClick(item: Bookmark, view: View) {
		startActivity(
			ReaderActivity.IntentBuilder(view.context).bookmark(item).build(),
		)
	}

	override fun onItemLongClick(item: Bookmark, view: View): Boolean {
		val menu = PopupMenu(view.context, view)
		menu.inflate(R.menu.popup_bookmark)
		menu.setOnMenuItemClickListener { menuItem ->
			when (menuItem.itemId) {
				R.id.action_remove -> viewModel.removeBookmark(item)
			}
			true
		}
		menu.show()
		return true
	}

	override fun onDraw() {
		viewBinding?.run {
			buttonDescriptionMore.isVisible = textViewDescription.maxLines == Int.MAX_VALUE ||
				textViewDescription.isTextTruncated
		}
	}

	override fun onLayoutChange(
		v: View?,
		left: Int,
		top: Int,
		right: Int,
		bottom: Int,
		oldLeft: Int,
		oldTop: Int,
		oldRight: Int,
		oldBottom: Int
	) {
		with(viewBinding ?: return) {
			buttonDescriptionMore.isVisible = textViewDescription.isTextTruncated
		}
	}

	private fun onMangaUpdated(manga: Manga) {
		with(requireViewBinding()) {
			// Main
			loadCover(manga)
			textViewTitle.text = manga.title
			textViewSubtitle.textAndVisible = manga.altTitle
			textViewAuthor.textAndVisible = manga.author
			if (manga.hasRating) {
				ratingBar.rating = manga.rating * ratingBar.numStars
				ratingBar.isVisible = true
			} else {
				ratingBar.isVisible = false
			}

			infoLayout.textViewState.apply {
				manga.state?.let { state ->
					textAndVisible = resources.getString(state.titleResId)
					drawableTop = ContextCompat.getDrawable(context, state.iconResId)
				} ?: run {
					isVisible = false
				}
			}
			if (manga.source == MangaSource.LOCAL || manga.source == MangaSource.DUMMY) {
				infoLayout.textViewSource.isVisible = false
			} else {
				infoLayout.textViewSource.text = manga.source.title
				infoLayout.textViewSource.isVisible = true
			}

			infoLayout.textViewNsfw.isVisible = manga.isNsfw

			// Chips
			bindTags(manga)
		}
	}

	private fun onChaptersChanged(chapters: List<ChapterListItem>?) {
		val infoLayout = requireViewBinding().infoLayout
		if (chapters.isNullOrEmpty()) {
			infoLayout.textViewChapters.isVisible = false
		} else {
			val count = chapters.countChaptersByBranch()
			infoLayout.textViewChapters.isVisible = true
			val chaptersText = resources.getQuantityString(R.plurals.chapters, count, count)
			infoLayout.textViewChapters.text = chaptersText
		}
	}

	private fun onDescriptionChanged(description: CharSequence?) {
		val tv = requireViewBinding().textViewDescription
		if (description.isNullOrBlank()) {
			tv.setText(R.string.no_description)
		} else {
			tv.text = description
		}
	}

	private fun onLocalSizeChanged(size: Long) {
		val textView = requireViewBinding().infoLayout.textViewSize
		if (size == 0L) {
			textView.isVisible = false
		} else {
			textView.text = FileSize.BYTES.format(textView.context, size)
			textView.isVisible = true
		}
	}

	private fun onHistoryChanged(history: HistoryInfo) {
		requireViewBinding().progressView.setPercent(history.history?.percent ?: PROGRESS_NONE, animate = true)
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		requireViewBinding().progressBar.showOrHide(isLoading)
	}

	private fun onBookmarksChanged(bookmarks: List<Bookmark>) {
		var adapter = requireViewBinding().recyclerViewBookmarks.adapter as? BookmarksAdapter
		requireViewBinding().groupBookmarks.isGone = bookmarks.isEmpty()
		if (adapter != null) {
			adapter.items = bookmarks
		} else {
			adapter = BookmarksAdapter(coil, viewLifecycleOwner, this)
			adapter.items = bookmarks
			requireViewBinding().recyclerViewBookmarks.adapter = adapter
			val spacing = resources.getDimensionPixelOffset(R.dimen.bookmark_list_spacing)
			requireViewBinding().recyclerViewBookmarks.addItemDecoration(SpacingItemDecoration(spacing))
		}
	}

	override fun onClick(v: View) {
		val manga = viewModel.manga.value ?: return
		when (v.id) {
			R.id.textView_author -> {
				startActivity(
					SearchActivity.newIntent(
						context = v.context,
						source = manga.source,
						query = manga.author ?: return,
					),
				)
			}

			R.id.textView_source -> {
				startActivity(
					MangaListActivity.newIntent(
						context = v.context,
						source = manga.source,
					),
				)
			}

			R.id.textView_size -> {
				LocalInfoDialog.show(parentFragmentManager, manga)
			}

			R.id.imageView_cover -> {
				startActivity(
					ImageActivity.newIntent(
						v.context,
						manga.largeCoverUrl.ifNullOrEmpty { manga.coverUrl },
						manga.source,
					),
					scaleUpActivityOptionsOf(v),
				)
			}

			R.id.button_description_more -> {
				val tv = requireViewBinding().textViewDescription
				TransitionManager.beginDelayedTransition(tv.parentView)
				if (tv.maxLines in 1 until Integer.MAX_VALUE) {
					tv.maxLines = Integer.MAX_VALUE
				} else {
					tv.maxLines = resources.getInteger(R.integer.details_description_lines)
				}
			}

			R.id.button_bookmarks_more -> {
				BookmarksSheet.show(parentFragmentManager, manga)
			}
		}
	}

	override fun onChipClick(chip: Chip, data: Any?) {
		val tag = data as? MangaTag ?: return
		startActivity(MangaListActivity.newIntent(requireContext(), setOf(tag)))
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		requireViewBinding().root.updatePadding(
			bottom = (
				(activity as? NoModalBottomSheetOwner)?.getBottomSheetCollapsedHeight()
					?.plus(insets.bottom)?.plus(resources.resolveDp(16))
				)
				?: insets.bottom,
		)
	}

	private fun bindTags(manga: Manga) {
		requireViewBinding().chipsTags.setChips(
			manga.tags.map { tag ->
				ChipsView.ChipModel(
					title = tag.title,
					tint = tagHighlighter.getTagTint(tag),
					icon = 0,
					data = tag,
					isCheckable = false,
					isChecked = false,
				)
			},
		)
	}

	private fun loadCover(manga: Manga) {
		val imageUrl = manga.largeCoverUrl.ifNullOrEmpty { manga.coverUrl }
		val lastResult = CoilUtils.result(requireViewBinding().imageViewCover)
		if (lastResult is SuccessResult && lastResult.request.data == imageUrl) {
			return
		}
		val request = ImageRequest.Builder(context ?: return)
			.target(requireViewBinding().imageViewCover)
			.size(CoverSizeResolver(requireViewBinding().imageViewCover))
			.data(imageUrl)
			.tag(manga.source)
			.crossfade(requireContext())
			.lifecycle(viewLifecycleOwner)
			.placeholderMemoryCacheKey(manga.coverUrl)
		val previousDrawable = lastResult?.drawable
		if (previousDrawable != null) {
			request.fallback(previousDrawable)
				.placeholder(previousDrawable)
				.error(previousDrawable)
		} else {
			request.fallback(R.drawable.ic_placeholder)
				.placeholder(R.drawable.ic_placeholder)
				.error(R.drawable.ic_error_placeholder)
		}
		request.enqueueWith(coil)
	}
}
