package com.example.dexreader.search.ui.suggestion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import coil.ImageLoader
import com.example.dexreader.R
import com.example.dexreader.core.ui.BaseFragment
import com.example.dexreader.core.util.ext.addMenuProvider
import com.example.dexreader.core.util.ext.observe
import com.example.dexreader.databinding.FragmentSearchSuggestionBinding
import com.example.dexreader.search.ui.suggestion.adapter.SearchSuggestionAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SearchSuggestionFragment :
	BaseFragment<FragmentSearchSuggestionBinding>(),
	SearchSuggestionItemCallback.SuggestionItemListener {

	@Inject
	lateinit var coil: ImageLoader

	private val viewModel by activityViewModels<SearchSuggestionViewModel>()

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = FragmentSearchSuggestionBinding.inflate(inflater, container, false)

	override fun onViewBindingCreated(binding: FragmentSearchSuggestionBinding, savedInstanceState: Bundle?) {
		super.onViewBindingCreated(binding, savedInstanceState)
		val adapter = SearchSuggestionAdapter(
			coil = coil,
			lifecycleOwner = viewLifecycleOwner,
			listener = requireActivity() as SearchSuggestionListener,
		)
		addMenuProvider(SearchSuggestionMenuProvider(binding.root.context, viewModel))
		binding.root.adapter = adapter
		binding.root.setHasFixedSize(true)
		viewModel.suggestion.observe(viewLifecycleOwner) {
			adapter.items = it
		}
		ItemTouchHelper(SearchSuggestionItemCallback(this))
			.attachToRecyclerView(binding.root)
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		val extraPadding = resources.getDimensionPixelOffset(R.dimen.list_spacing)
		requireViewBinding().root.updatePadding(
			top = extraPadding,
			right = insets.right,
			left = insets.left,
			bottom = insets.bottom,
		)
	}

	override fun onRemoveQuery(query: String) {
		viewModel.deleteQuery(query)
	}

	override fun onResume() {
		super.onResume()
		viewModel.onResume()
	}

	companion object {

		fun newInstance() = SearchSuggestionFragment()
	}
}
