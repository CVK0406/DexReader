package com.example.dexreader.explore.ui.adapter

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.example.dexreader.core.ui.BaseListAdapter
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.core.ui.widgets.TipView
import com.example.dexreader.explore.ui.model.MangaSourceItem
import com.example.dexreader.list.ui.adapter.ListItemType
import com.example.dexreader.list.ui.adapter.emptyHintAD
import com.example.dexreader.list.ui.adapter.listHeaderAD
import com.example.dexreader.list.ui.adapter.loadingStateAD
import com.example.dexreader.list.ui.adapter.tipAD
import com.example.dexreader.list.ui.model.ListModel
import org.example.dexreader.parsers.model.Manga

class ExploreAdapter(
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
	listener: ExploreListEventListener,
	tipClickListener: TipView.OnButtonClickListener,
	clickListener: OnListItemClickListener<MangaSourceItem>,
	mangaClickListener: OnListItemClickListener<Manga>,
) : BaseListAdapter<ListModel>() {

	init {
		addDelegate(ListItemType.EXPLORE_BUTTONS, exploreButtonsAD(listener))
		addDelegate(ListItemType.HEADER, listHeaderAD(listener))
		addDelegate(ListItemType.EXPLORE_SOURCE_LIST, exploreSourceListItemAD(coil, clickListener, lifecycleOwner))
		addDelegate(ListItemType.EXPLORE_SOURCE_GRID, exploreSourceGridItemAD(coil, clickListener, lifecycleOwner))
		addDelegate(ListItemType.HINT_EMPTY, emptyHintAD(coil, lifecycleOwner, listener))
		addDelegate(ListItemType.STATE_LOADING, loadingStateAD())
		addDelegate(ListItemType.TIP, tipAD(tipClickListener))
	}
}
