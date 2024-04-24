package com.example.dexreader.reader.domain

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.AnyThread
import androidx.collection.LongSparseArray
import androidx.collection.set
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.example.dexreader.core.network.CommonHeaders
import com.example.dexreader.core.network.ImageProxyInterceptor
import com.example.dexreader.core.network.MangaHttpClient
import com.example.dexreader.core.parser.MangaRepository
import com.example.dexreader.core.prefs.AppSettings
import com.example.dexreader.core.util.FileSize
import com.example.dexreader.core.util.RetainedLifecycleCoroutineScope
import com.example.dexreader.core.util.ext.URI_SCHEME_ZIP
import com.example.dexreader.core.util.ext.compressToPNG
import com.example.dexreader.core.util.ext.ensureRamAtLeast
import com.example.dexreader.core.util.ext.ensureSuccess
import com.example.dexreader.core.util.ext.exists
import com.example.dexreader.core.util.ext.getCompletionResultOrNull
import com.example.dexreader.core.util.ext.isTargetNotEmpty
import com.example.dexreader.core.util.ext.printStackTraceDebug
import com.example.dexreader.core.util.ext.ramAvailable
import com.example.dexreader.core.util.ext.withProgress
import com.example.dexreader.core.util.progress.ProgressDeferred
import com.example.dexreader.local.data.PagesCache
import com.example.dexreader.local.data.isFileUri
import com.example.dexreader.local.data.isZipUri
import com.example.dexreader.reader.ui.pager.ReaderPage
import dagger.hilt.android.ActivityRetainedLifecycle
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.use
import org.example.dexreader.parsers.model.MangaPage
import org.example.dexreader.parsers.model.MangaSource
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipFile
import javax.inject.Inject
import kotlin.concurrent.Volatile
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

@ActivityRetainedScoped
class PageLoader @Inject constructor(
	@ApplicationContext private val context: Context,
	lifecycle: ActivityRetainedLifecycle,
	@MangaHttpClient private val okHttp: OkHttpClient,
	private val cache: PagesCache,
	private val settings: AppSettings,
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val imageProxyInterceptor: ImageProxyInterceptor,
) {

	val loaderScope = RetainedLifecycleCoroutineScope(lifecycle) + InternalErrorHandler() + Dispatchers.Default

	private val tasks = LongSparseArray<ProgressDeferred<Uri, Float>>()
	private val semaphore = Semaphore(3)
	private val convertLock = Mutex()
	private val prefetchLock = Mutex()

	@Volatile
	private var repository: MangaRepository? = null
	private val prefetchQueue = LinkedList<MangaPage>()
	private val counter = AtomicInteger(0)
	private var prefetchQueueLimit = PREFETCH_LIMIT_DEFAULT // TODO adaptive

	@AnyThread
	fun prefetch(pages: List<ReaderPage>) = loaderScope.launch {
		prefetchLock.withLock {
			for (page in pages.asReversed()) {
				if (tasks.containsKey(page.id)) {
					continue
				}
				prefetchQueue.offerFirst(page.toMangaPage())
				if (prefetchQueue.size > prefetchQueueLimit) {
					prefetchQueue.pollLast()
				}
			}
		}
		if (counter.get() == 0) {
			onIdle()
		}
	}

	fun loadPageAsync(page: MangaPage, force: Boolean): ProgressDeferred<Uri, Float> {
		var task = tasks[page.id]?.takeIf { it.isValid() }
		if (force) {
			task?.cancel()
		} else if (task?.isCancelled == false) {
			return task
		}
		task = loadPageAsyncImpl(page, force)
		synchronized(tasks) {
			tasks[page.id] = task
		}
		return task
	}

	suspend fun loadPage(page: MangaPage, force: Boolean): Uri {
		return loadPageAsync(page, force).await()
	}

	suspend fun convertBimap(uri: Uri): Uri = convertLock.withLock {
		if (uri.isZipUri()) {
			val bitmap = runInterruptible(Dispatchers.IO) {
				ZipFile(uri.schemeSpecificPart).use { zip ->
					val entry = zip.getEntry(uri.fragment)
					context.ensureRamAtLeast(entry.size * 2)
					zip.getInputStream(zip.getEntry(uri.fragment)).use {
						BitmapFactory.decodeStream(it)
					}
				}
			}
			cache.put(uri.toString(), bitmap).toUri()
		} else {
			val file = uri.toFile()
			context.ensureRamAtLeast(file.length() * 2)
			val image = runInterruptible(Dispatchers.IO) {
				BitmapFactory.decodeFile(file.absolutePath)
			}
			try {
				image.compressToPNG(file)
			} finally {
				image.recycle()
			}
			uri
		}
	}

	suspend fun getPageUrl(page: MangaPage): String {
		return getRepository(page.source).getPageUrl(page)
	}

	private fun onIdle() = loaderScope.launch {
		prefetchLock.withLock {
			while (prefetchQueue.isNotEmpty()) {
				val page = prefetchQueue.pollFirst() ?: return@launch
				if (cache.get(page.url) == null) {
					synchronized(tasks) {
						tasks[page.id] = loadPageAsyncImpl(page, false)
					}
					return@launch
				}
			}
		}
	}

	private fun loadPageAsyncImpl(page: MangaPage, skipCache: Boolean): ProgressDeferred<Uri, Float> {
		val progress = MutableStateFlow(PROGRESS_UNDEFINED)
		val deferred = loaderScope.async {
			if (!skipCache) {
				cache.get(page.url)?.let { return@async it.toUri() }
			}
			counter.incrementAndGet()
			try {
				loadPageImpl(page, progress)
			} finally {
				if (counter.decrementAndGet() == 0) {
					onIdle()
				}
			}
		}
		return ProgressDeferred(deferred, progress)
	}

	@Synchronized
	private fun getRepository(source: MangaSource): MangaRepository {
		val result = repository
		return if (result != null && result.source == source) {
			result
		} else {
			mangaRepositoryFactory.create(source).also { repository = it }
		}
	}

	private suspend fun loadPageImpl(page: MangaPage, progress: MutableStateFlow<Float>): Uri = semaphore.withPermit {
		val pageUrl = getPageUrl(page)
		check(pageUrl.isNotBlank()) { "Cannot obtain full image url for $page" }
		val uri = Uri.parse(pageUrl)
		return when {
			uri.isZipUri() -> if (uri.scheme == URI_SCHEME_ZIP) {
				uri
			} else { // legacy uri
				uri.buildUpon().scheme(URI_SCHEME_ZIP).build()
			}

			uri.isFileUri() -> uri
			else -> {
				val request = createPageRequest(page, pageUrl)
				imageProxyInterceptor.interceptPageRequest(request, okHttp).ensureSuccess().use { response ->
					val body = checkNotNull(response.body) { "Null response body" }
					body.withProgress(progress).use {
						cache.put(pageUrl, it.source())
					}
				}.toUri()
			}
		}
	}

	private fun isLowRam(): Boolean {
		return context.ramAvailable <= FileSize.MEGABYTES.convert(PREFETCH_MIN_RAM_MB, FileSize.BYTES)
	}

	private fun Deferred<Uri>.isValid(): Boolean {
		return getCompletionResultOrNull()?.map { uri ->
			uri.exists() && uri.isTargetNotEmpty()
		}?.getOrDefault(false) ?: true
	}

	private class InternalErrorHandler : AbstractCoroutineContextElement(CoroutineExceptionHandler),
		CoroutineExceptionHandler {

		override fun handleException(context: CoroutineContext, exception: Throwable) {
			exception.printStackTraceDebug()
		}
	}

	companion object {

		private const val PROGRESS_UNDEFINED = -1f
		private const val PREFETCH_LIMIT_DEFAULT = 6
		private const val PREFETCH_MIN_RAM_MB = 80L

		fun createPageRequest(page: MangaPage, pageUrl: String) = Request.Builder()
			.url(pageUrl)
			.get()
			.header(CommonHeaders.ACCEPT, "image/webp,image/png;q=0.9,image/jpeg,*/*;q=0.8")
			.cacheControl(CommonHeaders.CACHE_CONTROL_NO_STORE)
			.tag(MangaSource::class.java, page.source)
			.build()
	}
}
