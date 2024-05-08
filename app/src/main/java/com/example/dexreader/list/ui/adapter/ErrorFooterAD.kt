package com.example.dexreader.list.ui.adapter

import com.example.dexreader.core.util.ext.getDisplayMessage
import com.example.dexreader.databinding.ItemErrorFooterBinding
import com.example.dexreader.list.ui.model.ErrorFooter
import com.example.dexreader.list.ui.model.ListModel
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding

fun errorFooterAD(
	listener: MangaListListener?,
) = adapterDelegateViewBinding<ErrorFooter, ListModel, ItemErrorFooterBinding>(
	{ inflater, parent -> ItemErrorFooterBinding.inflate(inflater, parent, false) },
) {

	if (listener != null) {
		binding.root.setOnClickListener {
			listener.onRetryClick(item.exception)
		}
	}

	bind {
		binding.textViewTitle.text = item.exception.getDisplayMessage(context.resources)
	}
}
