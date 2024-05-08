package com.example.dexreader.list.ui.preview

import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.text.getSpans
import androidx.core.text.parseAsHtml
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.dexreader.core.model.getPreferredBranch
import com.example.dexreader.core.model.parcelable.ParcelableManga
import com.example.dexreader.core.parser.MangaIntent
import com.example.dexreader.core.parser.MangaRepository
import com.example.dexreader.core.ui.BaseViewModel
import com.example.dexreader.core.ui.widgets.ChipsView
import com.example.dexreader.core.util.ext.require
import com.example.dexreader.core.util.ext.sanitize
import com.example.dexreader.history.data.HistoryRepository
import com.example.dexreader.list.domain.ListExtraProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.plus
import javax.inject.Inject

@HiltViewModel
class PreviewViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val extraProvider: ListExtraProvider,
	private val repositoryFactory: MangaRepository.Factory,
	private val historyRepository: HistoryRepository,
	private val imageGetter: Html.ImageGetter,
) : BaseViewModel() {

	val manga = MutableStateFlow(
		savedStateHandle.require<ParcelableManga>(MangaIntent.KEY_MANGA).manga,
	)

	val footer = combine(
		manga,
		historyRepository.observeOne(manga.value.id)
	) { m, history ->
		if (m.chapters == null) {
			return@combine null
		}
		val b = m.getPreferredBranch(history)
		val chapters = m.getChapters(b).orEmpty()
		FooterInfo(
			branch = b,
			currentChapter = history?.chapterId?.let {
				chapters.indexOfFirst { x -> x.id == it }
			} ?: -1,
			totalChapters = chapters.size
		)
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, null)

	val description = manga
		.distinctUntilChangedBy { it.description.orEmpty() }
		.transformLatest {
			val description = it.description
			if (description.isNullOrEmpty()) {
				emit(null)
			} else {
				emit(description.parseAsHtml().filterSpans().sanitize())
				emit(description.parseAsHtml(imageGetter = imageGetter).filterSpans())
			}
		}.combine(isLoading) { desc, loading ->
			if (loading) null else desc ?: ""
		}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.WhileSubscribed(5000), null)

	val tagsChips = manga.map {
		it.tags.map { tag ->
			ChipsView.ChipModel(
				title = tag.title,
				tint = extraProvider.getTagTint(tag),
				icon = 0,
				data = tag,
				isCheckable = false,
				isChecked = false,
			)
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, emptyList())

	init {
		launchLoadingJob(Dispatchers.Default) {
			val repo = repositoryFactory.create(manga.value.source)
			manga.value = repo.getDetails(manga.value)
		}
	}

	private fun Spanned.filterSpans(): CharSequence {
		val spannable = SpannableString.valueOf(this)
		val spans = spannable.getSpans<ForegroundColorSpan>()
		for (span in spans) {
			spannable.removeSpan(span)
		}
		return spannable.trim()
	}

	data class FooterInfo(
		val branch: String?,
		val currentChapter: Int,
		val totalChapters: Int
	) {
		fun isInProgress() = currentChapter >= 0
	}
}
