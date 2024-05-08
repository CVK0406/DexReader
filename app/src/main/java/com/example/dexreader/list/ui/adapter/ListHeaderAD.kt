package com.example.dexreader.list.ui.adapter

import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.example.dexreader.databinding.ItemHeaderButtonBinding
import com.example.dexreader.list.ui.model.ListHeader
import com.example.dexreader.list.ui.model.ListModel
import com.google.android.material.badge.BadgeDrawable
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding

fun listHeaderAD(
	listener: ListHeaderClickListener?,
) = adapterDelegateViewBinding<ListHeader, ListModel, ItemHeaderButtonBinding>(
	{ inflater, parent -> ItemHeaderButtonBinding.inflate(inflater, parent, false) },
) {
	var badge: BadgeDrawable? = null

	if (listener != null) {
		binding.buttonMore.setOnClickListener {
			listener.onListHeaderClick(item, it)
		}
	}

	bind {
		binding.textViewTitle.text = item.getText(context)
		if (item.buttonTextRes == 0) {
			binding.buttonMore.isInvisible = true
			binding.buttonMore.text = null
			binding.buttonMore.clearBadge(badge)
		} else {
			binding.buttonMore.setText(item.buttonTextRes)
			binding.buttonMore.isVisible = true
			badge = itemView.bindBadge(badge, item.badge)
		}
	}
}
