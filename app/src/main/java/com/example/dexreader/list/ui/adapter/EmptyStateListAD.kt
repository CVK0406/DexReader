package com.example.dexreader.list.ui.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.example.dexreader.core.util.ext.enqueueWith
import com.example.dexreader.core.util.ext.newImageRequest
import com.example.dexreader.core.util.ext.setTextAndVisible
import com.example.dexreader.databinding.ItemEmptyStateBinding
import com.example.dexreader.list.ui.model.EmptyState
import com.example.dexreader.list.ui.model.ListModel
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding

fun emptyStateListAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	listener: ListStateHolderListener?,
) = adapterDelegateViewBinding<EmptyState, ListModel, ItemEmptyStateBinding>(
	{ inflater, parent -> ItemEmptyStateBinding.inflate(inflater, parent, false) },
) {

	if (listener != null) {
		binding.buttonRetry.setOnClickListener { listener.onEmptyActionClick() }
	}

	bind {
		binding.icon.newImageRequest(lifecycleOwner, item.icon)?.enqueueWith(coil)
		binding.textPrimary.setText(item.textPrimary)
		binding.textSecondary.setTextAndVisible(item.textSecondary)
		if (listener != null) {
			binding.buttonRetry.setTextAndVisible(item.actionStringRes)
		}
	}
}
