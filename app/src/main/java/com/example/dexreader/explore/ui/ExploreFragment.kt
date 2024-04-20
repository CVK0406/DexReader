package com.example.dexreader.explore.ui

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import com.example.dexreader.R
import com.example.dexreader.bookmarks.ui.BookmarksActivity
import com.example.dexreader.core.ui.BaseFragment
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.core.ui.util.RecyclerViewOwner
import com.example.dexreader.core.ui.util.ReversibleActionObserver
import com.example.dexreader.core.ui.util.SpanSizeResolver
import com.example.dexreader.core.ui.widgets.TipView
import com.example.dexreader.core.util.ext.addMenuProvider
import com.example.dexreader.core.util.ext.observe
import com.example.dexreader.core.util.ext.observeEvent
import com.example.dexreader.databinding.FragmentExploreBinding
import com.example.dexreader.details.ui.DetailsActivity
import com.example.dexreader.download.ui.list.DownloadsActivity
import com.example.dexreader.explore.ui.adapter.ExploreAdapter
import com.example.dexreader.explore.ui.adapter.ExploreListEventListener
import com.example.dexreader.explore.ui.model.MangaSourceItem
import com.example.dexreader.list.ui.adapter.TypedListSpacingDecoration
import com.example.dexreader.list.ui.model.ListHeader
import com.example.dexreader.list.ui.model.TipModel
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.model.MangaSource
import com.example.dexreader.search.ui.MangaListActivity
import com.example.dexreader.settings.SettingsActivity
import com.example.dexreader.settings.newsources.NewSourcesDialogFragment
import com.example.dexreader.settings.sources.catalog.SourcesCatalogActivity
import javax.inject.Inject

@AndroidEntryPoint
class ExploreFragment :
	BaseFragment<FragmentExploreBinding>(),
	RecyclerViewOwner,
	ExploreListEventListener,
	OnListItemClickListener<MangaSourceItem>, TipView.OnButtonClickListener {

	@Inject
	lateinit var coil: ImageLoader

	private val viewModel by viewModels<ExploreViewModel>()
	private var exploreAdapter: ExploreAdapter? = null

	override val recyclerView: RecyclerView
		get() = requireViewBinding().recyclerView

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentExploreBinding {
		return FragmentExploreBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: FragmentExploreBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		exploreAdapter = ExploreAdapter(coil, viewLifecycleOwner, this, this, this) { manga, view ->
			startActivity(DetailsActivity.newIntent(view.context, manga))
		}
		with(binding.recyclerView) {
			adapter = exploreAdapter
			setHasFixedSize(true)
			SpanSizeResolver(this, resources.getDimensionPixelSize(R.dimen.explore_grid_width)).attach()
			addItemDecoration(TypedListSpacingDecoration(context, false))
		}
		addMenuProvider(ExploreMenuProvider(binding.root.context))
		viewModel.content.observe(viewLifecycleOwner) {
			exploreAdapter?.items = it
		}
		viewModel.onOpenManga.observeEvent(viewLifecycleOwner, ::onOpenManga)
		viewModel.onActionDone.observeEvent(viewLifecycleOwner, ReversibleActionObserver(binding.recyclerView))
		viewModel.isGrid.observe(viewLifecycleOwner, ::onGridModeChanged)
	}

	override fun onDestroyView() {
		super.onDestroyView()
		exploreAdapter = null
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		val rv = requireViewBinding().recyclerView
		rv.updatePadding(
			bottom = insets.bottom + rv.paddingTop,
		)
	}

	override fun onListHeaderClick(item: ListHeader, view: View) {
		startActivity(Intent(view.context, SourcesCatalogActivity::class.java))
	}

	override fun onPrimaryButtonClick(tipView: TipView) {
		when ((tipView.tag as? TipModel)?.key) {
			ExploreViewModel.TIP_NEW_SOURCES -> NewSourcesDialogFragment.show(childFragmentManager)
		}
	}

	override fun onSecondaryButtonClick(tipView: TipView) {
		when ((tipView.tag as? TipModel)?.key) {
			ExploreViewModel.TIP_NEW_SOURCES -> viewModel.discardNewSources()
		}
	}

	override fun onClick(v: View) {
		val intent = when (v.id) {
			R.id.button_local -> MangaListActivity.newIntent(v.context, MangaSource.LOCAL)
			R.id.button_bookmarks -> BookmarksActivity.newIntent(v.context)
			R.id.button_downloads -> DownloadsActivity.newIntent(v.context)
			R.id.button_random -> {
				viewModel.openRandom()
				return
			}

			else -> return
		}
		startActivity(intent)
	}

	override fun onItemClick(item: MangaSourceItem, view: View) {
		val intent = MangaListActivity.newIntent(view.context, item.source)
		startActivity(intent)
	}

	override fun onItemLongClick(item: MangaSourceItem, view: View): Boolean {
		val menu = PopupMenu(view.context, view)
		menu.inflate(R.menu.popup_source)
		menu.setOnMenuItemClickListener(SourceMenuListener(item))
		menu.show()
		return true
	}

	override fun onRetryClick(error: Throwable) = Unit

	override fun onEmptyActionClick() {
		startActivity(Intent(context ?: return, SourcesCatalogActivity::class.java))
	}

	private fun onOpenManga(manga: Manga) {
		val intent = DetailsActivity.newIntent(context ?: return, manga)
		startActivity(intent)
	}

	private fun onGridModeChanged(isGrid: Boolean) {
		requireViewBinding().recyclerView.layoutManager = if (isGrid) {
			GridLayoutManager(requireContext(), 4).also { lm ->
				lm.spanSizeLookup = ExploreGridSpanSizeLookup(checkNotNull(exploreAdapter), lm)
			}
		} else {
			LinearLayoutManager(requireContext())
		}
	}

	private inner class SourceMenuListener(
		private val sourceItem: MangaSourceItem,
	) : PopupMenu.OnMenuItemClickListener {

		override fun onMenuItemClick(item: MenuItem): Boolean {
			when (item.itemId) {
				R.id.action_settings -> {
					startActivity(SettingsActivity.newSourceSettingsIntent(requireContext(), sourceItem.source))
				}

				R.id.action_hide -> {
					viewModel.hideSource(sourceItem.source)
				}

				else -> return false
			}
			return true
		}
	}
}
