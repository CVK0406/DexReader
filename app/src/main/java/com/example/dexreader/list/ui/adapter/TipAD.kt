package com.example.dexreader.list.ui.adapter

import com.example.dexreader.core.ui.widgets.TipView
import com.example.dexreader.databinding.ItemTip2Binding
import com.example.dexreader.list.ui.model.ListModel
import com.example.dexreader.list.ui.model.TipModel
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding

fun tipAD(
	listener: TipView.OnButtonClickListener,
) = adapterDelegateViewBinding<TipModel, ListModel, ItemTip2Binding>(
	{ layoutInflater, parent -> ItemTip2Binding.inflate(layoutInflater, parent, false) }
) {

	binding.root.onButtonClickListener = listener

	bind {
		with(binding.root) {
			tag = item
			setTitle(item.title)
			setText(item.text)
			setIcon(item.icon)
			setPrimaryButtonText(item.primaryButtonText)
			setSecondaryButtonText(item.secondaryButtonText)
		}
	}
}
