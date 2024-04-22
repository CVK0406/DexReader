package com.example.dexreader.settings.backup

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import com.example.dexreader.core.ui.BaseListAdapter
import com.example.dexreader.core.ui.list.OnListItemClickListener
import com.example.dexreader.core.util.ext.setChecked
import com.example.dexreader.databinding.ItemCheckableMultipleBinding
import com.example.dexreader.list.ui.ListModelDiffCallback.Companion.PAYLOAD_CHECKED_CHANGED
import com.example.dexreader.list.ui.adapter.ListItemType

class BackupEntriesAdapter(
	clickListener: OnListItemClickListener<BackupEntryModel>,
) : BaseListAdapter<BackupEntryModel>() {

	init {
		addDelegate(ListItemType.NAV_ITEM, backupEntryAD(clickListener))
	}
}

private fun backupEntryAD(
	clickListener: OnListItemClickListener<BackupEntryModel>,
) = adapterDelegateViewBinding<BackupEntryModel, BackupEntryModel, ItemCheckableMultipleBinding>(
	{ layoutInflater, parent -> ItemCheckableMultipleBinding.inflate(layoutInflater, parent, false) },
) {

	binding.root.setOnClickListener { v ->
		clickListener.onItemClick(item, v)
	}

	bind { payloads ->
		with(binding.root) {
			setText(item.titleResId)
			setChecked(item.isChecked, PAYLOAD_CHECKED_CHANGED in payloads)
			isEnabled = item.isEnabled
		}
	}
}
