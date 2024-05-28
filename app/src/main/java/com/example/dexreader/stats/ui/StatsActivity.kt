package com.example.dexreader.stats.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewStub
import android.widget.CompoundButton
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.Insets
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.AsyncListDiffer
import coil.ImageLoader
import com.example.dexreader.R
import com.example.dexreader.core.model.FavouriteCategory
import com.example.dexreader.core.ui.BaseActivity
import com.example.dexreader.core.ui.BaseListAdapter
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.core.ui.util.ReversibleActionObserver
import com.example.dexreader.core.util.DexReaderColors
import com.example.dexreader.core.util.ext.DIALOG_THEME_CENTERED
import com.example.dexreader.core.util.ext.enqueueWith
import com.example.dexreader.core.util.ext.newImageRequest
import com.example.dexreader.core.util.ext.observe
import com.example.dexreader.core.util.ext.observeEvent
import com.example.dexreader.core.util.ext.setTextAndVisible
import com.example.dexreader.core.util.ext.showOrHide
import com.example.dexreader.databinding.ActivityStatsBinding
import com.example.dexreader.databinding.ItemEmptyStateBinding
import com.example.dexreader.list.ui.adapter.ListItemType
import com.example.dexreader.stats.domain.StatsPeriod
import com.example.dexreader.stats.domain.StatsRecord
import com.example.dexreader.stats.ui.sheet.MangaStatsSheet
import com.example.dexreader.stats.ui.views.PieChartView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.example.dexreader.parsers.model.Manga
import javax.inject.Inject

@AndroidEntryPoint
class StatsActivity : BaseActivity<ActivityStatsBinding>(),
	OnListItemClickListener<Manga>,
	PieChartView.OnSegmentClickListener,
	AsyncListDiffer.ListListener<StatsRecord>,
	ViewStub.OnInflateListener, View.OnClickListener, CompoundButton.OnCheckedChangeListener {

	@Inject
	lateinit var coil: ImageLoader

	private val viewModel: StatsViewModel by viewModels()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityStatsBinding.inflate(layoutInflater))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		val adapter = BaseListAdapter<StatsRecord>()
			.addDelegate(ListItemType.FEED, statsAD(this))
			.addListListener(this)
		viewBinding.recyclerView.adapter = adapter
		viewBinding.chart.onSegmentClickListener = this
		viewBinding.stubEmpty.setOnInflateListener(this)
		viewBinding.chipPeriod.setOnClickListener(this)

		viewModel.isLoading.observe(this) {
			viewBinding.progressBar.showOrHide(it)
		}
		viewModel.period.observe(this) {
			viewBinding.chipPeriod.setText(it.titleResId)
		}
		viewModel.favoriteCategories.observe(this, ::createCategoriesChips)
		viewModel.onActionDone.observeEvent(this, ReversibleActionObserver(viewBinding.recyclerView))
		viewModel.readingStats.observe(this) {
			val sum = it.sumOf { it.duration }
			viewBinding.chart.setData(
				it.map { v ->
					PieChartView.Segment(
						value = (v.duration / 1000).toInt(),
						label = v.manga?.title ?: getString(R.string.other_manga),
						percent = (v.duration.toDouble() / sum).toFloat(),
						color = DexReaderColors.ofManga(this, v.manga),
						tag = v.manga,
					)
				},
			)
			adapter.emit(it)
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) = Unit

	override fun onClick(v: View) {
		when (v.id) {
			R.id.chip_period -> showPeriodSelector()
		}
	}

	override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
		val category = buttonView?.tag as? FavouriteCategory ?: return
		viewModel.setCategoryChecked(category, isChecked)
	}

	override fun onItemClick(item: Manga, view: View) {
		MangaStatsSheet.show(supportFragmentManager, item)
	}

	override fun onSegmentClick(view: PieChartView, segment: PieChartView.Segment) {
		val manga = segment.tag as? Manga ?: return
		onItemClick(manga, view)
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.opt_stats, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_clear -> {
				showClearConfirmDialog()
				true
			}

			else -> super.onOptionsItemSelected(item)
		}
	}

	override fun onCurrentListChanged(previousList: MutableList<StatsRecord>, currentList: MutableList<StatsRecord>) {
		val isEmpty = currentList.isEmpty()
		with(viewBinding) {
			chart.isGone = isEmpty
			recyclerView.isGone = isEmpty
			stubEmpty.isVisible = isEmpty
		}
	}

	override fun onInflate(stub: ViewStub?, inflated: View) {
		val stubBinding = ItemEmptyStateBinding.bind(inflated)
		stubBinding.icon.newImageRequest(this, R.drawable.ic_empty_history)?.enqueueWith(coil)
		stubBinding.textPrimary.setText(R.string.text_empty_holder_primary)
		stubBinding.textSecondary.setTextAndVisible(R.string.empty_stats_text)
		stubBinding.buttonRetry.isVisible = false
	}

	private fun createCategoriesChips(categories: List<FavouriteCategory>) {
		val container = viewBinding.layoutChips
		if (container.childCount > 1) {
			// avoid duplication
			return
		}
		val checkedIds = viewModel.selectedCategories.value
		for (category in categories) {
			val chip = Chip(this)
			val drawable = ChipDrawable.createFromAttributes(this, null, 0, R.style.Widget_DexReader_Chip_Filter)
			chip.setChipDrawable(drawable)
			chip.text = category.title
			chip.tag = category
			chip.isChecked = category.id in checkedIds
			chip.setOnCheckedChangeListener(this)
			container.addView(chip)
		}
	}

	private fun showClearConfirmDialog() {
		MaterialAlertDialogBuilder(this, DIALOG_THEME_CENTERED)
			.setMessage(R.string.clear_stats_confirm)
			.setTitle(R.string.clear_stats)
			.setIcon(R.drawable.ic_delete)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.clear) { _, _ ->
				viewModel.clear()
			}.show()
	}

	private fun showPeriodSelector() {
		val menu = PopupMenu(this, viewBinding.chipPeriod)
		val selected = viewModel.period.value
		for ((i, branch) in StatsPeriod.entries.withIndex()) {
			val item = menu.menu.add(R.id.group_period, Menu.NONE, i, branch.titleResId)
			item.isCheckable = true
			item.isChecked = selected.ordinal == i
		}
		menu.menu.setGroupCheckable(R.id.group_period, true, true)

		menu.setOnMenuItemClickListener {
			StatsPeriod.entries.getOrNull(it.order)?.also {
				viewModel.period.value = it
			} != null
		}
		menu.show()
	}
}
