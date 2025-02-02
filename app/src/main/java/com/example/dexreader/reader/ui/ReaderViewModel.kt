package com.example.dexreader.reader.ui

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.dexreader.R
import com.example.dexreader.bookmarks.domain.Bookmark
import com.example.dexreader.bookmarks.domain.BookmarksRepository
import com.example.dexreader.core.parser.MangaDataRepository
import com.example.dexreader.core.parser.MangaIntent
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.prefs.ReaderMode
import com.example.dexreader.core.prefs.ScreenshotsPolicy
import com.example.dexreader.core.prefs.observeAsFlow
import com.example.dexreader.core.prefs.observeAsStateFlow
import com.example.dexreader.core.ui.BaseViewModel
import com.example.dexreader.core.util.ext.MutableEventFlow
import com.example.dexreader.core.util.ext.call
import com.example.dexreader.core.util.ext.ifNullOrEmpty
import com.example.dexreader.core.util.ext.printStackTraceDebug
import com.example.dexreader.core.util.ext.requireValue
import com.example.dexreader.details.data.MangaDetails
import com.example.dexreader.details.domain.DetailsLoadUseCase
import com.example.dexreader.history.data.HistoryRepository
import com.example.dexreader.history.data.PROGRESS_NONE
import com.example.dexreader.history.domain.HistoryUpdateUseCase
import com.example.dexreader.reader.domain.ChaptersLoader
import com.example.dexreader.reader.domain.DetectReaderModeUseCase
import com.example.dexreader.reader.domain.PageLoader
import com.example.dexreader.reader.ui.config.ReaderSettings
import com.example.dexreader.reader.ui.pager.ReaderUiState
import com.example.dexreader.stats.domain.StatsCollector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.plus
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.model.MangaPage
import org.example.dexreader.parsers.util.assertNotNull
import java.time.Instant
import javax.inject.Inject

private const val BOUNDS_PAGE_OFFSET = 2

@HiltViewModel
class ReaderViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val dataRepository: MangaDataRepository,
	private val historyRepository: HistoryRepository,
	private val bookmarksRepository: BookmarksRepository,
	private val settings: AppSettings,
	private val pageSaveHelper: PageSaveHelper,
	private val pageLoader: PageLoader,
	private val chaptersLoader: ChaptersLoader,
	private val detailsLoadUseCase: DetailsLoadUseCase,
	private val historyUpdateUseCase: HistoryUpdateUseCase,
	private val detectReaderModeUseCase: DetectReaderModeUseCase,
	private val statsCollector: StatsCollector,
) : BaseViewModel() {

	private val intent = MangaIntent(savedStateHandle)
	private val preselectedBranch = savedStateHandle.get<String>(ReaderActivity.EXTRA_BRANCH)

	private var loadingJob: Job? = null
	private var pageSaveJob: Job? = null
	private var bookmarkJob: Job? = null
	private var stateChangeJob: Job? = null
	private val currentState = MutableStateFlow<ReaderState?>(savedStateHandle[ReaderActivity.EXTRA_STATE])
	private val mangaData = MutableStateFlow(intent.manga?.let { MangaDetails(it, null, null, false) })
	private val mangaFlow: Flow<Manga?>
		get() = mangaData.map { it?.toManga() }

	val readerMode = MutableStateFlow<ReaderMode?>(null)
	val onPageSaved = MutableEventFlow<Uri?>()
	val onShowToast = MutableEventFlow<Int>()
	val uiState = MutableStateFlow<ReaderUiState?>(null)

	val content = MutableStateFlow(ReaderContent(emptyList(), null))
	val manga: MangaDetails?
		get() = mangaData.value

	val pageAnimation = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_READER_ANIMATION,
		valueProducer = { readerAnimation },
	)

	val isInfoBarEnabled = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_READER_BAR,
		valueProducer = { isReaderBarEnabled },
	)

	val isKeepScreenOnEnabled = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_READER_SCREEN_ON,
		valueProducer = { isReaderKeepScreenOn },
	)

	val isWebtoonZooEnabled = observeIsWebtoonZoomEnabled()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, false)

	val defaultWebtoonZoomOut = observeIsWebtoonZoomEnabled().flatMapLatest {
		if (it) {
			observeWebtoonZoomOut()
		} else {
			flowOf(0f)
		}
	}.flowOn(Dispatchers.Default)

	val isZoomControlsEnabled = getObserveIsZoomControlEnabled().flatMapLatest { zoom ->
		if (zoom) {
			combine(readerMode, isWebtoonZooEnabled) { mode, ze -> ze || mode != ReaderMode.WEBTOON }
		} else {
			flowOf(false)
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, false)

	val readerSettings = ReaderSettings(
		parentScope = viewModelScope,
		settings = settings,
		colorFilterFlow = mangaFlow.flatMapLatest {
			if (it == null) flowOf(null) else dataRepository.observeColorFilter(it.id)
		}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, null),
	)

	val isScreenshotsBlockEnabled = combine(
		mangaFlow,
		settings.observeAsFlow(AppSettings.KEY_SCREENSHOTS_POLICY) { screenshotsPolicy },
	) { manga, policy ->
		policy == ScreenshotsPolicy.BLOCK_ALL ||
			(policy == ScreenshotsPolicy.BLOCK_NSFW && manga != null && manga.isNsfw)
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, false)

	val isBookmarkAdded = currentState.flatMapLatest { state ->
		val manga = mangaData.value?.toManga()
		if (state == null || manga == null) {
			flowOf(false)
		} else {
			bookmarksRepository.observeBookmark(manga, state.chapterId, state.page)
				.map {
					it != null && it.chapterId == state.chapterId && it.page == state.page
				}
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, false)

	init {
		loadImpl()
		settings.observe()
			.onEach { key ->
				if (key == AppSettings.KEY_READER_SLIDER) notifyStateChanged()
			}.launchIn(viewModelScope + Dispatchers.Default)
	}

	fun reload() {
		loadingJob?.cancel()
		loadImpl()
	}

	fun onPause() {
		manga?.let {
			statsCollector.onPause(it.id)
		}
	}

	fun switchMode(newMode: ReaderMode) {
		launchJob {
			val manga = checkNotNull(mangaData.value?.toManga())
			dataRepository.saveReaderMode(
				manga = manga,
				mode = newMode,
			)
			readerMode.value = newMode
			content.update {
				it.copy(state = getCurrentState())
			}
		}
	}

	fun saveCurrentState(state: ReaderState? = null) {
		if (state != null) {
			currentState.value = state
		}
		val readerState = state ?: currentState.value ?: return
		historyUpdateUseCase.invokeAsync(
			manga = mangaData.value?.toManga() ?: return,
			readerState = readerState,
			percent = computePercent(readerState.chapterId, readerState.page),
		)
	}

	fun getCurrentState() = currentState.value

	fun getCurrentChapterPages(): List<MangaPage>? {
		val chapterId = currentState.value?.chapterId ?: return null
		return chaptersLoader.getPages(chapterId).map { it.toMangaPage() }
	}

	fun saveCurrentPage(
		page: MangaPage,
		saveLauncher: ActivityResultLauncher<String>,
	) {
		val prevJob = pageSaveJob
		pageSaveJob = launchLoadingJob(Dispatchers.Default) {
			prevJob?.cancelAndJoin()
			try {
				val dest = pageSaveHelper.savePage(pageLoader, page, saveLauncher)
				onPageSaved.call(dest)
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				e.printStackTraceDebug()
				onPageSaved.call(null)
			}
		}
	}

	fun onActivityResult(uri: Uri?) {
		if (uri != null) {
			pageSaveHelper.onActivityResult(uri)
		} else {
			pageSaveJob?.cancel()
			pageSaveJob = null
		}
	}

	fun getCurrentPage(): MangaPage? {
		val state = currentState.value ?: return null
		return content.value.pages.find {
			it.chapterId == state.chapterId && it.index == state.page
		}?.toMangaPage()
	}

	fun switchChapter(id: Long, page: Int) {
		val prevJob = loadingJob
		loadingJob = launchLoadingJob(Dispatchers.Default) {
			prevJob?.cancelAndJoin()
			content.value = ReaderContent(emptyList(), null)
			chaptersLoader.loadSingleChapter(id)
			content.value = ReaderContent(chaptersLoader.snapshot(), ReaderState(id, page, 0))
		}
	}

	fun switchChapterBy(delta: Int) {
		val prevJob = loadingJob
		loadingJob = launchLoadingJob(Dispatchers.Default) {
			prevJob?.cancelAndJoin()
			val currentChapterId = currentState.requireValue().chapterId
			val allChapters = checkNotNull(manga).allChapters
			var index = allChapters.indexOfFirst { x -> x.id == currentChapterId }
			if (index < 0) {
				return@launchLoadingJob
			}
			index += delta
			val newChapterId = (allChapters.getOrNull(index) ?: return@launchLoadingJob).id
			content.value = ReaderContent(emptyList(), null)
			chaptersLoader.loadSingleChapter(newChapterId)
			content.value = ReaderContent(chaptersLoader.snapshot(), ReaderState(newChapterId, 0, 0))
		}
	}

	@MainThread
	fun onCurrentPageChanged(lowerPos: Int, upperPos: Int) {
		val prevJob = stateChangeJob
		val pages = content.value.pages // capture immediately
		stateChangeJob = launchJob(Dispatchers.Default) {
			prevJob?.cancelAndJoin()
			loadingJob?.join()
			if (pages.size != content.value.pages.size) {
				return@launchJob // TODO
			}
			val centerPos = (lowerPos + upperPos) / 2
			pages.getOrNull(centerPos)?.let { page ->
				currentState.update { cs ->
					cs?.copy(chapterId = page.chapterId, page = page.index)
				}
			}
			notifyStateChanged()
			if (pages.isEmpty() || loadingJob?.isActive == true) {
				return@launchJob
			}
			ensureActive()
			if (upperPos >= pages.lastIndex - BOUNDS_PAGE_OFFSET) {
				loadPrevNextChapter(pages.last().chapterId, isNext = true)
			}
			if (lowerPos <= BOUNDS_PAGE_OFFSET) {
				loadPrevNextChapter(pages.first().chapterId, isNext = false)
			}
		}
	}

	fun addBookmark() {
		if (bookmarkJob?.isActive == true) {
			return
		}
		bookmarkJob = launchJob(Dispatchers.Default) {
			loadingJob?.join()
			val state = checkNotNull(currentState.value)
			val page = checkNotNull(getCurrentPage()) { "Page not found" }
			val bookmark = Bookmark(
				manga = mangaData.requireValue().toManga(),
				pageId = page.id,
				chapterId = state.chapterId,
				page = state.page,
				scroll = state.scroll,
				imageUrl = page.preview.ifNullOrEmpty { page.url },
				createdAt = Instant.now(),
				percent = computePercent(state.chapterId, state.page),
			)
			bookmarksRepository.addBookmark(bookmark)
			onShowToast.call(R.string.bookmark_added)
		}
	}

	fun removeBookmark() {
		if (bookmarkJob?.isActive == true) {
			return
		}
		bookmarkJob = launchJob {
			loadingJob?.join()
			val manga = mangaData.requireValue().toManga()
			val state = checkNotNull(getCurrentState())
			bookmarksRepository.removeBookmark(manga.id, state.chapterId, state.page)
			onShowToast.call(R.string.bookmark_removed)
		}
	}

	private fun loadImpl() {
		loadingJob = launchLoadingJob(Dispatchers.Default) {
			val details = detailsLoadUseCase.invoke(intent).first { x -> x.isLoaded }
			mangaData.value = details
			chaptersLoader.init(details)
			val manga = details.toManga()
			// obtain state
			if (currentState.value == null) {
				currentState.value = historyRepository.getOne(manga)?.let {
					ReaderState(it)
				} ?: ReaderState(manga, preselectedBranch)
			}
			val mode = detectReaderModeUseCase.invoke(manga, currentState.value)
			val branch = chaptersLoader.peekChapter(currentState.value?.chapterId ?: 0L)?.branch
			mangaData.value = details.filterChapters(branch)
			readerMode.value = mode

			chaptersLoader.loadSingleChapter(requireNotNull(currentState.value).chapterId)
			// save state
			currentState.value?.let {
				val percent = computePercent(it.chapterId, it.page)
				historyUpdateUseCase.invoke(manga, it, percent)
			}
			notifyStateChanged()
			content.value = ReaderContent(chaptersLoader.snapshot(), currentState.value)
		}
	}

	@AnyThread
	private fun loadPrevNextChapter(currentId: Long, isNext: Boolean) {
		val prevJob = loadingJob
		loadingJob = launchLoadingJob(Dispatchers.Default) {
			prevJob?.join()
			chaptersLoader.loadPrevNextChapter(mangaData.requireValue(), currentId, isNext)
			content.value = ReaderContent(chaptersLoader.snapshot(), null)
		}
	}

	private fun <T> List<T>.trySublist(fromIndex: Int, toIndex: Int): List<T> {
		val fromIndexBounded = fromIndex.coerceAtMost(lastIndex)
		val toIndexBounded = toIndex.coerceIn(fromIndexBounded, lastIndex)
		return if (fromIndexBounded == toIndexBounded) {
			emptyList()
		} else {
			subList(fromIndexBounded, toIndexBounded)
		}
	}

	@WorkerThread
	private fun notifyStateChanged() {
		val state = getCurrentState().assertNotNull("state") ?: return
		val chapter = chaptersLoader.peekChapter(state.chapterId).assertNotNull("chapter") ?: return
		val m = manga.assertNotNull("manga") ?: return
		val chapterIndex = m.chapters[chapter.branch]?.indexOfFirst { it.id == chapter.id } ?: -1
		val newState = ReaderUiState(
			mangaName = m.toManga().title,
			branch = chapter.branch,
			chapterName = chapter.name,
			chapterNumber = chapterIndex + 1,
			chaptersTotal = m.chapters[chapter.branch]?.size ?: 0,
			totalPages = chaptersLoader.getPagesCount(chapter.id),
			currentPage = state.page,
			isSliderEnabled = settings.isReaderSliderEnabled,
			percent = computePercent(state.chapterId, state.page),
		)
		uiState.value = newState
		statsCollector.onStateChanged(m.id, state)
	}

	private fun computePercent(chapterId: Long, pageIndex: Int): Float {
		val branch = chaptersLoader.peekChapter(chapterId)?.branch
		val chapters = manga?.chapters?.get(branch) ?: return PROGRESS_NONE
		val chaptersCount = chapters.size
		val chapterIndex = chapters.indexOfFirst { x -> x.id == chapterId }
		val pagesCount = chaptersLoader.getPagesCount(chapterId)
		if (chaptersCount == 0 || pagesCount == 0) {
			return PROGRESS_NONE
		}
		val pagePercent = (pageIndex + 1) / pagesCount.toFloat()
		val ppc = 1f / chaptersCount
		return ppc * chapterIndex + ppc * pagePercent
	}

	private fun observeIsWebtoonZoomEnabled() = settings.observeAsFlow(
		key = AppSettings.KEY_WEBTOON_ZOOM,
		valueProducer = { isWebtoonZoomEnable },
	)

	private fun observeWebtoonZoomOut() = settings.observeAsFlow(
		key = AppSettings.KEY_WEBTOON_ZOOM_OUT,
		valueProducer = { defaultWebtoonZoomOut },
	)

	private fun getObserveIsZoomControlEnabled() = settings.observeAsFlow(
		key = AppSettings.KEY_READER_ZOOM_BUTTONS,
		valueProducer = { isReaderZoomButtonsEnabled },
	)
}
