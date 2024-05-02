package com.example.dexreader.details.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.plus
import okio.FileNotFoundException
import com.example.dexreader.R
import com.example.dexreader.bookmarks.domain.Bookmark
import com.example.dexreader.bookmarks.domain.BookmarksRepository
import com.example.dexreader.core.model.findById
import com.example.dexreader.core.model.getPreferredBranch
import com.example.dexreader.core.parser.MangaIntent
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.prefs.ListMode
import com.example.dexreader.core.prefs.observeAsStateFlow
import com.example.dexreader.core.ui.BaseViewModel
import com.example.dexreader.core.ui.util.ReversibleAction
import com.example.dexreader.core.util.ext.MutableEventFlow
import com.example.dexreader.core.util.ext.call
import com.example.dexreader.core.util.ext.combine
import com.example.dexreader.core.util.ext.computeSize
import com.example.dexreader.core.util.ext.onEachWhile
import com.example.dexreader.core.util.ext.requireValue
import com.example.dexreader.details.data.MangaDetails
import com.example.dexreader.details.domain.BranchComparator
import com.example.dexreader.details.domain.DetailsInteractor
import com.example.dexreader.details.domain.DetailsLoadUseCase
import com.example.dexreader.details.domain.ProgressUpdateUseCase
import com.example.dexreader.details.domain.ReadingTimeUseCase
import com.example.dexreader.details.domain.RelatedMangaUseCase
import com.example.dexreader.details.ui.model.ChapterListItem
import com.example.dexreader.details.ui.model.HistoryInfo
import com.example.dexreader.details.ui.model.MangaBranch
import com.example.dexreader.download.ui.worker.DownloadWorker
import com.example.dexreader.history.data.HistoryRepository
import com.example.dexreader.list.domain.ListExtraProvider
import com.example.dexreader.list.ui.model.MangaItemModel
import com.example.dexreader.list.ui.model.toUi
import com.example.dexreader.local.data.LocalStorageChanges
import com.example.dexreader.local.domain.DeleteLocalMangaUseCase
import com.example.dexreader.local.domain.model.LocalManga
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.util.runCatchingCancellable
import com.example.dexreader.stats.data.StatsRepository
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
	private val historyRepository: HistoryRepository,
	private val bookmarksRepository: BookmarksRepository,
	private val settings: AppSettings,
	@LocalStorageChanges private val localStorageChanges: SharedFlow<LocalManga?>,
	private val downloadScheduler: DownloadWorker.Scheduler,
	private val interactor: DetailsInteractor,
	savedStateHandle: SavedStateHandle,
	private val deleteLocalMangaUseCase: DeleteLocalMangaUseCase,
	private val relatedMangaUseCase: RelatedMangaUseCase,
	private val extraProvider: ListExtraProvider,
	private val detailsLoadUseCase: DetailsLoadUseCase,
	private val progressUpdateUseCase: ProgressUpdateUseCase,
	private val readingTimeUseCase: ReadingTimeUseCase,
	private val statsRepository: StatsRepository,
) : BaseViewModel() {

	private val intent = MangaIntent(savedStateHandle)
	private val mangaId = intent.mangaId
	private var loadingJob: Job

	val onActionDone = MutableEventFlow<ReversibleAction>()
	val onShowTip = MutableEventFlow<Unit>()
	val onSelectChapter = MutableEventFlow<Long>()
	val onDownloadStarted = MutableEventFlow<Unit>()

	val details = MutableStateFlow(intent.manga?.let { MangaDetails(it, null, null, false) })
	val manga = details.map { x -> x?.toManga() }
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, null)

	val history = historyRepository.observeOne(mangaId)
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, null)

	val favouriteCategories = interactor.observeIsFavourite(mangaId)
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, false)

	val isStatsAvailable = statsRepository.observeHasStats(mangaId)
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, false)

	val remoteManga = MutableStateFlow<Manga?>(null)

	val newChaptersCount = details.flatMapLatest { d ->
		if (d?.isLocal == false) {
			interactor.observeNewChapters(mangaId)
		} else {
			flowOf(0)
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, 0)

	private val chaptersQuery = MutableStateFlow("")
	val selectedBranch = MutableStateFlow<String?>(null)

	val isChaptersReversed = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_REVERSE_CHAPTERS,
		valueProducer = { isChaptersReverse },
	)

	val isChaptersInGridView = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_GRID_VIEW_CHAPTERS,
		valueProducer = { isChaptersGridView },
	)

	val historyInfo: StateFlow<HistoryInfo> = combine(
		manga,
		selectedBranch,
		history
	) { m, b, h ->
		HistoryInfo(m, b, h)
	}.stateIn(
		scope = viewModelScope + Dispatchers.Default,
		started = SharingStarted.Eagerly,
		initialValue = HistoryInfo(null, null, null),
	)

	val bookmarks = manga.flatMapLatest {
		if (it != null) bookmarksRepository.observeBookmarks(it) else flowOf(emptyList())
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, emptyList())

	val localSize = details
		.map { it?.local }
		.distinctUntilChanged()
		.combine(localStorageChanges.onStart { emit(null) }) { x, _ -> x }
		.map { local ->
			if (local != null) {
				runCatchingCancellable {
					local.file.computeSize()
				}.getOrDefault(0L)
			} else {
				0L
			}
		}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.WhileSubscribed(5000), 0L)

	@Deprecated("")
	val description = details
		.map { it?.description }
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, null)

	val onMangaRemoved = MutableEventFlow<Manga>()

	val relatedManga: StateFlow<List<MangaItemModel>> = manga.mapLatest {
		if (it != null && settings.isRelatedMangaEnabled) {
			relatedMangaUseCase.invoke(it)?.toUi(ListMode.GRID, extraProvider).orEmpty()
		} else {
			emptyList()
		}
	}.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

	val branches: StateFlow<List<MangaBranch>> = combine(
		details,
		selectedBranch,
		history,
	) { m, b, h ->
		val c = m?.chapters
		if (c.isNullOrEmpty()) {
			return@combine emptyList()
		}
		val currentBranch = h?.let { m.allChapters.findById(it.chapterId) }?.branch
		c.map { x ->
			MangaBranch(
				name = x.key,
				count = x.value.size,
				isSelected = x.key == b,
				isCurrent = h != null && x.key == currentBranch,
			)
		}.sortedWith(BranchComparator())
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, emptyList())

	val isChaptersEmpty: StateFlow<Boolean> = details.map {
		it != null && it.isLoaded && it.allChapters.isEmpty()
	}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

	val chapters = combine(
		combine(
			details,
			history,
			selectedBranch,
			newChaptersCount,
			bookmarks,
			isChaptersInGridView,
		) { manga, history, branch, news, bookmarks, grid ->
			manga?.mapChapters(
				history,
				news,
				branch,
				bookmarks,
				grid,
			).orEmpty()
		},
		isChaptersReversed,
		chaptersQuery,
	) { list, reversed, query ->
		(if (reversed) list.asReversed() else list).filterSearch(query)
	}.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

	val readingTime = combine(
		details,
		selectedBranch,
		history,
	) { m, b, h ->
		readingTimeUseCase.invoke(m, b, h)
	}.stateIn(viewModelScope, SharingStarted.Lazily, null)

	val selectedBranchValue: String?
		get() = selectedBranch.value

	init {
		loadingJob = doLoad()
		launchJob(Dispatchers.Default) {
			localStorageChanges
				.collect { onDownloadComplete(it) }
		}
		launchJob(Dispatchers.Default) {
			if (settings.isTipEnabled(DetailsActivity.TIP_BUTTON)) {
				manga.filterNot { it?.chapters.isNullOrEmpty() }.first()
				onShowTip.call(Unit)
			}
		}
		launchJob(Dispatchers.Default) {
			val manga = details.firstOrNull { !it?.chapters.isNullOrEmpty() } ?: return@launchJob
			val h = history.firstOrNull()
			if (h != null) {
				progressUpdateUseCase(manga.toManga())
			}
		}
		launchJob(Dispatchers.Default) {
			val manga = details.firstOrNull { it != null && it.isLocal } ?: return@launchJob
			remoteManga.value = interactor.findRemote(manga.toManga())
		}
	}

	fun reload() {
		loadingJob.cancel()
		loadingJob = doLoad()
	}

	fun deleteLocal() {
		val m = details.value?.local?.manga
		if (m == null) {
			errorEvent.call(FileNotFoundException())
			return
		}
		launchLoadingJob(Dispatchers.Default) {
			deleteLocalMangaUseCase(m)
			onMangaRemoved.call(m)
		}
	}

	fun removeBookmark(bookmark: Bookmark) {
		launchJob(Dispatchers.Default) {
			bookmarksRepository.removeBookmark(bookmark)
			onActionDone.call(ReversibleAction(R.string.bookmark_removed, null))
		}
	}

	fun setChaptersReversed(newValue: Boolean) {
		settings.isChaptersReverse = newValue
	}

	fun setChaptersInGridView(newValue: Boolean) {
		settings.isChaptersGridView = newValue
	}

	fun setSelectedBranch(branch: String?) {
		selectedBranch.value = branch
	}

	fun performChapterSearch(query: String?) {
		chaptersQuery.value = query?.trim().orEmpty()
	}

	fun markChapterAsCurrent(chapterId: Long) {
		launchJob(Dispatchers.Default) {
			val manga = checkNotNull(details.value)
			val chapters = checkNotNull(manga.chapters[selectedBranchValue])
			val chapterIndex = chapters.indexOfFirst { it.id == chapterId }
			check(chapterIndex in chapters.indices) { "Chapter not found" }
			val percent = chapterIndex / chapters.size.toFloat()
			historyRepository.addOrUpdate(
				manga = manga.toManga(),
				chapterId = chapterId,
				page = 0,
				scroll = 0,
				percent = percent,
				force = true,
			)
		}
	}

	fun download(chaptersIds: Set<Long>?) {
		launchJob(Dispatchers.Default) {
			downloadScheduler.schedule(
				details.requireValue().toManga(),
				chaptersIds,
			)
			onDownloadStarted.call(Unit)
		}
	}

	fun startChaptersSelection() {
		val chapters = chapters.value
		val chapter = chapters.find {
			it.isUnread && !it.isDownloaded
		} ?: chapters.firstOrNull() ?: return
		onSelectChapter.call(chapter.chapter.id)
	}

	fun onButtonTipClosed() {
		settings.closeTip(DetailsActivity.TIP_BUTTON)
	}

	fun removeFromHistory() {
		launchJob(Dispatchers.Default) {
			val handle = historyRepository.delete(setOf(mangaId))
			onActionDone.call(ReversibleAction(R.string.removed_from_history, handle))
		}
	}

	private fun doLoad() = launchLoadingJob(Dispatchers.Default) {
		detailsLoadUseCase.invoke(intent)
			.onEachWhile {
				if (it.allChapters.isEmpty()) {
					return@onEachWhile false
				}
				val manga = it.toManga()
				// find default branch
				val hist = historyRepository.getOne(manga)
				selectedBranch.value = manga.getPreferredBranch(hist)
				true
			}.collect {
				details.value = it
			}
	}

	private fun List<ChapterListItem>.filterSearch(query: String): List<ChapterListItem> {
		if (query.isEmpty() || this.isEmpty()) {
			return this
		}
		return filter {
			it.chapter.name.contains(query, ignoreCase = true)
		}
	}

	private suspend fun onDownloadComplete(downloadedManga: LocalManga?) {
		downloadedManga ?: return
		launchJob {
			details.update {
				interactor.updateLocal(it, downloadedManga)
			}
		}
	}
}
