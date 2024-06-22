package com.example.dexreader.reader.ui.thumbnails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import com.example.dexreader.R
import com.example.dexreader.core.model.parcelable.ParcelableManga
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.ui.list.BoundsScrollListener
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.core.ui.sheet.AdaptiveSheetBehavior
import com.example.dexreader.core.ui.sheet.AdaptiveSheetCallback
import com.example.dexreader.core.ui.sheet.BaseAdaptiveSheet
import com.example.dexreader.core.util.RecyclerViewScrollCallback
import com.example.dexreader.core.util.ext.observe
import com.example.dexreader.core.util.ext.plus
import com.example.dexreader.core.util.ext.showDistinct
import com.example.dexreader.core.util.ext.showOrHide
import com.example.dexreader.core.util.ext.withArgs
import com.example.dexreader.databinding.SheetPagesBinding
import com.example.dexreader.list.ui.MangaListSpanResolver
import com.example.dexreader.list.ui.adapter.ListItemType
import com.example.dexreader.list.ui.adapter.TypedListSpacingDecoration
import com.example.dexreader.list.ui.model.ListModel
import com.example.dexreader.reader.ui.thumbnails.adapter.PageThumbnailAdapter
import com.example.dexreader.reader.ui.ReaderActivity.IntentBuilder
import com.example.dexreader.reader.ui.ReaderState
import dagger.hilt.android.AndroidEntryPoint
import org.example.dexreader.parsers.model.Manga
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class PagesThumbnailsSheet :
	BaseAdaptiveSheet<SheetPagesBinding>(),
	AdaptiveSheetCallback,
	OnListItemClickListener<PageThumbnail> {

	private val viewModel by viewModels<PagesThumbnailsViewModel>()

	@Inject
	lateinit var coil: ImageLoader

	@Inject
	lateinit var settings: AppSettings

	private var thumbnailsAdapter: PageThumbnailAdapter? = null
	private var spanResolver: MangaListSpanResolver? = null
	private var scrollListener: ScrollListener? = null

	private val spanSizeLookup = SpanSizeLookup()
	private val listCommitCallback = Runnable {
		spanSizeLookup.invalidateCache()
	}

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): SheetPagesBinding {
		return SheetPagesBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: SheetPagesBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		addSheetCallback(this)
		spanResolver = MangaListSpanResolver(binding.root.resources)
		thumbnailsAdapter = PageThumbnailAdapter(
			coil = coil,
			lifecycleOwner = viewLifecycleOwner,
			clickListener = this@PagesThumbnailsSheet,
		)
		with(binding.recyclerView) {
			addItemDecoration(TypedListSpacingDecoration(context, false))
			adapter = thumbnailsAdapter
			addOnLayoutChangeListener(spanResolver)
			spanResolver?.setGridSize(settings.gridSize / 100f, this)
			addOnScrollListener(ScrollListener().also { scrollListener = it })
			(layoutManager as GridLayoutManager).spanSizeLookup = spanSizeLookup
		}
		viewModel.thumbnails.observe(viewLifecycleOwner, ::onThumbnailsChanged)
		viewModel.branch.observe(viewLifecycleOwner, ::updateTitle)
		viewModel.isLoading.observe(viewLifecycleOwner) { binding.progressBar.showOrHide(it) }
	}

	override fun onDestroyView() {
		spanResolver = null
		scrollListener = null
		thumbnailsAdapter = null
		spanSizeLookup.invalidateCache()
		super.onDestroyView()
	}

	override fun onItemClick(item: PageThumbnail, view: View) {
		val listener = (parentFragment as? OnPageSelectListener) ?: (activity as? OnPageSelectListener)
		if (listener != null) {
			listener.onPageSelected(item.page)
		} else {
			val state = ReaderState(item.page.chapterId, item.page.index, 0)
			val intent = IntentBuilder(view.context).manga(viewModel.manga).state(state).build()
			startActivity(intent)
		}
		dismiss()
	}

	override fun onStateChanged(sheet: View, newState: Int) {
		viewBinding?.recyclerView?.isFastScrollerEnabled = newState == AdaptiveSheetBehavior.STATE_EXPANDED
	}

	private fun updateTitle(branch: String?) {
		val mangaName = viewModel.manga.title
		viewBinding?.headerBar?.title = if (branch != null) {
			getString(R.string.manga_branch_title_template, mangaName, branch)
		} else {
			mangaName
		}
	}

	private fun onThumbnailsChanged(list: List<ListModel>) {
		val adapter = thumbnailsAdapter ?: return
		if (adapter.itemCount == 0) {
			var position = list.indexOfFirst { it is PageThumbnail && it.isCurrent }
			if (position > 0) {
				val spanCount = spanResolver?.spanCount ?: 0
				val offset = if (position > spanCount + 1) {
					(resources.getDimensionPixelSize(R.dimen.manga_list_details_item_height) * 0.6).roundToInt()
				} else {
					position = 0
					0
				}
				val scrollCallback = RecyclerViewScrollCallback(requireViewBinding().recyclerView, position, offset)
				adapter.setItems(list, listCommitCallback + scrollCallback)
			} else {
				adapter.setItems(list, listCommitCallback)
			}
		} else {
			adapter.setItems(list, listCommitCallback)
		}
	}

	private inner class ScrollListener : BoundsScrollListener(3, 3) {

		override fun onScrolledToStart(recyclerView: RecyclerView) {
			viewModel.loadPrevChapter()
		}

		override fun onScrolledToEnd(recyclerView: RecyclerView) {
			viewModel.loadNextChapter()
		}
	}

	private inner class SpanSizeLookup : GridLayoutManager.SpanSizeLookup() {

		init {
			isSpanIndexCacheEnabled = true
			isSpanGroupIndexCacheEnabled = true
		}

		override fun getSpanSize(position: Int): Int {
			val total = (viewBinding?.recyclerView?.layoutManager as? GridLayoutManager)?.spanCount ?: return 1
			return when (thumbnailsAdapter?.getItemViewType(position)) {
				ListItemType.PAGE_THUMB.ordinal -> 1
				else -> total
			}
		}

		fun invalidateCache() {
			invalidateSpanGroupIndexCache()
			invalidateSpanIndexCache()
		}
	}

	companion object {

		const val ARG_MANGA = "manga"
		const val ARG_CURRENT_PAGE = "current"
		const val ARG_CHAPTER_ID = "chapter_id"

		private const val TAG = "PagesThumbnailsSheet"

		fun show(fm: FragmentManager, manga: Manga, chapterId: Long, currentPage: Int = -1) {
			PagesThumbnailsSheet().withArgs(3) {
				putParcelable(ARG_MANGA, ParcelableManga(manga))
				putLong(ARG_CHAPTER_ID, chapterId)
				putInt(ARG_CURRENT_PAGE, currentPage)
			}.showDistinct(fm, TAG)
		}
	}
}
