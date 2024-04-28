package com.example.dexreader.settings.sources.adapter

import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.example.dexreader.R
import com.example.dexreader.core.model.getSummary
import com.example.dexreader.core.model.getTitle
import com.example.dexreader.core.parser.favicon.faviconUri
import com.example.dexreader.core.ui.image.FaviconDrawable
import com.example.dexreader.core.ui.list.OnTipCloseListener
import com.example.dexreader.core.util.ext.crossfade
import com.example.dexreader.core.util.ext.enqueueWith
import com.example.dexreader.core.util.ext.newImageRequest
import com.example.dexreader.core.util.ext.source
import com.example.dexreader.databinding.ItemSourceConfigBinding
import com.example.dexreader.databinding.ItemSourceConfigCheckableBinding
import com.example.dexreader.databinding.ItemTipBinding
import com.example.dexreader.settings.sources.model.SourceConfigItem

fun sourceConfigItemCheckableDelegate(
	listener: SourceConfigListener,
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
) = adapterDelegateViewBinding<SourceConfigItem.SourceItem, SourceConfigItem, ItemSourceConfigCheckableBinding>(
	{ layoutInflater, parent ->
		ItemSourceConfigCheckableBinding.inflate(
			layoutInflater,
			parent,
			false,
		)
	},
) {

	binding.switchToggle.setOnCheckedChangeListener { _, isChecked ->
		listener.onItemEnabledChanged(item, isChecked)
	}

	bind {
		binding.textViewTitle.text = item.source.getTitle(context)
		binding.switchToggle.isChecked = item.isEnabled
		binding.switchToggle.isEnabled = item.isAvailable
		binding.textViewDescription.text = item.source.getSummary(context)
		val fallbackIcon = FaviconDrawable(context, R.style.FaviconDrawable_Small, item.source.name)
		binding.imageViewIcon.newImageRequest(lifecycleOwner, item.source.faviconUri())?.run {
			crossfade(context)
			error(fallbackIcon)
			placeholder(fallbackIcon)
			fallback(fallbackIcon)
			source(item.source)
			enqueueWith(coil)
		}
	}
}

fun sourceConfigItemDelegate2(
	listener: SourceConfigListener,
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
) = adapterDelegateViewBinding<SourceConfigItem.SourceItem, SourceConfigItem, ItemSourceConfigBinding>(
	{ layoutInflater, parent ->
		ItemSourceConfigBinding.inflate(
			layoutInflater,
			parent,
			false,
		)
	},
) {

	val eventListener = View.OnClickListener { v ->
		when (v.id) {
			R.id.imageView_add -> listener.onItemEnabledChanged(item, true)
			R.id.imageView_remove -> listener.onItemEnabledChanged(item, false)
			R.id.imageView_menu -> showSourceMenu(v, item, listener)
		}
	}
	binding.imageViewRemove.setOnClickListener(eventListener)
	binding.imageViewAdd.setOnClickListener(eventListener)
	binding.imageViewMenu.setOnClickListener(eventListener)

	bind {
		binding.textViewTitle.text = item.source.getTitle(context)
		binding.imageViewAdd.isGone = item.isEnabled || !item.isAvailable
		binding.imageViewRemove.isVisible = item.isEnabled
		binding.imageViewMenu.isVisible = item.isEnabled
		binding.textViewDescription.text = item.source.getSummary(context)
		val fallbackIcon = FaviconDrawable(context, R.style.FaviconDrawable_Small, item.source.name)
		binding.imageViewIcon.newImageRequest(lifecycleOwner, item.source.faviconUri())?.run {
			crossfade(context)
			error(fallbackIcon)
			placeholder(fallbackIcon)
			fallback(fallbackIcon)
			source(item.source)
			enqueueWith(coil)
		}
	}
}

fun sourceConfigTipDelegate(
	listener: OnTipCloseListener<SourceConfigItem.Tip>,
) = adapterDelegateViewBinding<SourceConfigItem.Tip, SourceConfigItem, ItemTipBinding>(
	{ layoutInflater, parent -> ItemTipBinding.inflate(layoutInflater, parent, false) },
) {

	binding.buttonClose.setOnClickListener {
		listener.onCloseTip(item)
	}

	bind {
		binding.imageViewIcon.setImageResource(item.iconResId)
		binding.textView.setText(item.textResId)
	}
}

fun sourceConfigEmptySearchDelegate() =
	adapterDelegate<SourceConfigItem.EmptySearchResult, SourceConfigItem>(
		R.layout.item_sources_empty,
	) { }

private fun showSourceMenu(
	anchor: View,
	item: SourceConfigItem.SourceItem,
	listener: SourceConfigListener,
) {
	val menu = PopupMenu(anchor.context, anchor)
	menu.inflate(R.menu.popup_source_config)
	menu.menu.findItem(R.id.action_lift)?.isVisible = item.isDraggable
	menu.setOnMenuItemClickListener {
		when (it.itemId) {
			R.id.action_settings -> listener.onItemSettingsClick(item)
			R.id.action_lift -> listener.onItemLiftClick(item)
		}
		true
	}
	menu.show()
}
