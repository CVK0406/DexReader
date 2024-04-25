package com.example.dexreader.settings.newsources

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.example.dexreader.core.ui.BaseListAdapter
import com.example.dexreader.settings.sources.adapter.SourceConfigListener
import com.example.dexreader.settings.sources.adapter.sourceConfigItemCheckableDelegate
import com.example.dexreader.settings.sources.model.SourceConfigItem

class SourcesSelectAdapter(
	listener: SourceConfigListener,
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
) : BaseListAdapter<SourceConfigItem>() {

	init {
		delegatesManager.addDelegate(sourceConfigItemCheckableDelegate(listener, coil, lifecycleOwner))
	}
}
