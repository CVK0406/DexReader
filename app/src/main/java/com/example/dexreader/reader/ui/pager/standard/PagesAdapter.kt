package com.example.dexreader.reader.ui.pager.standard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.example.dexreader.core.exceptions.resolve.ExceptionResolver
import com.example.dexreader.core.os.NetworkState
import com.example.dexreader.databinding.ItemPageBinding
import com.example.dexreader.reader.domain.PageLoader
import com.example.dexreader.reader.ui.config.ReaderSettings
import com.example.dexreader.reader.ui.pager.BaseReaderAdapter

class PagesAdapter(
	private val lifecycleOwner: LifecycleOwner,
	loader: PageLoader,
	settings: ReaderSettings,
	networkState: NetworkState,
	exceptionResolver: ExceptionResolver,
) : BaseReaderAdapter<PageHolder>(loader, settings, networkState, exceptionResolver) {

	override fun onCreateViewHolder(
		parent: ViewGroup,
		loader: PageLoader,
		settings: ReaderSettings,
		networkState: NetworkState,
		exceptionResolver: ExceptionResolver,
	) = PageHolder(
		owner = lifecycleOwner,
		binding = ItemPageBinding.inflate(LayoutInflater.from(parent.context), parent, false),
		loader = loader,
		settings = settings,
		networkState = networkState,
		exceptionResolver = exceptionResolver,
	)
}
