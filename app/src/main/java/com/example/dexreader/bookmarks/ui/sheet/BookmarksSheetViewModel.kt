package com.example.dexreader.bookmarks.ui.sheet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import com.example.dexreader.bookmarks.domain.Bookmark
import com.example.dexreader.bookmarks.domain.BookmarksRepository
import com.example.dexreader.core.model.parcelable.ParcelableManga
import com.example.dexreader.core.parser.MangaRepository
import com.example.dexreader.core.ui.BaseViewModel
import com.example.dexreader.core.util.ext.require
import com.example.dexreader.list.ui.model.ListHeader
import com.example.dexreader.list.ui.model.ListModel
import com.example.dexreader.list.ui.model.LoadingFooter
import org.example.dexreader.parsers.util.SuspendLazy
import javax.inject.Inject

@HiltViewModel
class BookmarksSheetViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	mangaRepositoryFactory: MangaRepository.Factory,
	bookmarksRepository: BookmarksRepository,
) : BaseViewModel() {

	val manga = savedStateHandle.require<ParcelableManga>(BookmarksSheet.ARG_MANGA).manga
	private val chaptersLazy = SuspendLazy {
		requireNotNull(manga.chapters ?: mangaRepositoryFactory.create(manga.source).getDetails(manga).chapters)
	}

	val content: StateFlow<List<ListModel>> = bookmarksRepository.observeBookmarks(manga)
		.map { mapList(it) }
		.withErrorHandling()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, listOf(LoadingFooter()))

	private suspend fun mapList(bookmarks: List<Bookmark>): List<ListModel> {
		val chapters = chaptersLazy.get()
		val bookmarksMap = bookmarks.groupBy { it.chapterId }
		val result = ArrayList<ListModel>(bookmarks.size + bookmarksMap.size)
		for (chapter in chapters) {
			val b = bookmarksMap[chapter.id]
			if (b.isNullOrEmpty()) {
				continue
			}
			result += ListHeader(chapter.name)
			result.addAll(b)
		}
		return result
	}
}
