package com.example.dexreader.search.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.fragment.app.commit
import com.example.dexreader.R
import com.example.dexreader.core.ui.BaseActivity
import com.example.dexreader.core.util.ext.getSerializableExtraCompat
import com.example.dexreader.core.util.ext.showKeyboard
import com.example.dexreader.databinding.ActivitySearchBinding
import com.example.dexreader.search.ui.suggestion.SearchSuggestionViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.example.dexreader.parsers.model.MangaSource

@AndroidEntryPoint
class SearchActivity : BaseActivity<ActivitySearchBinding>(), SearchView.OnQueryTextListener {

	private val searchSuggestionViewModel by viewModels<SearchSuggestionViewModel>()
	private lateinit var source: MangaSource

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivitySearchBinding.inflate(layoutInflater))
		source = intent.getSerializableExtraCompat(EXTRA_SOURCE) ?: run {
			finishAfterTransition()
			return
		}
		val query = intent.getStringExtra(EXTRA_QUERY)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		with(viewBinding.searchView) {
			queryHint = getString(R.string.search_on_s, source.title)
			setOnQueryTextListener(this@SearchActivity)

			if (query.isNullOrBlank()) {
				requestFocus()
				showKeyboard()
			} else {
				setQuery(query, true)
			}
		}
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		viewBinding.toolbar.updatePadding(
			left = insets.left,
			right = insets.right,
			top = insets.top
		)
		viewBinding.container.updatePadding(
			bottom = insets.bottom,
		)
	}

	override fun onQueryTextSubmit(query: String?): Boolean {
		val q = query?.trim()
		if (q.isNullOrEmpty()) {
			return false
		}
		title = query
		supportFragmentManager.commit {
			setReorderingAllowed(true)
			replace(R.id.container, SearchFragment.newInstance(source, q))
		}
		viewBinding.searchView.clearFocus()
		searchSuggestionViewModel.saveQuery(q)
		return true
	}

	override fun onQueryTextChange(newText: String?): Boolean = false

	companion object {

		private const val EXTRA_SOURCE = "source"
		private const val EXTRA_QUERY = "query"

		fun newIntent(context: Context, source: MangaSource, query: String?) =
			Intent(context, SearchActivity::class.java)
				.putExtra(EXTRA_SOURCE, source)
				.putExtra(EXTRA_QUERY, query)
	}
}
