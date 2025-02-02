package com.example.dexreader.core.util.ext

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleDestroyedException
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.dexreader.core.util.RetainedLifecycleCoroutineScope
import dagger.hilt.android.lifecycle.RetainedLifecycle
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import org.example.dexreader.parsers.util.runCatchingCancellable
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

val processLifecycleScope: LifecycleCoroutineScope
	inline get() = ProcessLifecycleOwner.get().lifecycleScope

val RetainedLifecycle.lifecycleScope: RetainedLifecycleCoroutineScope
	inline get() = RetainedLifecycleCoroutineScope(this)

suspend fun Lifecycle.awaitStateAtLeast(state: Lifecycle.State) {
	if (currentState.isAtLeast(state)) {
		return
	}
	suspendCancellableCoroutine { cont ->
		val observer = ContinuationLifecycleObserver(this, cont, state)
		addObserverFromAnyThread(observer)
		cont.invokeOnCancellation {
			removeObserverFromAnyThread(observer)
		}
	}
}

private class ContinuationLifecycleObserver(
	private val lifecycle: Lifecycle,
	private val continuation: CancellableContinuation<Unit>,
	private val targetState: Lifecycle.State,
) : LifecycleEventObserver {

	override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
		if (event == Lifecycle.Event.upTo(targetState)) {
			lifecycle.removeObserver(this)
			continuation.resume(Unit)
		} else if (event == Lifecycle.Event.ON_DESTROY) {
			lifecycle.removeObserver(this)
			continuation.resumeWithException(LifecycleDestroyedException())
		}
	}
}

private fun Lifecycle.addObserverFromAnyThread(observer: LifecycleObserver) {
	val dispatcher = Dispatchers.Main.immediate
	if (dispatcher.isDispatchNeeded(EmptyCoroutineContext)) {
		dispatcher.dispatch(EmptyCoroutineContext) { addObserver(observer) }
	} else {
		addObserver(observer)
	}
}

private fun Lifecycle.removeObserverFromAnyThread(observer: LifecycleObserver) {
	val dispatcher = Dispatchers.Main.immediate
	if (dispatcher.isDispatchNeeded(EmptyCoroutineContext)) {
		dispatcher.dispatch(EmptyCoroutineContext) { removeObserver(observer) }
	} else {
		removeObserver(observer)
	}
}

fun <T> Deferred<T>.getCompletionResultOrNull(): Result<T>? = if (isCompleted) {
	getCompletionExceptionOrNull()?.let { error ->
		Result.failure(error)
	} ?: Result.success(getCompleted())
} else {
	null
}

fun <T> Deferred<T>.peek(): T? = if (isCompleted) {
	runCatchingCancellable {
		getCompleted()
	}.getOrNull()
} else {
	null
}
