package com.example.dexreader.settings.sources.adapter

import com.example.dexreader.core.ui.list.OnTipCloseListener
import com.example.dexreader.settings.sources.model.SourceConfigItem

interface SourceConfigListener : OnTipCloseListener<SourceConfigItem.Tip> {

	fun onItemSettingsClick(item: SourceConfigItem.SourceItem)

	fun onItemLiftClick(item: SourceConfigItem.SourceItem)

	fun onItemShortcutClick(item: SourceConfigItem.SourceItem)

	fun onItemEnabledChanged(item: SourceConfigItem.SourceItem, isEnabled: Boolean)
}
