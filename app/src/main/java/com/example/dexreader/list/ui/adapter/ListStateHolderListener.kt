package com.example.dexreader.list.ui.adapter

interface ListStateHolderListener {

	fun onRetryClick(error: Throwable)
	
	fun onEmptyActionClick()
}
