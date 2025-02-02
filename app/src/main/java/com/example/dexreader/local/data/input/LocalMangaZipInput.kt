package com.example.dexreader.local.data.input

import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.collection.ArraySet
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.example.dexreader.core.util.AlphanumComparator
import com.example.dexreader.core.util.ext.longHashCode
import com.example.dexreader.core.util.ext.readText
import com.example.dexreader.core.util.ext.toListSorted
import com.example.dexreader.local.data.MangaIndex
import com.example.dexreader.local.data.output.LocalMangaOutput
import com.example.dexreader.local.domain.model.LocalManga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.example.dexreader.parsers.model.Manga
import org.example.dexreader.parsers.model.MangaChapter
import org.example.dexreader.parsers.model.MangaPage
import org.example.dexreader.parsers.model.MangaSource
import org.example.dexreader.parsers.util.toCamelCase
import java.io.File
import java.util.Enumeration
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Manga archive {.cbz or .zip file}
 * |--- index.json (optional)
 * |--- Page 1.png
 * |--- Page 2.png
 * :
 * L--- Page x.png
 */
class LocalMangaZipInput(root: File) : LocalMangaInput(root) {

	override suspend fun getManga(): LocalManga {
		val manga = runInterruptible(Dispatchers.IO) {
			ZipFile(root).use { zip ->
				val fileUri = root.toUri().toString()
				val entry = zip.getEntry(LocalMangaOutput.ENTRY_NAME_INDEX)
				val index = entry?.let(zip::readText)?.let(::MangaIndex)
				val info = index?.getMangaInfo()
				if (info != null) {
					val cover = zipUri(
						root,
						entryName = index.getCoverEntry() ?: findFirstImageEntry(zip.entries())?.name.orEmpty(),
					)
					return@use info.copy2(
						source = MangaSource.LOCAL,
						url = fileUri,
						coverUrl = cover,
						largeCoverUrl = cover,
						chapters = info.chapters?.map { c ->
							c.copy(url = fileUri, source = MangaSource.LOCAL)
						},
					)
				}
				// fallback
				val title = root.nameWithoutExtension.replace("_", " ").toCamelCase()
				val chapters = ArraySet<String>()
				for (x in zip.entries()) {
					if (!x.isDirectory) {
						chapters += x.name.substringBeforeLast(File.separatorChar, "")
					}
				}
				val uriBuilder = root.toUri().buildUpon()
				Manga(
					id = root.absolutePath.longHashCode(),
					title = title,
					url = fileUri,
					publicUrl = fileUri,
					source = MangaSource.LOCAL,
					coverUrl = zipUri(root, findFirstImageEntry(zip.entries())?.name.orEmpty()),
					chapters = chapters.sortedWith(AlphanumComparator())
						.mapIndexed { i, s ->
							MangaChapter(
								id = "$i$s".longHashCode(),
								name = s.ifEmpty { title },
								number = 0f,
								volume = 0,
								source = MangaSource.LOCAL,
								uploadDate = 0L,
								url = uriBuilder.fragment(s).build().toString(),
								scanlator = null,
								branch = null,
							)
						},
					altTitle = null,
					rating = -1f,
					isNsfw = false,
					tags = setOf(),
					state = null,
					author = null,
					largeCoverUrl = null,
					description = null,
				)
			}
		}
		return LocalManga(manga, root)
	}

	override suspend fun getMangaInfo(): Manga? = runInterruptible(Dispatchers.IO) {
		ZipFile(root).use { zip ->
			val entry = zip.getEntry(LocalMangaOutput.ENTRY_NAME_INDEX)
			val index = entry?.let(zip::readText)?.let(::MangaIndex)
			index?.getMangaInfo()
		}
	}

	override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
		return runInterruptible(Dispatchers.IO) {
			val uri = Uri.parse(chapter.url)
			val file = uri.toFile()
			val zip = ZipFile(file)
			val index = zip.getEntry(LocalMangaOutput.ENTRY_NAME_INDEX)?.let(zip::readText)?.let(::MangaIndex)
			var entries = zip.entries().asSequence()
			entries = if (index != null) {
				val pattern = index.getChapterNamesPattern(chapter)
				entries.filter { x -> !x.isDirectory && x.name.substringBefore('.').matches(pattern) }
			} else {
				val parent = uri.fragment.orEmpty()
				entries.filter { x ->
					!x.isDirectory && x.name.substringBeforeLast(
						File.separatorChar,
						"",
					) == parent
				}
			}
			entries
				.toListSorted(compareBy(AlphanumComparator()) { x -> x.name })
				.map { x ->
					val entryUri = zipUri(file, x.name)
					MangaPage(
						id = entryUri.longHashCode(),
						url = entryUri,
						preview = null,
						source = MangaSource.LOCAL,
					)
				}
		}
	}

	private fun findFirstImageEntry(entries: Enumeration<out ZipEntry>): ZipEntry? {
		val list = entries.toList()
			.filterNot { it.isDirectory }
			.sortedWith(compareBy(AlphanumComparator()) { x -> x.name })
		val map = MimeTypeMap.getSingleton()
		return list.firstOrNull {
			map.getMimeTypeFromExtension(it.name.substringAfterLast('.'))
				?.startsWith("image/") == true
		}
	}
}
