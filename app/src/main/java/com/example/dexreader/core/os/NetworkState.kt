package com.example.dexreader.core.os

import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.dexreader.core.util.MediatorStateFlow
import com.example.dexreader.core.util.ext.isOnline
import kotlinx.coroutines.flow.first

class NetworkState(
	private val connectivityManager: ConnectivityManager,
) : MediatorStateFlow<Boolean>(connectivityManager.isOnline()) {

	private val callback = NetworkCallbackImpl()

	@Synchronized
	override fun onActive() {
		invalidate()
		val request = NetworkRequest.Builder()
			.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
			.build()
		connectivityManager.registerNetworkCallback(request, callback)
	}

	@Synchronized
	override fun onInactive() {
		connectivityManager.unregisterNetworkCallback(callback)
	}

	suspend fun awaitForConnection() {
		if (value) {
			return
		}
		first { it }
	}

	private fun invalidate() {
		publishValue(connectivityManager.isOnline())
	}

	private inner class NetworkCallbackImpl : NetworkCallback() {

		override fun onAvailable(network: Network) = invalidate()

		override fun onLost(network: Network) = invalidate()

		override fun onUnavailable() = invalidate()
	}
}
