package com.example.dexreader.search.ui.multi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import coil.ImageLoader
import com.example.dexreader.R
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.ui.BaseActivity
import com.example.dexreader.core.ui.list.ListSelectionController
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.core.util.ShareHelper
import com.example.dexreader.core.util.ext.invalidateNestedItemDecorations
import com.example.dexreader.core.util.ext.observe
import com.example.dexreader.core.util.ext.observeEvent
import com.example.dexreader.databinding.ActivitySearchMultiBinding
import com.example.dexreader.details.ui.DetailsActivity
import com.example.dexreader.download.ui.worker.DownloadStartedObserver
import com.example.dexreader.favourites.ui.categories.select.FavoriteSheet
import com.example.dexreader.list.ui.MangaSelectionDecoration
import com.example.dexreader.list.ui.adapter.MangaListListener
import com.example.dexreader.list.ui.adapter.TypedListSpacingDecoration
import com.example.dexreader.list.ui.model.ListHeader
import com.example.dexreader.list.ui.size.DynamicItemSizeResolver
import com.example.dexreader.search.ui.MangaListActivity
import com.example.dexreader.search.ui.SearchActivity
import com.example.dexreader.search.ui.multi.adapter.MultiSearchAdapter
import dagger.hilt.android.AndroidEntryPoint
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.model.MangaTag
import com.example.dexreader.reader.ui.ReaderActivity.IntentBuilder
import javax.inject.Inject

@AndroidEntryPoint
class MultiSearchActivity :
	BaseActivity<ActivitySearchMultiBinding>(),
	MangaListListener,
	ListSelectionController.Callback2 {

	@Inject
	lateinit var coil: ImageLoader

	@Inject
	lateinit var settings: AppSettings

	private val viewModel by viewModels<MultiSearchViewModel>()
	private lateinit var adapter: MultiSearchAdapter
	private lateinit var selectionController: ListSelectionController

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivitySearchMultiBinding.inflate(layoutInflater))
		title = viewModel.query

		val itemCLickListener = OnListItemClickListener<MultiSearchListModel> { item, view ->
			startActivity(SearchActivity.newIntent(view.context, item.source, viewModel.query))
		}
		val sizeResolver = DynamicItemSizeResolver(resources, settings, adjustWidth = true)
		val selectionDecoration = MangaSelectionDecoration(this)
		selectionController = ListSelectionController(
			activity = this,
			decoration = selectionDecoration,
			registryOwner = this,
			callback = this,
		)
		adapter = MultiSearchAdapter(
			lifecycleOwner = this,
			coil = coil,
			listener = this,
			itemClickListener = itemCLickListener,
			sizeResolver = sizeResolver,
			selectionDecoration = selectionDecoration,
		)
		viewBinding.recyclerView.adapter = adapter
		viewBinding.recyclerView.setHasFixedSize(true)
		viewBinding.recyclerView.addItemDecoration(TypedListSpacingDecoration(this, true))

		supportActionBar?.run {
			setDisplayHomeAsUpEnabled(true)
			setSubtitle(R.string.search_results)
		}

		viewModel.list.observe(this) { adapter.items = it }
		viewModel.onDownloadStarted.observeEvent(this, DownloadStartedObserver(viewBinding.recyclerView))
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
		viewBinding.recyclerView.updatePadding(
			bottom = insets.bottom + viewBinding.recyclerView.paddingTop,
		)
	}

	override fun onItemClick(item: Manga, view: View) {
		if (!selectionController.onItemClick(item.id)) {
			val intent = DetailsActivity.newIntent(this, item)
			startActivity(intent)
		}
	}

	override fun onItemLongClick(item: Manga, view: View): Boolean {
		return selectionController.onItemLongClick(item.id)
	}

	override fun onReadClick(manga: Manga, view: View) {
		if (!selectionController.onItemClick(manga.id)) {
			val intent = IntentBuilder(this).manga(manga).build()
			startActivity(intent)
		}
	}

	override fun onTagClick(manga: Manga, tag: MangaTag, view: View) {
		if (!selectionController.onItemClick(manga.id)) {
			val intent = MangaListActivity.newIntent(this, setOf(tag))
			startActivity(intent)
		}
	}

	override fun onRetryClick(error: Throwable) {
		viewModel.retry()
	}

	override fun onUpdateFilter(tags: Set<MangaTag>) = Unit

	override fun onFilterClick(view: View?) = Unit

	override fun onEmptyActionClick() = Unit

	override fun onListHeaderClick(item: ListHeader, view: View) = Unit

	override fun onSelectionChanged(controller: ListSelectionController, count: Int) {
		viewBinding.recyclerView.invalidateNestedItemDecorations()
	}

	override fun onCreateActionMode(controller: ListSelectionController, mode: ActionMode, menu: Menu): Boolean {
		mode.menuInflater.inflate(R.menu.mode_remote, menu)
		return true
	}

	override fun onActionItemClicked(controller: ListSelectionController, mode: ActionMode, item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_favourite -> {
				FavoriteSheet.show(supportFragmentManager, collectSelectedItems())
				mode.finish()
				true
			}

			R.id.action_save -> {
				viewModel.download(collectSelectedItems())
				mode.finish()
				true
			}

			else -> false
		}
	}

	private fun collectSelectedItems(): Set<Manga> {
		return viewModel.getItems(selectionController.peekCheckedIds())
	}

	companion object {

		const val EXTRA_QUERY = "query"

		fun newIntent(context: Context, query: String) =
			Intent(context, MultiSearchActivity::class.java)
				.putExtra(EXTRA_QUERY, query)
	}
}
