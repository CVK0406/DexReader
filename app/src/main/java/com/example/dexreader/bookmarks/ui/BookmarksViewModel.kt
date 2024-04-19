package com.example.dexreader.bookmarks.ui

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import com.example.dexreader.R
import com.example.dexreader.bookmarks.domain.Bookmark
import com.example.dexreader.bookmarks.domain.BookmarksRepository
import com.example.dexreader.core.ui.BaseViewModel
import com.example.dexreader.core.ui.util.ReversibleAction
import com.example.dexreader.core.util.ext.MutableEventFlow
import com.example.dexreader.core.util.ext.call
import com.example.dexreader.list.ui.model.EmptyState
import com.example.dexreader.list.ui.model.ListHeader
import com.example.dexreader.list.ui.model.ListModel
import com.example.dexreader.list.ui.model.LoadingState
import com.example.dexreader.list.ui.model.toErrorState
import org.example.dexreader.parsers.model.Manga
import javax.inject.Inject

@HiltViewModel
class BookmarksViewModel @Inject constructor(
	private val repository: BookmarksRepository,
) : BaseViewModel() {

	val onActionDone = MutableEventFlow<ReversibleAction>()

	val content: StateFlow<List<ListModel>> = repository.observeBookmarks()
		.map { list ->
			if (list.isEmpty()) {
				listOf(
					EmptyState(
						icon = R.drawable.ic_empty_favourites,
						textPrimary = R.string.no_bookmarks_yet,
						textSecondary = R.string.no_bookmarks_summary,
						actionStringRes = 0,
					),
				)
			} else {
				mapList(list)
			}
		}
		.catch { e -> emit(listOf(e.toErrorState(canRetry = false))) }
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(LoadingState))

	fun removeBookmarks(ids: Set<Long>) {
		launchJob(Dispatchers.Default) {
			val handle = repository.removeBookmarks(ids)
			onActionDone.call(ReversibleAction(R.string.bookmarks_removed, handle))
		}
	}

	private fun mapList(data: Map<Manga, List<Bookmark>>): List<ListModel> {
		val result = ArrayList<ListModel>(data.values.sumOf { it.size + 1 })
		for ((manga, bookmarks) in data) {
			result.add(ListHeader(manga.title, R.string.more, manga))
			result.addAll(bookmarks)
		}
		return result
	}
}
