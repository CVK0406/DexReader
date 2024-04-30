package com.example.dexreader.settings.sources.catalog

import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.example.dexreader.core.model.titleResId
import com.example.dexreader.core.ui.BaseListAdapter
import com.example.dexreader.core.ui.list.OnListItemClickListener

class SourcesCatalogPagerAdapter(
	listener: OnListItemClickListener<SourceCatalogItem.Source>,
	coil: ImageLoader,
	lifecycleOwner: LifecycleOwner,
) : BaseListAdapter<SourceCatalogPage>(), TabLayoutMediator.TabConfigurationStrategy {

	init {
		delegatesManager.addDelegate(sourceCatalogPageAD(listener, coil, lifecycleOwner))
	}

	override fun onConfigureTab(tab: TabLayout.Tab, position: Int) {
		val item = items.getOrNull(position) ?: return
		tab.setText(item.type.titleResId)
	}
}
