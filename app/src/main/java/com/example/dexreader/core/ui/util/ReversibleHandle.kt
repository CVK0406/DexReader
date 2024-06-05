package com.example.dexreader.core.ui.util

import com.example.dexreader.core.util.ext.printStackTraceDebug
import com.example.dexreader.core.util.ext.processLifecycleScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.dexreader.parsers.util.runCatchingCancellable

fun interface ReversibleHandle {

	suspend fun reverse()
}

fun ReversibleHandle.reverseAsync() = processLifecycleScope.launch(Dispatchers.Default, CoroutineStart.ATOMIC) {
	runCatchingCancellable {
		withContext(NonCancellable) {
			reverse()
		}
	}.onFailure {
		it.printStackTraceDebug()
	}
}

operator fun ReversibleHandle.plus(other: ReversibleHandle) = ReversibleHandle {
	this.reverse()
	other.reverse()
}
