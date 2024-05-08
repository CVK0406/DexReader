package com.example.dexreader.list.ui.adapter

import androidx.core.view.isVisible
import com.example.dexreader.core.util.ext.getDisplayMessage
import com.example.dexreader.databinding.ItemErrorStateBinding
import com.example.dexreader.list.ui.model.ErrorState
import com.example.dexreader.list.ui.model.ListModel
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding

fun errorStateListAD(
	listener: ListStateHolderListener,
) = adapterDelegateViewBinding<ErrorState, ListModel, ItemErrorStateBinding>(
	{ inflater, parent -> ItemErrorStateBinding.inflate(inflater, parent, false) },
) {

	binding.buttonRetry.setOnClickListener {
		listener.onRetryClick(item.exception)
	}

	bind {
		with(binding.textViewError) {
			text = item.exception.getDisplayMessage(context.resources)
			setCompoundDrawablesWithIntrinsicBounds(0, item.icon, 0, 0)
		}
		with(binding.buttonRetry) {
			isVisible = item.canRetry
			setText(item.buttonText)
		}
	}
}
