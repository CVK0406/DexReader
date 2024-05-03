package com.example.dexreader.settings.sources.catalog

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.example.dexreader.core.ui.BaseListAdapter
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.core.ui.list.fastscroll.FastScroller
import com.example.dexreader.list.ui.adapter.ListItemType

class SourcesCatalogAdapter(
	listener: OnListItemClickListener<SourceCatalogItem.Source>,
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
) : BaseListAdapter<SourceCatalogItem>(), FastScroller.SectionIndexer {

	init {
		addDelegate(ListItemType.CHAPTER_LIST, sourceCatalogItemSourceAD(coil, lifecycleOwner, listener))
		addDelegate(ListItemType.HINT_EMPTY, sourceCatalogItemHintAD(coil, lifecycleOwner))
	}

	override fun getSectionText(context: Context, position: Int): CharSequence? {
		return (items.getOrNull(position) as? SourceCatalogItem.Source)?.source?.title?.take(1)
	}
}
