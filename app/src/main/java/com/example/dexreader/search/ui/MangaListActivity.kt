package com.example.dexreader.search.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.graphics.Insets
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.bundleOf
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.dexreader.BuildConfig
import com.example.dexreader.R
import com.example.dexreader.core.model.MangaSource
import com.example.dexreader.core.model.parcelable.ParcelableManga
import com.example.dexreader.core.model.parcelable.ParcelableMangaTags
import com.example.dexreader.core.parser.MangaIntent
import com.example.dexreader.core.ui.BaseActivity
import com.example.dexreader.core.ui.model.titleRes
import com.example.dexreader.core.util.ViewBadge
import com.example.dexreader.core.util.ext.getParcelableExtraCompat
import com.example.dexreader.core.util.ext.getThemeColor
import com.example.dexreader.core.util.ext.observe
import com.example.dexreader.core.util.ext.setTextAndVisible
import com.example.dexreader.databinding.ActivityMangaListBinding
import com.example.dexreader.filter.ui.FilterHeaderFragment
import com.example.dexreader.filter.ui.FilterOwner
import com.example.dexreader.filter.ui.MangaFilter
import com.example.dexreader.filter.ui.sheet.FilterSheetFragment
import com.example.dexreader.list.ui.preview.PreviewFragment
import com.example.dexreader.local.ui.LocalListFragment
import com.example.dexreader.main.ui.owners.AppBarOwner
import com.example.dexreader.remotelist.ui.RemoteListFragment
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.model.MangaSource
import org.example.dexreader.parsers.model.MangaTag
import kotlin.math.absoluteValue
import com.google.android.material.R as materialR

@AndroidEntryPoint
class MangaListActivity :
	BaseActivity<ActivityMangaListBinding>(),
	AppBarOwner, View.OnClickListener, FilterOwner, AppBarLayout.OnOffsetChangedListener {

	override val appBar: AppBarLayout
		get() = viewBinding.appbar

	override val filter: MangaFilter
		get() = checkNotNull(findFilterOwner()) {
			"Cannot find FilterOwner fragment in ${supportFragmentManager.fragments}"
		}.filter

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityMangaListBinding.inflate(layoutInflater))
		val tags = intent.getParcelableExtraCompat<ParcelableMangaTags>(EXTRA_TAGS)?.tags
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		if (viewBinding.containerFilterHeader != null) {
			viewBinding.appbar.addOnOffsetChangedListener(this)
		}
		val source = intent.getStringExtra(EXTRA_SOURCE)?.let(::MangaSource) ?: tags?.firstOrNull()?.source
		if (source == null) {
			finishAfterTransition()
			return
		}
		viewBinding.buttonOrder?.setOnClickListener(this)
		title = if (source == MangaSource.LOCAL) getString(R.string.local_storage) else source.title
		initList(source, tags)
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding.root.updatePadding(
			left = insets.left,
			right = insets.right,
		)
		viewBinding.cardSide?.updateLayoutParams<MarginLayoutParams> {
			bottomMargin = marginStart + insets.bottom
			topMargin = marginStart + insets.top
		}
	}

	override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
		val container = viewBinding.containerFilterHeader ?: return
		container.background = if (verticalOffset.absoluteValue < appBarLayout.totalScrollRange) {
			container.context.getThemeColor(materialR.attr.backgroundColor).toDrawable()
		} else {
			viewBinding.collapsingToolbarLayout?.contentScrim
		}
	}

	override fun onClick(v: View) {
		when (v.id) {
			R.id.button_order -> FilterSheetFragment.show(supportFragmentManager)
		}
	}

	fun showPreview(manga: Manga): Boolean = setSideFragment(
		PreviewFragment::class.java,
		bundleOf(MangaIntent.KEY_MANGA to ParcelableManga(manga)),
	)

	fun hidePreview() = setSideFragment(FilterSheetFragment::class.java, null)

	private fun initList(source: MangaSource, tags: Set<MangaTag>?) {
		val fm = supportFragmentManager
		val existingFragment = fm.findFragmentById(R.id.container)
		if (existingFragment is FilterOwner) {
			initFilter(existingFragment)
		} else {
			fm.commit {
				setReorderingAllowed(true)
				val fragment = if (source == MangaSource.LOCAL) {
					LocalListFragment()
				} else {
					RemoteListFragment.newInstance(source)
				}
				replace(R.id.container, fragment)
				runOnCommit { initFilter(fragment) }
				if (!tags.isNullOrEmpty()) {
					runOnCommit(ApplyFilterRunnable(fragment, tags))
				}
			}
		}
	}

	private fun initFilter(filterOwner: FilterOwner) {
		if (viewBinding.containerSide != null) {
			if (supportFragmentManager.findFragmentById(R.id.container_side) == null) {
				setSideFragment(FilterSheetFragment::class.java, null)
			}
		} else if (viewBinding.containerFilterHeader != null) {
			if (supportFragmentManager.findFragmentById(R.id.container_filter_header) == null) {
				supportFragmentManager.commit {
					setReorderingAllowed(true)
					replace(R.id.container_filter_header, FilterHeaderFragment::class.java, null)
				}
			}
		}
		val filter = filterOwner.filter
		val chipSort = viewBinding.buttonOrder
		if (chipSort != null) {
			val filterBadge = ViewBadge(chipSort, this)
			filterBadge.setMaxCharacterCount(0)
			filter.header.observe(this) {
				chipSort.setTextAndVisible(it.sortOrder?.titleRes ?: 0)
				filterBadge.counter = if (it.isFilterApplied) 1 else 0
			}
		} else {
			filter.header.map {
				it.textSummary
			}.flowOn(Dispatchers.Default)
				.observe(this) {
					supportActionBar?.subtitle = it
				}
		}
	}

	private fun findFilterOwner(): FilterOwner? {
		return supportFragmentManager.findFragmentById(R.id.container) as? FilterOwner
	}

	private fun setSideFragment(cls: Class<out Fragment>, args: Bundle?) = if (viewBinding.containerSide != null) {
		supportFragmentManager.commit {
			setReorderingAllowed(true)
			replace(R.id.container_side, cls, args)
		}
		true
	} else {
		false
	}

	private class ApplyFilterRunnable(
		private val filterOwner: FilterOwner,
		private val tags: Set<MangaTag>,
	) : Runnable {

		override fun run() {
			filterOwner.filter.applyFilter(tags)
		}
	}

	companion object {

		private const val EXTRA_TAGS = "tags"
		private const val EXTRA_SOURCE = "source"
		const val ACTION_MANGA_EXPLORE = "${BuildConfig.APPLICATION_ID}.action.EXPLORE_MANGA"

		fun newIntent(context: Context, tags: Set<MangaTag>) = Intent(context, MangaListActivity::class.java)
			.setAction(ACTION_MANGA_EXPLORE)
			.putExtra(EXTRA_TAGS, ParcelableMangaTags(tags))

		fun newIntent(context: Context, source: MangaSource) = Intent(context, MangaListActivity::class.java)
			.setAction(ACTION_MANGA_EXPLORE)
			.putExtra(EXTRA_SOURCE, source.name)
	}
}
