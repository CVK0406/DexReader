package com.example.dexreader.download.ui.list.chapters

import com.example.dexreader.list.ui.ListModelDiffCallback
import com.example.dexreader.list.ui.model.ListModel

data class DownloadChapter(
	val number: String?,
	val name: String,
	val isDownloaded: Boolean,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is DownloadChapter && other.name == name
	}

	override fun getChangePayload(previousState: ListModel): Any? {
		return if (previousState is DownloadChapter && previousState.name == name && previousState.number == number) {
			ListModelDiffCallback.PAYLOAD_PROGRESS_CHANGED
		} else {
			super.getChangePayload(previousState)
		}
	}
}
