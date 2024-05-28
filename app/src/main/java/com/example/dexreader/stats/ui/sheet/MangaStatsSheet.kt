package com.example.dexreader.stats.ui.sheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.IntList
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.example.dexreader.R
import com.example.dexreader.core.model.parcelable.ParcelableManga
import com.example.dexreader.core.ui.sheet.BaseAdaptiveSheet
import com.example.dexreader.core.util.DexReaderColors
import com.example.dexreader.core.util.ext.observe
import com.example.dexreader.core.util.ext.showDistinct
import com.example.dexreader.core.util.ext.textAndVisible
import com.example.dexreader.core.util.ext.withArgs
import com.example.dexreader.databinding.SheetStatsMangaBinding
import com.example.dexreader.details.ui.DetailsActivity
import com.example.dexreader.stats.ui.views.BarChartView
import dagger.hilt.android.AndroidEntryPoint
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.util.format

@AndroidEntryPoint
class MangaStatsSheet : BaseAdaptiveSheet<SheetStatsMangaBinding>(), View.OnClickListener {

	private val viewModel: MangaStatsViewModel by viewModels()

	override fun onCreateViewBinding(inflater: LayoutInflater, container: ViewGroup?): SheetStatsMangaBinding {
		return SheetStatsMangaBinding.inflate(inflater, container, false)
	}

	override fun onViewBindingCreated(binding: SheetStatsMangaBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		binding.textViewTitle.text = viewModel.manga.title
		binding.chartView.barColor = DexReaderColors.ofManga(binding.root.context, viewModel.manga)
		viewModel.stats.observe(viewLifecycleOwner, ::onStatsChanged)
		viewModel.startDate.observe(viewLifecycleOwner) {
			binding.textViewStart.textAndVisible = it?.format(resources)
		}
		viewModel.totalPagesRead.observe(viewLifecycleOwner) {
			binding.textViewPages.text = getString(R.string.pages_read_s, it.format())
		}
		binding.buttonOpen.setOnClickListener(this)
	}

	override fun onClick(v: View) {
		startActivity(DetailsActivity.newIntent(v.context, viewModel.manga))
	}

	private fun onStatsChanged(stats: IntList) {
		val chartView = viewBinding?.chartView ?: return
		if (stats.isEmpty()) {
			chartView.setData(emptyList())
			return
		}
		val bars = ArrayList<BarChartView.Bar>(stats.size)
		stats.forEach { pages ->
			bars.add(
				BarChartView.Bar(
					value = pages,
					label = pages.toString(),
				),
			)
		}
		chartView.setData(bars)
	}

	companion object {

		const val ARG_MANGA = "manga"

		private const val TAG = "MangaStatsSheet"

		fun show(fm: FragmentManager, manga: Manga) {
			MangaStatsSheet().withArgs(1) {
				putParcelable(ARG_MANGA, ParcelableManga(manga))
			}.showDistinct(fm, TAG)
		}
	}
}
