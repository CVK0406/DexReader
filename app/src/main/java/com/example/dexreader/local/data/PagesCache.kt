package com.example.dexreader.local.data

import android.content.Context
import android.graphics.Bitmap
import android.os.StatFs
import com.example.dexreader.core.exceptions.NoDataReceivedException
import com.example.dexreader.core.util.FileSize
import com.example.dexreader.core.util.ext.compressToPNG
import com.example.dexreader.core.util.ext.longHashCode
import com.example.dexreader.core.util.ext.printStackTraceDebug
import com.example.dexreader.core.util.ext.subdir
import com.example.dexreader.core.util.ext.takeIfReadable
import com.example.dexreader.core.util.ext.takeIfWriteable
import com.example.dexreader.core.util.ext.writeAllCancellable
import com.tomclaw.cache.DiskLruCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import okio.Source
import okio.buffer
import okio.sink
import okio.use
import org.example.dexreader.parsers.util.SuspendLazy
import org.example.dexreader.parsers.util.runCatchingCancellable
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PagesCache @Inject constructor(@ApplicationContext context: Context) {

	private val cacheDir = SuspendLazy {
		val dirs = context.externalCacheDirs + context.cacheDir
		dirs.firstNotNullOf {
			it?.subdir(CacheDir.PAGES.dir)?.takeIfWriteable()
		}
	}
	private val lruCache = SuspendLazy {
		val dir = cacheDir.get()
		val availableSize = (getAvailableSize() * 0.8).toLong()
		val size = SIZE_DEFAULT.coerceAtMost(availableSize).coerceAtLeast(SIZE_MIN)
		runCatchingCancellable {
			DiskLruCache.create(dir, size)
		}.recoverCatching { error ->
			error.printStackTraceDebug()
			dir.deleteRecursively()
			dir.mkdir()
			DiskLruCache.create(dir, size)
		}.getOrThrow()
	}

	suspend fun get(url: String): File? {
		val cache = lruCache.get()
		return runInterruptible(Dispatchers.IO) {
			cache.get(url)?.takeIfReadable()
		}
	}

	suspend fun put(url: String, source: Source): File = withContext(Dispatchers.IO) {
		val file = File(cacheDir.get().parentFile, url.longHashCode().toString())
		try {
			val bytes = file.sink(append = false).buffer().use {
				it.writeAllCancellable(source)
			}
			if (bytes == 0L) {
				throw NoDataReceivedException(url)
			}
			lruCache.get().put(url, file)
		} finally {
			file.delete()
		}
	}

	suspend fun put(url: String, bitmap: Bitmap): File = withContext(Dispatchers.IO) {
		val file = File(cacheDir.get().parentFile, url.longHashCode().toString())
		try {
			bitmap.compressToPNG(file)
			lruCache.get().put(url, file)
		} finally {
			file.delete()
		}
	}

	private suspend fun getAvailableSize(): Long = runCatchingCancellable {
		val statFs = StatFs(cacheDir.get().absolutePath)
		statFs.availableBytes
	}.onFailure {
		it.printStackTraceDebug()
	}.getOrDefault(SIZE_DEFAULT)

	private companion object {

		val SIZE_MIN
			get() = FileSize.MEGABYTES.convert(20, FileSize.BYTES)

		val SIZE_DEFAULT
			get() = FileSize.MEGABYTES.convert(200, FileSize.BYTES)
	}
}
