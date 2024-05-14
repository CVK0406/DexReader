package com.example.dexreader.download.ui.list

import com.example.dexreader.core.ui.list.OnListItemClickListener

interface DownloadItemListener : OnListItemClickListener<DownloadItemModel> {

	fun onCancelClick(item: DownloadItemModel)

	fun onPauseClick(item: DownloadItemModel)

	fun onResumeClick(item: DownloadItemModel, skip: Boolean)

	fun onExpandClick(item: DownloadItemModel)
}
