package com.example.dexreader.download.ui.dialog

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.example.dexreader.R
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.databinding.ItemDownloadOptionBinding

fun downloadOptionAD(
	onClickListener: OnListItemClickListener<DownloadOption>,
) = adapterDelegateViewBinding<DownloadOption, DownloadOption, ItemDownloadOptionBinding>(
	{ layoutInflater, parent -> ItemDownloadOptionBinding.inflate(layoutInflater, parent, false) },
) {

	binding.root.setOnClickListener { v -> onClickListener.onItemClick(item, v) }

	bind {
		with(binding.root) {
			title = item.getLabel(resources)
			subtitle = if (item.chaptersCount == 0) null else resources.getQuantityString(
				R.plurals.chapters,
				item.chaptersCount,
				item.chaptersCount,
			)
			setIconResource(item.iconResId)
		}
	}
}
