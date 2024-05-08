package com.example.dexreader.settings.storage.directories

import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.example.dexreader.R
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.core.util.ext.drawableStart
import com.example.dexreader.core.util.ext.textAndVisible
import com.example.dexreader.databinding.ItemStorageConfigBinding
import com.example.dexreader.settings.storage.DirectoryModel

fun directoryConfigAD(
	clickListener: OnListItemClickListener<DirectoryModel>,
) = adapterDelegateViewBinding<DirectoryModel, DirectoryModel, ItemStorageConfigBinding>(
	{ layoutInflater, parent -> ItemStorageConfigBinding.inflate(layoutInflater, parent, false) },
) {

	binding.imageViewRemove.setOnClickListener { v -> clickListener.onItemClick(item, v) }

	bind {
		binding.textViewTitle.text = item.title ?: getString(item.titleRes)
		binding.textViewSubtitle.textAndVisible = item.file?.absolutePath
		binding.imageViewRemove.isVisible = item.isRemovable
		binding.imageViewRemove.isEnabled = !item.isChecked
		binding.textViewTitle.drawableStart = if (!item.isAvailable) {
			ContextCompat.getDrawable(context, R.drawable.ic_alert_outline)?.apply {
				setTint(ContextCompat.getColor(context, R.color.warning))
			}
		} else if (item.isChecked) {
			ContextCompat.getDrawable(context, R.drawable.ic_download)
		} else {
			null
		}
	}
}
