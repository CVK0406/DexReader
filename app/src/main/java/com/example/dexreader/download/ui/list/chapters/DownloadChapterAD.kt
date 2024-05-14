package com.example.dexreader.download.ui.list.chapters

import androidx.core.content.ContextCompat
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.example.dexreader.R
import com.example.dexreader.core.util.ext.drawableEnd
import com.example.dexreader.databinding.ItemChapterDownloadBinding

fun downloadChapterAD() = adapterDelegateViewBinding<DownloadChapter, DownloadChapter, ItemChapterDownloadBinding>(
	{ layoutInflater, parent -> ItemChapterDownloadBinding.inflate(layoutInflater, parent, false) },
) {

	val iconDone = ContextCompat.getDrawable(context, R.drawable.ic_check)

	bind {
		binding.textViewNumber.text = item.number.toString()
		binding.textViewTitle.text = item.name
		binding.textViewTitle.drawableEnd = if (item.isDownloaded) iconDone else null
	}
}
