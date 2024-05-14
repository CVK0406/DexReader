package com.example.dexreader.download.ui.list

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.example.dexreader.core.ui.BaseListAdapter
import com.example.dexreader.list.ui.adapter.ListItemType
import com.example.dexreader.list.ui.adapter.emptyStateListAD
import com.example.dexreader.list.ui.adapter.listHeaderAD
import com.example.dexreader.list.ui.adapter.loadingStateAD
import com.example.dexreader.list.ui.model.ListModel

class DownloadsAdapter(
	lifecycleOwner: LifecycleOwner,
	coil: ImageLoader,
	listener: DownloadItemListener,
) : BaseListAdapter<ListModel>() {

	init {
		addDelegate(ListItemType.DOWNLOAD, downloadItemAD(lifecycleOwner, coil, listener))
		addDelegate(ListItemType.STATE_LOADING, loadingStateAD())
		addDelegate(ListItemType.STATE_EMPTY, emptyStateListAD(coil, lifecycleOwner, null))
		addDelegate(ListItemType.HEADER, listHeaderAD(null))
	}
}
