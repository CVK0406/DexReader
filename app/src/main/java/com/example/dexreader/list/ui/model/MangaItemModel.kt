package com.example.dexreader.list.ui.model

import com.example.dexreader.list.ui.ListModelDiffCallback
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.model.MangaSource

sealed class MangaItemModel : ListModel {

	abstract val id: Long
	abstract val manga: Manga
	abstract val title: String
	abstract val coverUrl: String
	abstract val counter: Int
	abstract val progress: Float

	val source: MangaSource
		get() = manga.source

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is MangaItemModel && other.javaClass == javaClass && id == other.id
	}

	override fun getChangePayload(previousState: ListModel): Any? {
		return when {
			previousState !is MangaItemModel -> super.getChangePayload(previousState)
			progress != previousState.progress -> ListModelDiffCallback.PAYLOAD_PROGRESS_CHANGED
			counter != previousState.counter -> ListModelDiffCallback.PAYLOAD_ANYTHING_CHANGED
			else -> null
		}
	}
}
