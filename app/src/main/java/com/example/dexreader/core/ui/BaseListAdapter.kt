package com.example.dexreader.core.ui

import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.AsyncListDiffer.ListListener
import com.example.dexreader.core.util.ContinuationResumeRunnable
import com.example.dexreader.list.ui.ListModelDiffCallback
import com.example.dexreader.list.ui.adapter.ListItemType
import com.example.dexreader.list.ui.model.ListHeader
import com.example.dexreader.list.ui.model.ListModel
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.FlowCollector
import kotlin.coroutines.suspendCoroutine

open class BaseListAdapter<T : ListModel> : AsyncListDifferDelegationAdapter<T>(
	AsyncDifferConfig.Builder(ListModelDiffCallback<T>())
		.setBackgroundThreadExecutor(Dispatchers.Default.limitedParallelism(2).asExecutor())
		.build(),
), FlowCollector<List<T>?> {

	override suspend fun emit(value: List<T>?) = suspendCoroutine { cont ->
		setItems(value.orEmpty(), ContinuationResumeRunnable(cont))
	}

	fun addDelegate(type: ListItemType, delegate: AdapterDelegate<List<T>>): BaseListAdapter<T> {
		delegatesManager.addDelegate(type.ordinal, delegate)
		return this
	}

	fun addListListener(listListener: ListListener<T>): BaseListAdapter<T> {
		differ.addListListener(listListener)
		return this
	}

	fun removeListListener(listListener: ListListener<T>) {
		differ.removeListListener(listListener)
	}

	fun findHeader(position: Int): ListHeader? {
		val snapshot = items
		for (i in (0..position).reversed()) {
			val item = snapshot.getOrNull(i) ?: continue
			if (item is ListHeader) {
				return item
			}
		}
		return null
	}
}
