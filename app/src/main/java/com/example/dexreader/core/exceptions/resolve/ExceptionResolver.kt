package com.example.dexreader.core.exceptions.resolve

import androidx.activity.result.ActivityResultCallback
import androidx.annotation.StringRes
import androidx.collection.ArrayMap
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.example.dexreader.R
import com.example.dexreader.core.util.TaggedActivityResult
import org.example.dexreader.parsers.exception.AuthRequiredException
import org.example.dexreader.parsers.exception.NotFoundException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class ExceptionResolver : ActivityResultCallback<TaggedActivityResult> {

	private val continuations = ArrayMap<String, Continuation<Boolean>>(1)
	private val activity: FragmentActivity?
	private val fragment: Fragment?
	constructor(activity: FragmentActivity) {
		this.activity = activity
		fragment = null
	}

	constructor(fragment: Fragment) {
		this.fragment = fragment
		activity = null
	}

	override fun onActivityResult(result: TaggedActivityResult) {
		continuations.remove(result.tag)?.resume(result.isSuccess)
	}

	suspend fun resolve(e: Throwable): Boolean = when (e) {
		is NotFoundException -> {
			false
		}
		else -> false
	}

	private fun getFragmentManager() = checkNotNull(fragment?.childFragmentManager ?: activity?.supportFragmentManager)

	companion object {

		@StringRes
		fun getResolveStringId(e: Throwable) = when (e) {
			is AuthRequiredException -> R.string.sign_in
			is NotFoundException -> if (e.url.isNotEmpty()) R.string.open_in_browser else 0
			else -> 0
		}

		fun canResolve(e: Throwable) = getResolveStringId(e) != 0
	}
}
