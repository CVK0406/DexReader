package com.example.dexreader.reader.domain

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Size
import com.example.dexreader.core.model.findChapter
import com.example.dexreader.core.network.ImageProxyInterceptor
import com.example.dexreader.core.network.MangaHttpClient
import com.example.dexreader.core.parser.MangaDataRepository
import com.example.dexreader.core.parser.MangaRepository
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.prefs.ReaderMode
import com.example.dexreader.core.util.ext.printStackTraceDebug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import okhttp3.OkHttpClient
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.model.MangaPage
import org.example.dexreader.parsers.util.runCatchingCancellable
import com.example.dexreader.reader.ui.ReaderState
import java.io.InputStream
import java.util.zip.ZipFile
import javax.inject.Inject
import kotlin.math.roundToInt

class DetectReaderModeUseCase @Inject constructor(
	private val dataRepository: MangaDataRepository,
	private val settings: AppSettings,
	private val mangaRepositoryFactory: MangaRepository.Factory,
	@MangaHttpClient private val okHttpClient: OkHttpClient,
	private val imageProxyInterceptor: ImageProxyInterceptor,
) {

	suspend operator fun invoke(manga: Manga, state: ReaderState?): ReaderMode {
		dataRepository.getReaderMode(manga.id)?.let { return it }
		val defaultMode = settings.defaultReaderMode
		if (!settings.isReaderModeDetectionEnabled || defaultMode == ReaderMode.WEBTOON) {
			return defaultMode
		}
		val chapter = state?.let { manga.findChapter(it.chapterId) }
			?: manga.chapters?.firstOrNull()
			?: error("There are no chapters in this manga")
		val repo = mangaRepositoryFactory.create(manga.source)
		val pages = repo.getPages(chapter)
		return runCatchingCancellable {
			val isWebtoon = guessMangaIsWebtoon(repo, pages)
			if (isWebtoon) ReaderMode.WEBTOON else defaultMode
		}.onSuccess {
			dataRepository.saveReaderMode(manga, it)
		}.onFailure {
			it.printStackTraceDebug()
		}.getOrDefault(defaultMode)
	}

	/**
	 * Automatic determine type of manga by page size
	 * @return ReaderMode.WEBTOON if page is wide
	 */
	private suspend fun guessMangaIsWebtoon(repository: MangaRepository, pages: List<MangaPage>): Boolean {
		val pageIndex = (pages.size * 0.3).roundToInt()
		val page = requireNotNull(pages.getOrNull(pageIndex)) { "No pages" }
		val url = repository.getPageUrl(page)
		val uri = Uri.parse(url)
		val size = if (uri.scheme == "cbz") {
			runInterruptible(Dispatchers.IO) {
				val zip = ZipFile(uri.schemeSpecificPart)
				val entry = zip.getEntry(uri.fragment)
				zip.getInputStream(entry).use {
					getBitmapSize(it)
				}
			}
		} else {
			val request = PageLoader.createPageRequest(page, url)
			imageProxyInterceptor.interceptPageRequest(request, okHttpClient).use {
				runInterruptible(Dispatchers.IO) {
					getBitmapSize(it.body?.byteStream())
				}
			}
		}
		return size.width * MIN_WEBTOON_RATIO < size.height
	}

	companion object {

		private const val MIN_WEBTOON_RATIO = 1.8

		private fun getBitmapSize(input: InputStream?): Size {
			val options = BitmapFactory.Options().apply {
				inJustDecodeBounds = true
			}
			BitmapFactory.decodeStream(input, null, options)?.recycle()
			val imageHeight: Int = options.outHeight
			val imageWidth: Int = options.outWidth
			check(imageHeight > 0 && imageWidth > 0)
			return Size(imageWidth, imageHeight)
		}
	}
}
