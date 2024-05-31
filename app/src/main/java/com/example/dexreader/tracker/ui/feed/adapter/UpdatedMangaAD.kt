package com.example.dexreader.tracker.ui.feed.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.example.dexreader.R
import com.example.dexreader.core.ui.BaseListAdapter
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.core.ui.list.decor.SpacingItemDecoration
import com.example.dexreader.databinding.ItemListGroupBinding
import com.example.dexreader.list.ui.adapter.ListHeaderClickListener
import com.example.dexreader.list.ui.adapter.ListItemType
import com.example.dexreader.list.ui.adapter.mangaGridItemAD
import com.example.dexreader.list.ui.model.ListHeader
import com.example.dexreader.list.ui.model.ListModel
import com.example.dexreader.list.ui.size.ItemSizeResolver
import org.example.dexreader.parsers.model.Manga
import com.example.dexreader.tracker.ui.feed.model.UpdatedMangaHeader

fun updatedMangaAD(
	lifecycleOwner: LifecycleOwner,
	coil: ImageLoader,
	sizeResolver: ItemSizeResolver,
	listener: OnListItemClickListener<Manga>,
	headerClickListener: ListHeaderClickListener,
) = adapterDelegateViewBinding<UpdatedMangaHeader, ListModel, ItemListGroupBinding>(
	{ layoutInflater, parent -> ItemListGroupBinding.inflate(layoutInflater, parent, false) },
) {

	val adapter = BaseListAdapter<ListModel>()
		.addDelegate(ListItemType.MANGA_GRID, mangaGridItemAD(coil, lifecycleOwner, sizeResolver, listener))
	binding.recyclerView.adapter = adapter
	val spacing = context.resources.getDimensionPixelOffset(R.dimen.grid_spacing)
	binding.recyclerView.addItemDecoration(SpacingItemDecoration(spacing))
	binding.buttonMore.setOnClickListener { v ->
		headerClickListener.onListHeaderClick(ListHeader(0, payload = item), v)
	}
	binding.textViewTitle.setText(R.string.updates)
	binding.buttonMore.setText(R.string.more)

	bind {
		adapter.items = item.list
	}
}
