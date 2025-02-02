package com.example.dexreader.core.util.ext

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.dexreader.core.util.Event
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

fun <T> Flow<T>.observe(owner: LifecycleOwner, collector: FlowCollector<T>) {
	val start = if (this is StateFlow) CoroutineStart.UNDISPATCHED else CoroutineStart.DEFAULT
	owner.lifecycleScope.launch(start = start) {
		collect(collector)
	}
}

fun <T> Flow<T>.observe(owner: LifecycleOwner, minState: Lifecycle.State, collector: FlowCollector<T>) {
	owner.lifecycleScope.launch {
		owner.lifecycle.repeatOnLifecycle(minState) {
			collect(collector)
		}
	}
}

fun <T> Flow<Event<T>?>.observeEvent(owner: LifecycleOwner, collector: FlowCollector<T>) {
	owner.lifecycleScope.launch {
		owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
			collect {
				it?.consume(collector)
			}
		}
	}
}
