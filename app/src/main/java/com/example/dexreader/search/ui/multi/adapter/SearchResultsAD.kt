package com.example.dexreader.search.ui.multi.adapter

import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import coil.ImageLoader
import com.example.dexreader.R
import com.example.dexreader.core.ui.list.AdapterDelegateClickListenerAdapter
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.core.ui.list.decor.SpacingItemDecoration
import com.example.dexreader.core.util.ext.getDisplayMessage
import com.example.dexreader.core.util.ext.textAndVisible
import com.example.dexreader.databinding.ItemListGroupBinding
import com.example.dexreader.list.ui.MangaSelectionDecoration
import com.example.dexreader.list.ui.adapter.mangaGridItemAD
import com.example.dexreader.list.ui.model.ListModel
import com.example.dexreader.list.ui.size.ItemSizeResolver
import com.example.dexreader.search.ui.multi.MultiSearchListModel
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.example.dexreader.parsers.model.Manga

fun searchResultsAD(
	sharedPool: RecycledViewPool,
	lifecycleOwner: LifecycleOwner,
	coil: ImageLoader,
	sizeResolver: ItemSizeResolver,
	selectionDecoration: MangaSelectionDecoration,
	listener: OnListItemClickListener<Manga>,
	itemClickListener: OnListItemClickListener<MultiSearchListModel>,
) = adapterDelegateViewBinding<MultiSearchListModel, ListModel, ItemListGroupBinding>(
	{ layoutInflater, parent -> ItemListGroupBinding.inflate(layoutInflater, parent, false) },
) {

	binding.recyclerView.setRecycledViewPool(sharedPool)
	val adapter = ListDelegationAdapter(
		mangaGridItemAD(coil, lifecycleOwner, sizeResolver, listener),
	)
	binding.recyclerView.addItemDecoration(selectionDecoration)
	binding.recyclerView.adapter = adapter
	val spacing = context.resources.getDimensionPixelOffset(R.dimen.grid_spacing)
	binding.recyclerView.addItemDecoration(SpacingItemDecoration(spacing))
	val eventListener = AdapterDelegateClickListenerAdapter(this, itemClickListener)
	binding.buttonMore.setOnClickListener(eventListener)

	bind {
		binding.textViewTitle.text = item.source.title
		binding.buttonMore.isVisible = item.hasMore
		adapter.items = item.list
		adapter.notifyDataSetChanged()
		binding.recyclerView.isGone = item.list.isEmpty()
		binding.textViewError.textAndVisible = item.error?.getDisplayMessage(context.resources)
	}
}
