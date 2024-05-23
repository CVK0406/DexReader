package com.example.dexreader.core.util.iterator

import com.example.dexreader.R

class MappingIterator<T, R>(
	private val upstream: Iterator<T>,
	private val mapper: (T) -> R,
) : Iterator<R> {

	override fun hasNext(): Boolean = upstream.hasNext()

	override fun next(): R = mapper(upstream.next())
}
