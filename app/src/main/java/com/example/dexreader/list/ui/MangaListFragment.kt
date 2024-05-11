package com.example.dexreader.list.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.annotation.CallSuper
import androidx.appcompat.view.ActionMode
import androidx.collection.ArraySet
import androidx.core.graphics.Insets
import androidx.core.view.isNotEmpty
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.GridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import coil.ImageLoader
import com.example.dexreader.R
import com.example.dexreader.core.exceptions.resolve.ExceptionResolver
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.prefs.ListMode
import com.example.dexreader.core.ui.BaseFragment
import com.example.dexreader.core.ui.list.FitHeightGridLayoutManager
import com.example.dexreader.core.ui.list.FitHeightLinearLayoutManager
import com.example.dexreader.core.ui.list.ListSelectionController
import com.example.dexreader.core.ui.list.PaginationScrollListener
import com.example.dexreader.core.ui.list.fastscroll.FastScroller
import com.example.dexreader.core.ui.util.ReversibleActionObserver
import com.example.dexreader.core.util.ShareHelper
import com.example.dexreader.core.util.ext.addMenuProvider
import com.example.dexreader.core.util.ext.measureHeight
import com.example.dexreader.core.util.ext.observe
import com.example.dexreader.core.util.ext.observeEvent
import com.example.dexreader.core.util.ext.resolveDp
import com.example.dexreader.core.util.ext.viewLifecycleScope
import com.example.dexreader.databinding.FragmentListBinding
import com.example.dexreader.details.ui.DetailsActivity
import com.example.dexreader.download.ui.worker.DownloadStartedObserver
import com.example.dexreader.favourites.ui.categories.select.FavoriteSheet
import com.example.dexreader.list.ui.adapter.ListItemType
import com.example.dexreader.list.ui.adapter.MangaListAdapter
import com.example.dexreader.list.ui.adapter.MangaListListener
import com.example.dexreader.list.ui.adapter.TypedListSpacingDecoration
import com.example.dexreader.list.ui.model.ListHeader
import com.example.dexreader.list.ui.model.ListModel
import com.example.dexreader.list.ui.model.MangaItemModel
import com.example.dexreader.list.ui.size.DynamicItemSizeResolver
import com.example.dexreader.main.ui.MainActivity
import com.example.dexreader.main.ui.owners.AppBarOwner
import com.example.dexreader.reader.ui.ReaderActivity.IntentBuilder
import com.example.dexreader.search.ui.MangaListActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.model.MangaTag
import javax.inject.Inject

@AndroidEntryPoint
abstract class MangaListFragment :
	BaseFragment<FragmentListBinding>(),
	PaginationScrollListener.Callback,
	MangaListListener,
	SwipeRefreshLayout.OnRefreshListener,
	ListSelectionController.Callback2,
	FastScroller.FastScrollListener {

	@Inject
	lateinit var coil: ImageLoader

	@Inject
	lateinit var settings: AppSettings

	private var listAdapter: MangaListAdapter? = null
	private var paginationListener: PaginationScrollListener? = null
	private var selectionController: ListSelectionController? = null
	private var spanResolver: MangaListSpanResolver? = null
	private val spanSizeLookup = SpanSizeLookup()
	open val isSwipeRefreshEnabled = true

	protected abstract val viewModel: MangaListViewModel

	protected val selectedItemsIds: Set<Long>
		get() = selectionController?.snapshot().orEmpty()

	protected val selectedItems: Set<Manga>
		get() = collectSelectedItems()

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = FragmentListBinding.inflate(inflater, container, false)

	override fun onViewBindingCreated(binding: FragmentListBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		listAdapter = onCreateAdapter()
		spanResolver = MangaListSpanResolver(binding.root.resources)
		selectionController = ListSelectionController(
			activity = requireActivity(),
			decoration = MangaSelectionDecoration(binding.root.context),
			registryOwner = this,
			callback = this,
		)
		paginationListener = PaginationScrollListener(4, this)
		with(binding.recyclerView) {
			setHasFixedSize(true)
			adapter = listAdapter
			checkNotNull(selectionController).attachToRecyclerView(binding.recyclerView)
			addItemDecoration(TypedListSpacingDecoration(context, false))
			addOnScrollListener(paginationListener!!)
			fastScroller.setFastScrollListener(this@MangaListFragment)
		}
		with(binding.swipeRefreshLayout) {
			setOnRefreshListener(this@MangaListFragment)
			isEnabled = isSwipeRefreshEnabled
		}
		addMenuProvider(MangaListMenuProvider(this))

		viewModel.listMode.observe(viewLifecycleOwner, ::onListModeChanged)
		viewModel.gridScale.observe(viewLifecycleOwner, ::onGridScaleChanged)
		viewModel.isLoading.observe(viewLifecycleOwner, ::onLoadingStateChanged)
		viewModel.content.observe(viewLifecycleOwner, ::onListChanged)
		viewModel.onActionDone.observeEvent(viewLifecycleOwner, ReversibleActionObserver(binding.recyclerView))
		viewModel.onDownloadStarted.observeEvent(viewLifecycleOwner, DownloadStartedObserver(binding.recyclerView))
	}

	override fun onDestroyView() {
		listAdapter = null
		paginationListener = null
		selectionController = null
		spanResolver = null
		spanSizeLookup.invalidateCache()
		super.onDestroyView()
	}

	override fun onItemClick(item: Manga, view: View) {
		if (selectionController?.onItemClick(item.id) != true) {
			if ((activity as? MangaListActivity)?.showPreview(item) != true) {
				startActivity(DetailsActivity.newIntent(context ?: return, item))
			}
		}
	}

	override fun onItemLongClick(item: Manga, view: View): Boolean {
		return selectionController?.onItemLongClick(item.id) ?: false
	}

	override fun onReadClick(manga: Manga, view: View) {
		if (selectionController?.onItemClick(manga.id) != true) {
			val intent = IntentBuilder(view.context).manga(manga).build()
			startActivity(intent)
		}
	}

	override fun onTagClick(manga: Manga, tag: MangaTag, view: View) {
		if (selectionController?.onItemClick(manga.id) != true) {
			val intent = MangaListActivity.newIntent(context ?: return, setOf(tag))
			startActivity(intent)
		}
	}

	@CallSuper
	override fun onRefresh() {
		requireViewBinding().swipeRefreshLayout.isRefreshing = true
		viewModel.onRefresh()
	}

	private suspend fun onListChanged(list: List<ListModel>) {
		listAdapter?.emit(list)
		spanSizeLookup.invalidateCache()
		viewBinding?.recyclerView?.let {
			paginationListener?.postInvalidate(it)
		}
	}

	private fun resolveException(e: Throwable) {
		if (ExceptionResolver.canResolve(e)) {
			viewLifecycleScope.launch {
				if (exceptionResolver.resolve(e)) {
					viewModel.onRetry()
				}
			}
		} else {
			viewModel.onRetry()
		}
	}

	@CallSuper
	protected open fun onLoadingStateChanged(isLoading: Boolean) {
		requireViewBinding().swipeRefreshLayout.isEnabled = requireViewBinding().swipeRefreshLayout.isRefreshing ||
			isSwipeRefreshEnabled && !isLoading
		if (!isLoading) {
			requireViewBinding().swipeRefreshLayout.isRefreshing = false
		}
	}

	protected open fun onCreateAdapter(): MangaListAdapter {
		return MangaListAdapter(
			coil = coil,
			lifecycleOwner = viewLifecycleOwner,
			listener = this,
			sizeResolver = DynamicItemSizeResolver(resources, settings, adjustWidth = false),
		)
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		val rv = requireViewBinding().recyclerView
		rv.updatePadding(
			bottom = insets.bottom + rv.paddingTop,
		)
		rv.fastScroller.updateLayoutParams<MarginLayoutParams> {
			bottomMargin = insets.bottom
		}
		if (activity is MainActivity) {
			val headerHeight = (activity as? AppBarOwner)?.appBar?.measureHeight() ?: insets.top
			requireViewBinding().swipeRefreshLayout.setProgressViewOffset(
				true,
				headerHeight + resources.resolveDp(-72),
				headerHeight + resources.resolveDp(10),
			)
		}
	}

	override fun onFilterClick(view: View?) = Unit

	override fun onEmptyActionClick() = Unit

	override fun onListHeaderClick(item: ListHeader, view: View) = Unit

	override fun onRetryClick(error: Throwable) {
		resolveException(error)
	}

	override fun onUpdateFilter(tags: Set<MangaTag>) {
		viewModel.onUpdateFilter(tags)
	}

	private fun onGridScaleChanged(scale: Float) {
		spanSizeLookup.invalidateCache()
		spanResolver?.setGridSize(scale, requireViewBinding().recyclerView)
	}

	private fun onListModeChanged(mode: ListMode) {
		spanSizeLookup.invalidateCache()
		with(requireViewBinding().recyclerView) {
			removeOnLayoutChangeListener(spanResolver)
			when (mode) {
				ListMode.LIST -> {
					layoutManager = FitHeightLinearLayoutManager(context)
				}

				ListMode.DETAILED_LIST -> {
					layoutManager = FitHeightLinearLayoutManager(context)
				}

				ListMode.GRID -> {
					layoutManager = FitHeightGridLayoutManager(context, checkNotNull(spanResolver).spanCount).also {
						it.spanSizeLookup = spanSizeLookup
					}
					addOnLayoutChangeListener(spanResolver)
				}
			}
		}
	}

	override fun onCreateActionMode(controller: ListSelectionController, mode: ActionMode, menu: Menu): Boolean {
		return menu.isNotEmpty()
	}

	override fun onActionItemClicked(controller: ListSelectionController, mode: ActionMode, item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_select_all -> {
				val ids = listAdapter?.items?.mapNotNull {
					(it as? MangaItemModel)?.id
				} ?: return false
				selectionController?.addAll(ids)
				true
			}

			R.id.action_favourite -> {
				FavoriteSheet.show(childFragmentManager, selectedItems)
				mode.finish()
				true
			}

			R.id.action_save -> {
				viewModel.download(selectedItems)
				mode.finish()
				true
			}

			else -> false
		}
	}

	override fun onSelectionChanged(controller: ListSelectionController, count: Int) {
		viewBinding?.recyclerView?.invalidateItemDecorations()
	}

	override fun onFastScrollStart(fastScroller: FastScroller) {
		(activity as? AppBarOwner)?.appBar?.setExpanded(false, true)
		requireViewBinding().swipeRefreshLayout.isEnabled = false
	}

	override fun onFastScrollStop(fastScroller: FastScroller) {
		requireViewBinding().swipeRefreshLayout.isEnabled = isSwipeRefreshEnabled
	}

	private fun collectSelectedItems(): Set<Manga> {
		val checkedIds = selectionController?.peekCheckedIds() ?: return emptySet()
		val items = listAdapter?.items ?: return emptySet()
		val result = ArraySet<Manga>(checkedIds.size)
		for (item in items) {
			if (item is MangaItemModel && item.id in checkedIds) {
				result.add(item.manga)
			}
		}
		return result
	}

	private inner class SpanSizeLookup : GridLayoutManager.SpanSizeLookup() {

		init {
			isSpanIndexCacheEnabled = true
			isSpanGroupIndexCacheEnabled = true
		}

		override fun getSpanSize(position: Int): Int {
			val total = (viewBinding?.recyclerView?.layoutManager as? GridLayoutManager)?.spanCount ?: return 1
			return when (listAdapter?.getItemViewType(position)) {
				ListItemType.MANGA_GRID.ordinal -> 1
				else -> total
			}
		}

		fun invalidateCache() {
			invalidateSpanGroupIndexCache()
			invalidateSpanIndexCache()
		}
	}
}
