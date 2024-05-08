package com.example.dexreader.list.ui.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.example.dexreader.core.util.ext.enqueueWith
import com.example.dexreader.core.util.ext.newImageRequest
import com.example.dexreader.core.util.ext.setTextAndVisible
import com.example.dexreader.databinding.ItemEmptyCardBinding
import com.example.dexreader.list.ui.model.EmptyHint
import com.example.dexreader.list.ui.model.ListModel
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding

fun emptyHintAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	listener: ListStateHolderListener,
) = adapterDelegateViewBinding<EmptyHint, ListModel, ItemEmptyCardBinding>(
	{ inflater, parent -> ItemEmptyCardBinding.inflate(inflater, parent, false) },
) {

	binding.buttonRetry.setOnClickListener { listener.onEmptyActionClick() }

	bind {
		binding.icon.newImageRequest(lifecycleOwner, item.icon)?.enqueueWith(coil)
		binding.textPrimary.setText(item.textPrimary)
		binding.textSecondary.setTextAndVisible(item.textSecondary)
		binding.buttonRetry.setTextAndVisible(item.actionStringRes)
	}
}
