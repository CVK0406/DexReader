package com.example.dexreader.stats.ui

import android.content.res.ColorStateList
import com.example.dexreader.R
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.core.util.DexReaderColors
import com.example.dexreader.databinding.ItemStatsBinding
import com.example.dexreader.stats.domain.StatsRecord
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.example.dexreader.parsers.model.Manga

fun statsAD(
	listener: OnListItemClickListener<Manga>,
) = adapterDelegateViewBinding<StatsRecord, StatsRecord, ItemStatsBinding>(
	{ layoutInflater, parent -> ItemStatsBinding.inflate(layoutInflater, parent, false) },
) {

	binding.root.setOnClickListener { v ->
		listener.onItemClick(item.manga ?: return@setOnClickListener, v)
	}

	bind {
		binding.textViewTitle.text = item.manga?.title ?: getString(R.string.other_manga)
		binding.textViewSummary.text = item.time.format(context.resources)
		binding.imageViewBadge.imageTintList = ColorStateList.valueOf(DexReaderColors.ofManga(context, item.manga))
		binding.root.isClickable = item.manga != null
	}
}
