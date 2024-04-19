package com.example.dexreader.bookmarks.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
import androidx.core.graphics.Insets
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import coil.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import com.example.dexreader.R
import com.example.dexreader.bookmarks.domain.Bookmark
import com.example.dexreader.bookmarks.ui.sheet.BookmarksAdapter
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.ui.BaseFragment
import com.example.dexreader.core.ui.list.ListSelectionController
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.core.ui.list.fastscroll.FastScroller
import com.example.dexreader.core.ui.util.ReversibleActionObserver
import com.example.dexreader.core.util.ext.observe
import com.example.dexreader.core.util.ext.observeEvent
import com.example.dexreader.databinding.FragmentListSimpleBinding
import com.example.dexreader.details.ui.DetailsActivity
import com.example.dexreader.list.ui.MangaListSpanResolver
import com.example.dexreader.list.ui.adapter.ListHeaderClickListener
import com.example.dexreader.list.ui.adapter.ListItemType
import com.example.dexreader.list.ui.adapter.ListStateHolderListener
import com.example.dexreader.list.ui.adapter.TypedListSpacingDecoration
import com.example.dexreader.list.ui.model.ListHeader
import com.example.dexreader.main.ui.owners.AppBarOwner
import org.example.dexreader.parsers.model.Manga
import com.example.dexreader.reader.ui.ReaderActivity
import javax.inject.Inject

@AndroidEntryPoint
class BookmarksFragment :
	BaseFragment<FragmentListSimpleBinding>(),
	ListStateHolderListener,
	OnListItemClickListener<Bookmark>,
	ListSelectionController.Callback2,
	FastScroller.FastScrollListener, ListHeaderClickListener {

	@Inject
	lateinit var coil: ImageLoader

	@Inject
	lateinit var settings: AppSettings

	private val viewModel by viewModels<BookmarksViewModel>()
	private var bookmarksAdapter: BookmarksAdapter? = null
	private var selectionController: ListSelectionController? = null

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	): FragmentListSimpleBinding {
		return FragmentListSimpleBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(
		binding: FragmentListSimpleBinding,
		savedInstanceState: Bundle?,
	) {
		super.onViewBindingCreated(binding, savedInstanceState)
		selectionController = ListSelectionController(
			activity = requireActivity(),
			decoration = BookmarksSelectionDecoration(binding.root.context),
			registryOwner = this,
			callback = this,
		)
		bookmarksAdapter = BookmarksAdapter(
			lifecycleOwner = viewLifecycleOwner,
			coil = coil,
			clickListener = this,
			headerClickListener = this,
		)
		val spanSizeLookup = SpanSizeLookup()
		with(binding.recyclerView) {
			setHasFixedSize(true)
			val spanResolver = MangaListSpanResolver(resources)
			addItemDecoration(TypedListSpacingDecoration(context, false))
			adapter = bookmarksAdapter
			addOnLayoutChangeListener(spanResolver)
			spanResolver.setGridSize(settings.gridSize / 100f, this)
			val lm = GridLayoutManager(context, spanResolver.spanCount)
			lm.spanSizeLookup = spanSizeLookup
			layoutManager = lm
			selectionController?.attachToRecyclerView(this)
		}
		viewModel.content.observe(viewLifecycleOwner) {
			bookmarksAdapter?.setItems(it, spanSizeLookup)
		}
		viewModel.onActionDone.observeEvent(viewLifecycleOwner, ReversibleActionObserver(binding.recyclerView))
	}

	override fun onDestroyView() {
		super.onDestroyView()
		bookmarksAdapter = null
		selectionController = null
	}

	override fun onItemClick(item: Bookmark, view: View) {
		if (selectionController?.onItemClick(item.pageId) != true) {
			val intent = ReaderActivity.IntentBuilder(view.context)
				.bookmark(item)
				.build()
			startActivity(intent)
		}
	}

	override fun onListHeaderClick(item: ListHeader, view: View) {
		val manga = item.payload as? Manga ?: return
		startActivity(DetailsActivity.newIntent(view.context, manga))
	}

	override fun onItemLongClick(item: Bookmark, view: View): Boolean {
		return selectionController?.onItemLongClick(item.pageId) ?: false
	}

	override fun onRetryClick(error: Throwable) = Unit

	override fun onEmptyActionClick() = Unit

	override fun onFastScrollStart(fastScroller: FastScroller) {
		(activity as? AppBarOwner)?.appBar?.setExpanded(false, true)
	}

	override fun onFastScrollStop(fastScroller: FastScroller) = Unit

	override fun onSelectionChanged(controller: ListSelectionController, count: Int) {
		requireViewBinding().recyclerView.invalidateItemDecorations()
	}

	override fun onCreateActionMode(
		controller: ListSelectionController,
		mode: ActionMode,
		menu: Menu,
	): Boolean {
		mode.menuInflater.inflate(R.menu.mode_bookmarks, menu)
		return true
	}

	override fun onActionItemClicked(
		controller: ListSelectionController,
		mode: ActionMode,
		item: MenuItem,
	): Boolean {
		return when (item.itemId) {
			R.id.action_remove -> {
				val ids = selectionController?.snapshot() ?: return false
				viewModel.removeBookmarks(ids)
				mode.finish()
				true
			}

			else -> false
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		val rv = requireViewBinding().recyclerView
		rv.updatePadding(
			bottom = insets.bottom + rv.paddingTop,
		)
		rv.fastScroller.updateLayoutParams<ViewGroup.MarginLayoutParams> {
			bottomMargin = insets.bottom
		}
	}

	private inner class SpanSizeLookup : GridLayoutManager.SpanSizeLookup(), Runnable {

		init {
			isSpanIndexCacheEnabled = true
			isSpanGroupIndexCacheEnabled = true
		}

		override fun getSpanSize(position: Int): Int {
			val total = (viewBinding?.recyclerView?.layoutManager as? GridLayoutManager)?.spanCount
				?: return 1
			return when (bookmarksAdapter?.getItemViewType(position)) {
				ListItemType.PAGE_THUMB.ordinal -> 1
				else -> total
			}
		}

		override fun run() {
			invalidateSpanGroupIndexCache()
			invalidateSpanIndexCache()
		}
	}

	companion object {

		@Deprecated(
			"", ReplaceWith(
				"BookmarksFragment()",
				"com.example.dexreader.bookmarks.ui.BookmarksFragment"
			)
		)
		fun newInstance() = BookmarksFragment()
	}
}
