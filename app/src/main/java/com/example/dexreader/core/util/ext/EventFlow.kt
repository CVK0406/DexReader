package com.example.dexreader.core.util.ext

import androidx.annotation.AnyThread
import com.example.dexreader.core.util.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Suppress("FunctionName")
fun <T> MutableEventFlow() = MutableStateFlow<Event<T>?>(null)

typealias EventFlow<T> = StateFlow<Event<T>?>

typealias MutableEventFlow<T> = MutableStateFlow<Event<T>?>

@AnyThread
fun <T> MutableEventFlow<T>.call(data: T) {
	value = Event(data)
}
