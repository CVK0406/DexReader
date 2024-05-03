package com.example.dexreader.settings.sources.catalog

import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.example.dexreader.R
import com.example.dexreader.core.model.getSummary
import com.example.dexreader.core.model.getTitle
import com.example.dexreader.core.parser.favicon.faviconUri
import com.example.dexreader.core.ui.image.FaviconDrawable
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.core.ui.util.WindowInsetsDelegate
import com.example.dexreader.core.util.ext.crossfade
import com.example.dexreader.core.util.ext.enqueueWith
import com.example.dexreader.core.util.ext.newImageRequest
import com.example.dexreader.core.util.ext.setTextAndVisible
import com.example.dexreader.core.util.ext.source
import com.example.dexreader.databinding.ItemCatalogPageBinding
import com.example.dexreader.databinding.ItemEmptyHintBinding
import com.example.dexreader.databinding.ItemSourceCatalogBinding

fun sourceCatalogItemSourceAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	listener: OnListItemClickListener<SourceCatalogItem.Source>
) = adapterDelegateViewBinding<SourceCatalogItem.Source, SourceCatalogItem, ItemSourceCatalogBinding>(
	{ layoutInflater, parent ->
		ItemSourceCatalogBinding.inflate(layoutInflater, parent, false)
	},
) {

	binding.imageViewAdd.setOnClickListener { v ->
		listener.onItemClick(item, v)
	}

	bind {
		binding.textViewTitle.text = item.source.getTitle(context)
		if (item.showSummary) {
			binding.textViewDescription.text = item.source.getSummary(context)
			binding.textViewDescription.isVisible = true
		} else {
			binding.textViewDescription.isVisible = false
		}
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

fun sourceCatalogItemHintAD(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
) = adapterDelegateViewBinding<SourceCatalogItem.Hint, SourceCatalogItem, ItemEmptyHintBinding>(
	{ inflater, parent -> ItemEmptyHintBinding.inflate(inflater, parent, false) },
) {

	binding.buttonRetry.isVisible = false

	bind {
		binding.icon.newImageRequest(lifecycleOwner, item.icon)?.enqueueWith(coil)
		binding.textPrimary.setText(item.title)
		binding.textSecondary.setTextAndVisible(item.text)
	}
}

fun sourceCatalogPageAD(
	listener: OnListItemClickListener<SourceCatalogItem.Source>,
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
) = adapterDelegateViewBinding<SourceCatalogPage, SourceCatalogPage, ItemCatalogPageBinding>(
	{ inflater, parent -> ItemCatalogPageBinding.inflate(inflater, parent, false) },
) {

	val sourcesAdapter = SourcesCatalogAdapter(listener, coil, lifecycleOwner)
	with(binding.recyclerView) {
		setHasFixedSize(true)
		adapter = sourcesAdapter
	}
	val insetsDelegate = WindowInsetsDelegate()
	ViewCompat.setOnApplyWindowInsetsListener(itemView, insetsDelegate)
	itemView.addOnLayoutChangeListener(insetsDelegate)
	insetsDelegate.addInsetsListener { insets ->
		binding.recyclerView.updatePadding(
			bottom = insets.bottom + binding.recyclerView.paddingTop,
		)
	}

	bind {
		sourcesAdapter.items = item.items
	}
}
